package com.example.TranThanhHai.controller;

import com.example.TranThanhHai.model.CartItem;
import com.example.TranThanhHai.model.User;
import com.example.TranThanhHai.service.CartService;
import com.example.TranThanhHai.service.MomoService;
import com.example.TranThanhHai.service.OrderService;
import com.example.TranThanhHai.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;
    @Autowired
    private MomoService momoService;
    @Autowired
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("cartSubTotal", cartService.getSubTotalAmount());
        model.addAttribute("shippingFee", cartService.getShippingFee());
        model.addAttribute("rewardPoints", cartService.getRewardPoints());
        model.addAttribute("rewardDiscount", cartService.getRewardDiscount());
        model.addAttribute("voucherDiscount", cartService.getVoucherDiscount());
        model.addAttribute("appliedVoucher", cartService.getAppliedVoucher());
        model.addAttribute("cartTotalAmount", cartService.getTotalAmount());
        return "cart/checkout";
    }

    @PostMapping("/submit")
    public String submitOrder(@RequestParam String customerName,
                              @RequestParam String phoneNumber,
                              @RequestParam String address,
                              @RequestParam(required = false) String note,
                              @RequestParam(defaultValue = "COD") String paymentMethod,
                              RedirectAttributes redirectAttributes) {

        List<CartItem> cartItems = cartService.getCartItems();
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        User user = getCurrentUser();

        if ("MOMO".equalsIgnoreCase(paymentMethod)) {
            try {
                String momoResponse = momoService.createPaymentRequest(String.valueOf((long) cartService.getTotalAmount()));
                JsonNode root = objectMapper.readTree(momoResponse);
                String payUrl = root.path("payUrl").asText("");
                String momoOrderId = root.path("orderId").asText("");

                if (payUrl == null || payUrl.isBlank()) {
                    redirectAttributes.addFlashAttribute("error", "Khong tao duoc link thanh toan MoMo.");
                    return "redirect:/order/checkout";
                }

                cartService.savePendingCheckoutInfo(customerName, phoneNumber, address, note, paymentMethod, momoOrderId);
                return "redirect:" + payUrl;
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("error", "Loi ket noi MoMo: " + ex.getMessage());
                return "redirect:/order/checkout";
            }
        }

        orderService.createOrder(user, customerName, phoneNumber, address, note, paymentMethod, cartItems);
        return "redirect:/order/confirmation";
    }

    @GetMapping("/momo-return")
    public String momoReturn(@RequestParam(required = false) String resultCode,
                             @RequestParam(required = false) String orderId,
                             RedirectAttributes redirectAttributes) {
        if (!"0".equals(resultCode)) {
            redirectAttributes.addFlashAttribute("error", "Thanh toan MoMo that bai hoac bi huy.");
            return "redirect:/order/checkout";
        }

        CartService.PendingCheckoutInfo pending = cartService.getPendingCheckoutInfo();
        if (pending == null || pending.getCartSnapshot() == null || pending.getCartSnapshot().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Khong tim thay thong tin don hang cho giao dich MoMo.");
            return "redirect:/cart";
        }

        String momoOrderId = orderId != null && !orderId.isBlank() ? orderId : pending.getMomoOrderId();
        orderService.createPaidMomoOrder(
                getCurrentUser(),
                pending.getCustomerName(),
                pending.getPhoneNumber(),
                pending.getAddress(),
                pending.getNote(),
                momoOrderId,
                pending.getCartSnapshot()
        );

        cartService.clearPendingCheckoutInfo();
        return "redirect:/order/confirmation";
    }

    @GetMapping("/confirmation")
    public String orderConfirmation(Model model) {
        model.addAttribute("message", "Your order has been successfully placed.");
        return "cart/order-confirmation";
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            if (!"anonymousUser".equals(username)) {
                return userService.findByUsername(username).orElse(null);
            }
        }
        return null;
    }
}