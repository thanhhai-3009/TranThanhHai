package com.example.TranThanhHai.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO để lưu thông tin đăng ký tạm thời trong session
 * Dùng cho quá trình xác thực OTP trong registration
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationData {
    private String email;
    private String username;
    private String password;
    private String phone;
    private LocalDateTime createdAt;

    /**
     * Kiểm tra dữ liệu đăng ký đã hết hạn (15 phút)
     */
    public boolean isExpired() {
        return createdAt != null &&
               LocalDateTime.now().isAfter(createdAt.plusMinutes(15));
    }
}

