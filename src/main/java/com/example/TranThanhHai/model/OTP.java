package com.example.TranThanhHai.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity để lưu trữ OTP gửi đến email người dùng
 */
@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OTP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /** Mã OTP (6 chữ số) */
    @Column(nullable = false)
    private String code;

    /** Email gửi OTP tới */
    @Column(nullable = false)
    private String email;

    /** Mục đích OTP: SIGNUP, LOGIN, REWARD_REDEMPTION */
    @Column(nullable = false)
    private String purpose;

    /** Trạng thái: PENDING, VERIFIED, EXPIRED, FAILED */
    @Column(nullable = false)
    private String status = "PENDING";

    /** Số lần thử nhập sai */
    @Column(nullable = false)
    private Integer failedAttempts = 0;

    /** Ngày tạo */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Thời gian hết hạn (mặc định 10 phút) */
    @Column(nullable = false)
    private LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);

    /** Ngày xác thực */
    private LocalDateTime verifiedAt;

    /**
     * Kiểm tra OTP còn hiệu lực không
     */
    public boolean isValid() {
        return "PENDING".equals(status) && LocalDateTime.now().isBefore(expiryTime) && failedAttempts < 5;
    }

    /**
     * Kiểm tra OTP đã hết hạn không
     */
    public void checkExpiry() {
        if ("PENDING".equals(status) && LocalDateTime.now().isAfter(expiryTime)) {
            this.status = "EXPIRED";
        }
    }

    /**
     * Xác thực OTP
     */
    public void verify() {
        if (isValid()) {
            this.status = "VERIFIED";
            this.verifiedAt = LocalDateTime.now();
        }
    }

    /**
     * Tăng số lần thử sai
     */
    public void incrementFailedAttempts() {
        this.failedAttempts++;
        if (failedAttempts >= 5) {
            this.status = "FAILED";
        }
    }
}

