package com.example.TranThanhHai.service;

import com.example.TranThanhHai.model.CartItem;
import com.example.TranThanhHai.model.Product;
import com.example.TranThanhHai.model.Voucher;
import com.example.TranThanhHai.repository.ProductRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;

@Service
@SessionScope
public class CartService {
    private static final double FREE_SHIP_MIN_TOTAL = 1_000_000;
    private static final int FREE_SHIP_MIN_QUANTITY = 2;
    private static final double DEFAULT_SHIPPING_FEE = 30_001;
    private static final int POINTS_PER_NORMAL_PRODUCT = 2;
    private static final int POINTS_PER_REWARD_BLOCK = 1;
    private static final double REWARD_VALUE_PER_BLOCK = 7_500;

    private final List<CartItem> cartItems = new ArrayList<>();
    private PendingCheckoutInfo pendingCheckoutInfo;
    private AppliedVoucher appliedVoucher;

    @Autowired
    private ProductRepository productRepository;

    public void addToCart(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        int safeQuantity = Math.max(quantity, 1);
        for (CartItem item : cartItems) {
            if (item.getProduct().getId().equals(productId)) {
                item.setQuantity(item.getQuantity() + safeQuantity);
                return;
            }
        }

        cartItems.add(new CartItem(product, safeQuantity));
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public int getTotalItems() {
        return cartItems.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public double getSubTotalAmount() {
        return cartItems.stream().mapToDouble(this::getCartItemSubtotal).sum();
    }

    public double getShippingFee() {
        double subTotal = getSubTotalAmount();
        int totalItems = getTotalItems();
        return (subTotal >= FREE_SHIP_MIN_TOTAL && totalItems >= FREE_SHIP_MIN_QUANTITY) ? 0 : DEFAULT_SHIPPING_FEE;
    }

    public int getRewardPoints() {
        int normalQuantity = cartItems.stream()
                .filter(item -> "NONE".equalsIgnoreCase(item.getProduct().getPromotionType()))
                .mapToInt(CartItem::getQuantity)
                .sum();
        return normalQuantity * POINTS_PER_NORMAL_PRODUCT;
    }

    public double getRewardDiscount() {
        int rewardBlocks = getRewardPoints() / POINTS_PER_REWARD_BLOCK;
        return rewardBlocks * REWARD_VALUE_PER_BLOCK;
    }

    public double getVoucherDiscount() {
        if (appliedVoucher == null) {
            return 0;
        }

        double subTotal = getSubTotalAmount();
        if (appliedVoucher.getMinOrderValue() != null && subTotal < appliedVoucher.getMinOrderValue()) {
            return 0;
        }

        if ("PERCENTAGE".equalsIgnoreCase(appliedVoucher.getType())) {
            double discount = subTotal * (appliedVoucher.getValue() / 100.0);
            if (appliedVoucher.getMaxDiscount() != null) {
                discount = Math.min(discount, appliedVoucher.getMaxDiscount());
            }
            return Math.max(discount, 0);
        }

        return Math.max(appliedVoucher.getValue(), 0);
    }

    public double getTotalAmount() {
        double rawTotal = getSubTotalAmount() + getShippingFee() - getRewardDiscount() - getVoucherDiscount();
        return Math.max(rawTotal, 0);
    }

    public double getCartItemSubtotal(CartItem item) {
        Product product = item.getProduct();
        int quantity = Math.max(item.getQuantity(), 0);
        if (!isDiscountPromotion(product)) {
            return product.getPrice() * quantity;
        }

        int discountedQty = getAppliedPromotionQuantity(item);
        int regularQty = Math.max(quantity - discountedQty, 0);
        double discountedUnitPrice = getDiscountedUnitPrice(product);

        return (discountedUnitPrice * discountedQty) + (product.getPrice() * regularQty);
    }

    public int getAppliedPromotionQuantity(CartItem item) {
        Product product = item.getProduct();
        if (!isDiscountPromotion(product)) {
            return 0;
        }

        int promotionStock = product.getPromotionStockQuantity() == null ? 0 : product.getPromotionStockQuantity();
        return Math.min(Math.max(item.getQuantity(), 0), Math.max(promotionStock, 0));
    }

    public int getRegularPriceQuantity(CartItem item) {
        int totalQty = Math.max(item.getQuantity(), 0);
        return Math.max(totalQty - getAppliedPromotionQuantity(item), 0);
    }

    public double getDiscountedUnitPrice(Product product) {
        if (!isDiscountPromotion(product)) {
            return product.getPrice();
        }
        double discountPercent = product.getDiscountPercent() == null ? 0.0 : product.getDiscountPercent();
        return product.getPrice() * (1 - discountPercent / 100.0);
    }

    public void savePendingCheckoutInfo(String customerName,
                                        String phoneNumber,
                                        String address,
                                        String note,
                                        String paymentMethod,
                                        String momoOrderId) {
        PendingCheckoutInfo info = new PendingCheckoutInfo();
        info.setCustomerName(customerName);
        info.setPhoneNumber(phoneNumber);
        info.setAddress(address);
        info.setNote(note);
        info.setPaymentMethod(paymentMethod);
        info.setMomoOrderId(momoOrderId);
        info.setCartSnapshot(createCartSnapshot());
        this.pendingCheckoutInfo = info;
    }

    public PendingCheckoutInfo getPendingCheckoutInfo() {
        return pendingCheckoutInfo;
    }

    public void clearPendingCheckoutInfo() {
        this.pendingCheckoutInfo = null;
    }

    public List<CartItem> createCartSnapshot() {
        List<CartItem> snapshot = new ArrayList<>();
        for (CartItem item : cartItems) {
            snapshot.add(new CartItem(item.getProduct(), item.getQuantity()));
        }
        return snapshot;
    }

    public void updateCartItemQuantity(Long productId, int newQuantity) {
        if (newQuantity <= 0) {
            removeFromCart(productId);
            return;
        }

        for (CartItem item : cartItems) {
            if (item.getProduct().getId().equals(productId)) {
                item.setQuantity(newQuantity);
                return;
            }
        }
    }

    public void removeFromCart(Long productId) {
        cartItems.removeIf(item -> item.getProduct().getId().equals(productId));
    }

    public void clearCart() {
        cartItems.clear();
        clearAppliedVoucher();
    }

    private boolean isDiscountPromotion(Product product) {
        return "DISCOUNT".equalsIgnoreCase(product.getPromotionType())
                && product.getDiscountPercent() != null
                && product.getDiscountPercent() > 0;
    }

    @Getter
    @Setter
    public static class PendingCheckoutInfo {
        private String customerName;
        private String phoneNumber;
        private String address;
        private String note;
        private String paymentMethod;
        private String momoOrderId;
        private List<CartItem> cartSnapshot;
    }

    @Getter
    @Setter
    public static class AppliedVoucher {
        private String code;
        private String type;
        private Double value;
        private Double maxDiscount;
        private Double minOrderValue;
    }

    public CartService.AppliedVoucher getAppliedVoucher() {
        return appliedVoucher;
    }

    public void applyVoucher(Voucher voucher) {
        if (voucher == null) {
            clearAppliedVoucher();
            return;
        }

        AppliedVoucher snapshot = new AppliedVoucher();
        snapshot.setCode(voucher.getCode());
        snapshot.setType(voucher.getType());
        snapshot.setValue(voucher.getValue());
        snapshot.setMaxDiscount(voucher.getMaxDiscount());
        snapshot.setMinOrderValue(voucher.getMinOrderValue());
        this.appliedVoucher = snapshot;
    }

    public void clearAppliedVoucher() {
        this.appliedVoucher = null;
    }
}
