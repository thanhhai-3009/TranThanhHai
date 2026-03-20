package com.example.TranThanhHai.service;

import com.example.TranThanhHai.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dịch vụ tính toán và cộng điểm tích lũy cho user
 * Được gọi khi order được thanh toán thành công
 */
@Service
@RequiredArgsConstructor
public class RewardPointsCalculationService {
    private static final Logger logger = LoggerFactory.getLogger(RewardPointsCalculationService.class);

    private final RewardService rewardService;

    /**
     * Tỷ lệ cộng điểm: 1000 VND = 1 điểm
     */
    private static final int POINTS_PER_THOUSAND_VND = 1;

    /**
     * Cộng điểm tích lũy khi order được thanh toán
     *
     * @param user User đã thanh toán
     * @param totalAmount Tổng tiền đơn hàng (VND)
     * @param orderId ID của order
     */
    public void addRewardPointsForOrder(User user, Double totalAmount, Long orderId) {
        try {
            // Tính điểm: 1000 VND = 1 điểm
            Integer points = calculatePoints(totalAmount);

            if (points > 0) {
                String reason = "Thanh toán đơn hàng #" + orderId + " (" + String.format("%.0f", totalAmount) + " VND)";
                rewardService.addRewardPoints(user, points, reason);
                logger.info("Added {} reward points to user: {} for order: {}", points, user.getUsername(), orderId);
            }
        } catch (Exception e) {
            logger.error("Error adding reward points for user: {} on order: {}", user.getUsername(), orderId, e);
            // Không ném exception, chỉ log - không ảnh hưởng đến quá trình thanh toán
        }
    }

    /**
     * Tính điểm tích lũy dựa trên số tiền
     *
     * @param amount Số tiền (VND)
     * @return Số điểm được cộng
     */
    public Integer calculatePoints(Double amount) {
        if (amount == null || amount <= 0) {
            return 0;
        }
        // 1000 VND = 1 điểm
        return (int) (amount / 1000);
    }

    /**
     * Lấy số tiền tương ứng với 1 điểm
     *
     * @return Số tiền (VND)
     */
    public Double getAmountPerPoint() {
        return 1000.0;
    }

    /**
     * Lấy số điểm tương ứng với số tiền
     *
     * @param amount Số tiền (VND)
     * @return Số điểm
     */
    public Integer getPointsForAmount(Double amount) {
        return calculatePoints(amount);
    }
}

