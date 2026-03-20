package com.example.TranThanhHai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    /**
     * Gửi OTP tới email người dùng
     */
    public boolean sendOTPEmail(String toEmail, String otp, String purpose) {
        try {
            logger.info("Attempting to send OTP email to: {} for purpose: {}", toEmail, purpose);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("megagekkouga9@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Mã xác thực OTP - The Gioi Di Dong");

            String purposeText = getPurposeText(purpose);
            message.setText(
                "Mã OTP của bạn là: " + otp + "\n\n" +
                "Mục đích: " + purposeText + "\n" +
                "Thời gian hiệu lực: 10 phút\n\n" +
                "Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.\n\n" +
                "The Gioi Di Dong - Mua hàng công nghệ"
            );

            logger.debug("Email message prepared. To: {}, Subject: {}", toEmail, message.getSubject());
            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send OTP email to: {} - Error: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gửi email thông báo đổi điểm thành công
     */
    public boolean sendRedemptionSuccessEmail(String toEmail, Integer points, Double voucherValue, String voucherCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@tgdd.vn");
            message.setTo(toEmail);
            message.setSubject("Đổi điểm thành công - The Gioi Di Dong");

            message.setText(
                "Bạn đã đổi điểm thành công!\n\n" +
                "Thông tin chi tiết:\n" +
                "- Điểm đã dùng: " + points + " điểm\n" +
                "- Giá trị voucher: " + String.format("%.0f", voucherValue) + " VND\n" +
                "- Mã voucher: " + voucherCode + "\n" +
                "- Hạn sử dụng: 30 ngày kể từ ngày đổi\n\n" +
                "Voucher có thể được sử dụng cho lần mua sắm tiếp theo của bạn.\n\n" +
                "The Gioi Di Dong - Mua hàng công nghệ"
            );

            mailSender.send(message);
            logger.info("Redemption success email sent to: {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send redemption email to: {}", toEmail, e);
            return false;
        }
    }

    private String getPurposeText(String purpose) {
        return switch (purpose) {
            case "SIGNUP" -> "Đăng ký tài khoản";
            case "LOGIN" -> "Đăng nhập";
            case "REWARD_REDEMPTION" -> "Đổi điểm tích lũy";
            default -> "Xác thực";
        };
    }
}

