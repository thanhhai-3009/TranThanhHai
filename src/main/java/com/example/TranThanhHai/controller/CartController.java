package com.example.TranThanhHai.controller;

import com.example.TranThanhHai.model.User;
import com.example.TranThanhHai.model.Voucher;
import com.example.TranThanhHai.service.CartService;
import com.example.TranThanhHai.service.UserService;
import com.example.TranThanhHai.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private UserService userService;

    @GetMapping
    public String showCart(Model model) {
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("cartSubTotal", cartService.getSubTotalAmount());
        model.addAttribute("shippingFee", cartService.getShippingFee());
        model.addAttribute("rewardPoints", cartService.getRewardPoints());
        model.addAttribute("rewardDiscount", cartService.getRewardDiscount());
        model.addAttribute("voucherDiscount", cartService.getVoucherDiscount());
        model.addAttribute("appliedVoucher", cartService.getAppliedVoucher());
        model.addAttribute("cartTotalAmount", cartService.getTotalAmount());

        User user = getCurrentUser();
        if (user != null) {
            List<Voucher> activeVouchers = voucherService.getActiveVouchers(user);
            model.addAttribute("activeVouchers", activeVouchers);
        }

        return "cart/cart";
    }

    @PostMapping("/voucher/apply")
    public String applyVoucher(@RequestParam String voucherCode) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        voucherService.getVoucherByCode(voucherCode).ifPresent(voucher -> {
            if (voucher.getUser().getId().equals(user.getId()) && voucher.isValid()) {
                cartService.applyVoucher(voucher);
            }
        });
        return "redirect:/cart";
    }

    @PostMapping("/voucher/clear")
    public String clearVoucher() {
        cartService.clearAppliedVoucher();
        return "redirect:/cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity) {
        cartService.addToCart(productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateCartQuantity(@RequestParam Long productId,
                                     @RequestParam int quantity) {
        cartService.updateCartItemQuantity(productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/update-ajax")
    public ResponseEntity<Map<String, Object>> updateCartQuantityAjax(@RequestParam Long productId,
                                                                       @RequestParam int quantity) {
        cartService.updateCartItemQuantity(productId, quantity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cartSubTotal", cartService.getSubTotalAmount());
        response.put("shippingFee", cartService.getShippingFee());
        response.put("rewardPoints", cartService.getRewardPoints());
        response.put("rewardDiscount", cartService.getRewardDiscount());
        response.put("voucherDiscount", cartService.getVoucherDiscount());
        response.put("cartTotalAmount", cartService.getTotalAmount());
        response.put("totalItems", cartService.getTotalItems());

        // Trả về subtotal của item vừa cập nhật
        cartService.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> response.put("itemSubtotal", cartService.getCartItemSubtotal(item)));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/remove/{productId}")
    public String removeFromCart(@PathVariable Long productId) {
        cartService.removeFromCart(productId);
        return "redirect:/cart";
    }

    @PostMapping("/remove-ajax/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromCartAjax(@PathVariable Long productId) {
        cartService.removeFromCart(productId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cartSubTotal", cartService.getSubTotalAmount());
        response.put("shippingFee", cartService.getShippingFee());
        response.put("rewardPoints", cartService.getRewardPoints());
        response.put("rewardDiscount", cartService.getRewardDiscount());
        response.put("voucherDiscount", cartService.getVoucherDiscount());
        response.put("cartTotalAmount", cartService.getTotalAmount());
        response.put("totalItems", cartService.getTotalItems());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/clear")
    public String clearCart() {
        cartService.clearCart();
        return "redirect:/cart";
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