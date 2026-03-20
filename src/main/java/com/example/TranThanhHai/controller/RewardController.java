package com.example.TranThanhHai.controller;

import com.example.TranThanhHai.model.User;
import com.example.TranThanhHai.model.Voucher;
import com.example.TranThanhHai.model.RewardPoints;
import com.example.TranThanhHai.service.RewardService;
import com.example.TranThanhHai.service.VoucherService;
import com.example.TranThanhHai.service.OTPService;
import com.example.TranThanhHai.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/rewards")
public class RewardController {
    private static final Logger logger = LoggerFactory.getLogger(RewardController.class);

    @Autowired
    private RewardService rewardService;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private OTPService otpService;

    @Autowired
    private UserService userService;

    /**
     * Hiển thị trang điểm tích lũy
     */
    @GetMapping
    public String showRewardsPage(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        RewardPoints rewardPoints = rewardService.getRewardPoints(user);
        List<Voucher> vouchers = voucherService.getUserVouchers(user);

        // Cập nhật status voucher hết hạn
        voucherService.updateExpiredVouchers(user);

        model.addAttribute("rewardPoints", rewardPoints);
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("activeVouchersCount", voucherService.getActiveVouchers(user).size());

        return "rewards/rewards-dashboard";
    }

    /**
     * Hiển thị trang đổi điểm
     */
    @GetMapping("/redeem")
    public String showRedeemPage(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        RewardPoints rewardPoints = rewardService.getRewardPoints(user);

        // Tính các tier voucher có sẵn
        Integer availablePoints = rewardPoints.getAvailablePoints();
        model.addAttribute("rewardPoints", rewardPoints);
        model.addAttribute("availablePoints", availablePoints);

        // Các lựa chọn voucher: 1000, 2000, 5000, 10000 điểm
        model.addAttribute("voucherOptions", new Integer[]{1000, 2000, 5000, 10000});
        model.addAttribute("voucherTypes", new String[]{"FIXED", "PERCENTAGE"});

        return "rewards/redeem-reward";
    }

    /**
     * Gửi OTP để xác thực đổi điểm
     */
    @PostMapping("/send-otp")
    @ResponseBody
    public java.util.Map<String, Object> sendOTP() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        User user = getCurrentUser();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return response;
        }

        boolean sent = otpService.generateAndSendOTP(user, "REWARD_REDEMPTION");
        response.put("success", sent);
        response.put("message", sent ? "OTP đã được gửi tới email của bạn" : "Gửi OTP thất bại");

        return response;
    }

    /**
     * Xác thực OTP và đổi điểm
     */
    @PostMapping("/redeem")
    public String redeemReward(
            @RequestParam Integer points,
            @RequestParam String otp,
            @RequestParam(defaultValue = "FIXED") String voucherType,
            RedirectAttributes redirectAttributes) {

        User user = getCurrentUser();
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        // Kiểm tra OTP
        if (!otpService.verifyOTP(user, otp, "REWARD_REDEMPTION")) {
            // Ghi lại thất bại
            otpService.recordFailedAttempt(user, "REWARD_REDEMPTION");
            redirectAttributes.addFlashAttribute("error", "OTP không chính xác hoặc đã hết hạn");
            return "redirect:/rewards/redeem";
        }

        // Kiểm tra điểm đủ không
        if (!rewardService.hasEnoughPoints(user, points)) {
            redirectAttributes.addFlashAttribute("error", "Điểm tích lũy không đủ");
            return "redirect:/rewards/redeem";
        }

        // Đổi điểm thành voucher
        Optional<Voucher> voucherOpt = voucherService.redeemVoucher(user, points, voucherType);

        if (voucherOpt.isPresent()) {
            Voucher voucher = voucherOpt.get();
            // Xóa OTP đã xác thực
            otpService.deleteVerifiedOTP(user, "REWARD_REDEMPTION");

            redirectAttributes.addFlashAttribute("success",
                "Đổi điểm thành công! Voucher: " + voucher.getCode());
            return "redirect:/rewards";
        } else {
            redirectAttributes.addFlashAttribute("error", "Đổi điểm thất bại, vui lòng thử lại");
            return "redirect:/rewards/redeem";
        }
    }

    /**
     * Hiển thị chi tiết voucher
     */
    @GetMapping("/voucher/{code}")
    public String viewVoucher(@PathVariable String code, Model model) {
        Optional<Voucher> voucherOpt = voucherService.getVoucherByCode(code);

        if (voucherOpt.isEmpty()) {
            return "redirect:/rewards";
        }

        Voucher voucher = voucherOpt.get();
        model.addAttribute("voucher", voucher);

        return "rewards/voucher-detail";
    }

    /**
     * Lấy thông tin user hiện tại
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userService.findByUsername(username).orElse(null);
        }
        return null;
    }
}

