package com.example.TranThanhHai.service;

import com.example.TranThanhHai.model.RewardPoints;
import com.example.TranThanhHai.model.User;
import com.example.TranThanhHai.repository.RewardPointsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RewardService {
    private static final Logger logger = LoggerFactory.getLogger(RewardService.class);

    private final RewardPointsRepository rewardPointsRepository;

    /**
     * Lấy hoặc tạo RewardPoints cho user
     */
    public RewardPoints getOrCreateRewardPoints(User user) {
        return rewardPointsRepository.findByUser(user)
                .orElseGet(() -> {
                    RewardPoints reward = RewardPoints.builder()
                            .user(user)
                            .totalPoints(0)
                            .usedPoints(0)
                            .history("")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return rewardPointsRepository.save(reward);
                });
    }

    /**
     * Thêm điểm tích lũy cho user (từ đơn hàng)
     */
    public void addRewardPoints(User user, Integer points, String reason) {
        try {
            RewardPoints reward = getOrCreateRewardPoints(user);
            reward.addPoints(points, reason);
            rewardPointsRepository.save(reward);
            logger.info("Added {} points to user: {}, reason: {}", points, user.getUsername(), reason);
        } catch (Exception e) {
            logger.error("Error adding reward points for user: {}", user.getUsername(), e);
        }
    }

    /**
     * Trừ điểm tích lũy (khi đổi thành voucher)
     */
    public boolean deductRewardPoints(User user, Integer points, String reason) {
        try {
            RewardPoints reward = getOrCreateRewardPoints(user);

            if (reward.getAvailablePoints() < points) {
                logger.warn("Insufficient reward points for user: {}", user.getUsername());
                return false;
            }

            reward.deductPoints(points, reason);
            rewardPointsRepository.save(reward);
            logger.info("Deducted {} points from user: {}, reason: {}", points, user.getUsername(), reason);
            return true;
        } catch (Exception e) {
            logger.error("Error deducting reward points for user: {}", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Lấy thông tin điểm tích lũy của user
     */
    public RewardPoints getRewardPoints(User user) {
        return getOrCreateRewardPoints(user);
    }

    /**
     * Kiểm tra user có đủ điểm không
     */
    public boolean hasEnoughPoints(User user, Integer requiredPoints) {
        RewardPoints reward = getOrCreateRewardPoints(user);
        return reward.getAvailablePoints() >= requiredPoints;
    }
}

