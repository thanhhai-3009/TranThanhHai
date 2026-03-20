package com.example.TranThanhHai.service;

import com.example.TranThanhHai.model.User;
import com.example.TranThanhHai.model.Voucher;
import com.example.TranThanhHai.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {
    private static final Logger logger = LoggerFactory.getLogger(VoucherService.class);

    private final VoucherRepository voucherRepository;
    private final RewardService rewardService;
    private final EmailService emailService;

    /**
     * Tạo voucher từ điểm tích lũy
     * - FIXED: 1000 điểm = 10,000 VND
     * - PERCENTAGE: 1000 điểm = 5% (tối đa 50,000 VND)
     */
    public Optional<Voucher> redeemVoucher(User user, Integer points, String voucherType) {
        try {
            if (!rewardService.hasEnoughPoints(user, points)) {
                logger.warn("Insufficient points for user: {}", user.getUsername());
                return Optional.empty();
            }

            VoucherValueConfig config = calculateVoucherValue(points, voucherType);
            String code = generateVoucherCode();

            Voucher voucher = Voucher.builder()
                    .user(user)
                    .code(code)
                    .type(config.type)
                    .value(config.value)
                    .maxDiscount(config.maxDiscount)
                    .minOrderValue(config.minOrderValue)
                    .pointsUsed(points)
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .expiryDate(LocalDateTime.now().plusDays(30))
                    .notes("Đổi từ " + points + " điểm tích lũy")
                    .build();

            Voucher savedVoucher = voucherRepository.save(voucher);
            boolean deducted = rewardService.deductRewardPoints(user, points, "Đổi thành voucher: " + code);

            if (deducted) {
                emailService.sendRedemptionSuccessEmail(user.getEmail(), points,
                        "FIXED".equals(config.type) ? config.value : config.maxDiscount, code);
                logger.info("Voucher created successfully for user: {}, code: {}, type={}",
                        user.getUsername(), code, config.type);
                return Optional.of(savedVoucher);
            }

            voucherRepository.delete(savedVoucher);
            logger.warn("Failed to deduct points for user: {}", user.getUsername());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error redeeming voucher for user: {}", user.getUsername(), e);
            return Optional.empty();
        }
    }

    public Optional<Voucher> redeemVoucher(User user, Integer points) {
        return redeemVoucher(user, points, "FIXED");
    }

    /**
     * Lấy danh sách voucher của user
     */
    public List<Voucher> getUserVouchers(User user) {
        return voucherRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Lấy danh sách voucher active của user
     */
    public List<Voucher> getActiveVouchers(User user) {
        // Dong bo trang thai het han truoc khi tra du lieu
        updateExpiredVouchers(user);

        return voucherRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(v -> v.getStatus() != null)
                .filter(v -> "ACTIVE".equalsIgnoreCase(v.getStatus()))
                .filter(Voucher::isValid)
                .collect(Collectors.toList());
    }

    /**
     * Lấy voucher theo mã
     */
    public Optional<Voucher> getVoucherByCode(String code) {
        return voucherRepository.findByCode(code);
    }

    /**
     * Sử dụng voucher
     */
    public boolean useVoucher(String code) {
        try {
            Optional<Voucher> voucherOpt = voucherRepository.findByCode(code);
            if (voucherOpt.isEmpty()) {
                logger.warn("Voucher not found: {}", code);
                return false;
            }

            Voucher voucher = voucherOpt.get();

            if (!voucher.isValid()) {
                logger.warn("Voucher is not valid: {}", code);
                return false;
            }

            voucher.use();
            voucherRepository.save(voucher);
            logger.info("Voucher used successfully: {}", code);
            return true;
        } catch (Exception e) {
            logger.error("Error using voucher: {}", code, e);
            return false;
        }
    }

    /**
     * Kiểm tra và cập nhật status voucher hết hạn
     */
    public void updateExpiredVouchers(User user) {
        try {
            List<Voucher> vouchers = getUserVouchers(user);
            vouchers.forEach(voucher -> {
                voucher.checkExpiry();
                voucherRepository.save(voucher);
            });
        } catch (Exception e) {
            logger.error("Error updating expired vouchers for user: {}", user.getUsername(), e);
        }
    }

    /**
     * Sinh mã voucher unique
     */
    private String generateVoucherCode() {
        String code;
        do {
            // Format: VOUCHER-XXXXXX (prefix + 6 chữ cái/số)
            code = "VOUCHER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (voucherRepository.findByCode(code).isPresent());
        return code;
    }

    private VoucherValueConfig calculateVoucherValue(Integer points, String voucherType) {
        String normalizedType = voucherType == null ? "FIXED" : voucherType.toUpperCase();
        if ("PERCENTAGE".equals(normalizedType)) {
            double percent = switch (Math.max(points, 0)) {
                case 1000 -> 5.0;
                case 2000 -> 10.0;
                case 5000 -> 15.0;
                case 10000 -> 20.0;
                default -> Math.min(points / 500.0, 25.0);
            };
            double maxDiscount = switch (Math.max(points, 0)) {
                case 1000 -> 50_000.0;
                case 2000 -> 100_000.0;
                case 5000 -> 200_000.0;
                case 10000 -> 400_000.0;
                default -> Math.max(points * 40.0, 50_000.0);
            };
            return new VoucherValueConfig("PERCENTAGE", percent, maxDiscount, 100_000.0);
        }

        double fixedValue = (points / 1000.0) * 10_000.0;
        return new VoucherValueConfig("FIXED", fixedValue, null, 0.0);
    }

    private record VoucherValueConfig(String type, Double value, Double maxDiscount, Double minOrderValue) {}
}
