package com.example.TranThanhHai.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity để lưu trữ điểm tích lũy của người dùng
 */
@Entity
@Table(name = "reward_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardPoints {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Tổng điểm tích lũy hiện có */
    @Column(nullable = false)
    private Integer totalPoints = 0;

    /** Điểm đã sử dụng */
    @Column(nullable = false)
    private Integer usedPoints = 0;

    /** Lịch sử cập nhật */
    @Column(columnDefinition = "TEXT")
    private String history = "";

    /** Ngày tạo */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Ngày cập nhật cuối */
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Lấy điểm còn lại có thể sử dụng
     */
    public Integer getAvailablePoints() {
        return totalPoints - usedPoints;
    }

    /**
     * Thêm điểm tích lũy
     */
    public void addPoints(Integer points, String reason) {
        if (points > 0) {
            this.totalPoints += points;
            addHistory("Cộng " + points + " điểm: " + reason);
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Trừ điểm tích lũy
     */
    public void deductPoints(Integer points, String reason) {
        if (points > 0 && getAvailablePoints() >= points) {
            this.usedPoints += points;
            addHistory("Trừ " + points + " điểm: " + reason);
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Thêm vào lịch sử
     */
    private void addHistory(String entry) {
        String timestamp = LocalDateTime.now().toString();
        if (history == null || history.isEmpty()) {
            this.history = "[" + timestamp + "] " + entry;
        } else {
            this.history += "\n[" + timestamp + "] " + entry;
        }
    }
}

