package com.example.TranThanhHai.service;

import com.example.TranThanhHai.model.OTP;
import com.example.TranThanhHai.model.User;
import com.example.TranThanhHai.repository.OTPRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OTPService {
    private static final Logger logger = LoggerFactory.getLogger(OTPService.class);

    private final OTPRepository otpRepository;
    private final EmailService emailService;
    private final Random random = new Random();

    /**
     * Tạo và gửi OTP
     */
    public boolean generateAndSendOTP(User user, String purpose) {
        try {
            // Với purpose=SIGNUP, user chưa tồn tại trong DB, nên chỉ dùng email để tìm OTP cũ
            if ("SIGNUP".equals(purpose) && user.getId() == null) {
                // Không cần hủy OTP cũ vì user chưa có ID
                logger.debug("Generating OTP for signup (user not in DB yet)");
            } else if (user.getId() != null) {
                // Hủy OTP cũ nếu còn hiệu lực (cho user đã tồn tại)
                otpRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(user, purpose)
                        .ifPresent(otp -> {
                            if ("PENDING".equals(otp.getStatus())) {
                                otp.setStatus("EXPIRED");
                                otpRepository.save(otp);
                            }
                        });
            }

            // Tạo mã OTP 6 chữ số
            String code = String.format("%06d", random.nextInt(1000000));

            // Lưu OTP vào database
            OTP otp = OTP.builder()
                    .user("SIGNUP".equals(purpose) && user.getId() == null ? null : user)  // user = null khi SIGNUP
                    .code(code)
                    .email(user.getEmail())
                    .purpose(purpose)
                    .status("PENDING")
                    .failedAttempts(0)
                    .createdAt(LocalDateTime.now())
                    .expiryTime(LocalDateTime.now().plusMinutes(10))
                    .build();

            otpRepository.save(otp);
            logger.info("OTP saved successfully for email: {}, purpose: {}", user.getEmail(), purpose);

            // Gửi OTP qua email
            boolean sent = emailService.sendOTPEmail(user.getEmail(), code, purpose);

            if (!sent) {
                logger.warn("Failed to send OTP email for user: {}", user.getUsername());
            }

            return sent;
        } catch (Exception e) {
            logger.error("Error generating OTP for user: {}", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Xác thực OTP
     */
    public boolean verifyOTP(User user, String code, String purpose) {
        try {
            Optional<OTP> otpOpt;

            // Với SIGNUP, tìm theo email vì user chưa có ID
            if ("SIGNUP".equals(purpose)) {
                otpOpt = otpRepository.findByCodeAndEmailAndStatusAndPurpose(
                        code, user.getEmail(), "PENDING", purpose
                );
            } else {
                // Với LOGIN hoặc REWARD_REDEMPTION, tìm theo user
                otpOpt = otpRepository.findByCodeAndUserAndStatusAndPurpose(
                        code, user, "PENDING", purpose
                );
            }

            if (otpOpt.isEmpty()) {
                logger.warn("OTP not found for user: {}, email: {}, purpose: {}", user.getUsername(), user.getEmail(), purpose);
                return false;
            }

            OTP otp = otpOpt.get();

            // Kiểm tra hết hạn
            otp.checkExpiry();
            if (!otp.isValid()) {
                otpRepository.save(otp);
                logger.warn("OTP expired or invalid for user: {}", user.getUsername());
                return false;
            }

            // Xác thực OTP
            otp.verify();
            otpRepository.save(otp);
            logger.info("OTP verified successfully for user: {}", user.getUsername());
            return true;
        } catch (Exception e) {
            logger.error("Error verifying OTP for user: {}", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Ghi lại thất bại khi nhập sai OTP
     */
    public void recordFailedAttempt(User user, String purpose) {
        try {
            Optional<OTP> otpOpt;

            // Với SIGNUP, tìm theo email vì user chưa có ID
            if ("SIGNUP".equals(purpose)) {
                otpOpt = otpRepository.findByEmailAndPurposeAndStatus(user.getEmail(), purpose, "PENDING");
            } else {
                otpOpt = otpRepository.findByUserAndPurposeAndStatus(user, purpose, "PENDING");
            }

            if (otpOpt.isPresent()) {
                OTP otp = otpOpt.get();
                otp.incrementFailedAttempts();
                otpRepository.save(otp);
                logger.warn("Failed OTP attempt for user: {}, attempts: {}", user.getUsername(), otp.getFailedAttempts());
            }
        } catch (Exception e) {
            logger.error("Error recording failed attempt for user: {}", user.getUsername(), e);
        }
    }

    /**
     * Kiểm tra OTP đã xác thực hay chưa
     */
    public boolean isOTPVerified(User user, String purpose) {
        try {
            var otpOpt = otpRepository.findByUserAndPurposeAndStatus(user, purpose, "VERIFIED");
            if (otpOpt.isPresent()) {
                OTP otp = otpOpt.get();
                // OTP verified chỉ có hiệu lực 5 phút
                return otp.getVerifiedAt() != null &&
                       LocalDateTime.now().isBefore(otp.getVerifiedAt().plusMinutes(5));
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking OTP verification for user: {}", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Xóa OTP đã xác thực
     */
    public void deleteVerifiedOTP(User user, String purpose) {
        try {
            Optional<OTP> otpOpt;

            // Với SIGNUP, tìm theo email vì user chưa có ID
            if ("SIGNUP".equals(purpose)) {
                otpOpt = otpRepository.findByEmailAndPurposeAndStatus(user.getEmail(), purpose, "VERIFIED");
            } else {
                otpOpt = otpRepository.findByUserAndPurposeAndStatus(user, purpose, "VERIFIED");
            }

            if (otpOpt.isPresent()) {
                otpRepository.delete(otpOpt.get());
                logger.info("Verified OTP deleted for user: {}", user.getUsername());
            }
        } catch (Exception e) {
            logger.error("Error deleting verified OTP for user: {}", user.getUsername(), e);
        }
    }
}

