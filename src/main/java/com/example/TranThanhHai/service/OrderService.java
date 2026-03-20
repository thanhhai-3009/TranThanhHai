package com.example.TranThanhHai.service;

import com.example.TranThanhHai.model.CartItem;
import com.example.TranThanhHai.model.Order;
import com.example.TranThanhHai.model.OrderDetail;
import com.example.TranThanhHai.model.Product;
import com.example.TranThanhHai.model.User;
import com.example.TranThanhHai.repository.OrderDetailRepository;
import com.example.TranThanhHai.repository.OrderRepository;
import com.example.TranThanhHai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private RewardPointsCalculationService rewardPointsCalculationService;

    @Transactional
    public Order createOrder(User user,
                             String customerName,
                             String phoneNumber,
                             String address,
                             String note,
                             String paymentMethod,
                             List<CartItem> cartItems) {
        return createOrderInternal(user, customerName, phoneNumber, address, note, paymentMethod,
                "UNPAID", null, cartItems);
    }

    @Transactional
    public Order createPaidMomoOrder(User user,
                                     String customerName,
                                     String phoneNumber,
                                     String address,
                                     String note,
                                     String momoOrderId,
                                     List<CartItem> cartItems) {
        return createOrderInternal(user, customerName, phoneNumber, address, note, "MOMO",
                "PAID", momoOrderId, cartItems);
    }

    private Order createOrderInternal(User user,
                                      String customerName,
                                      String phoneNumber,
                                      String address,
                                      String note,
                                      String paymentMethod,
                                      String paymentStatus,
                                      String momoOrderId,
                                      List<CartItem> cartItems) {
        Order order = new Order();
        order.setUser(user);
        order.setCustomerName(customerName);
        order.setPhoneNumber(phoneNumber);
        order.setAddress(address);
        order.setNote(note);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(paymentStatus);
        order.setMomoOrderId(momoOrderId);

        order.setSubTotal(cartService.getSubTotalAmount());
        order.setShippingFee(cartService.getShippingFee());
        order.setRewardPoints(cartService.getRewardPoints());
        order.setRewardDiscount(cartService.getRewardDiscount());
        order.setVoucherDiscount(cartService.getVoucherDiscount());

        CartService.AppliedVoucher appliedVoucher = cartService.getAppliedVoucher();
        if (appliedVoucher != null) {
            order.setVoucherCode(appliedVoucher.getCode());
            order.setVoucherType(appliedVoucher.getType());
            order.setVoucherValue(appliedVoucher.getValue());
        }

        order.setTotalAmount(cartService.getTotalAmount());
        order = orderRepository.save(order);

        for (CartItem item : cartItems) {
            Product latestProduct = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProduct().getId()));

            consumePromotionStock(latestProduct, item.getQuantity());
            productRepository.save(latestProduct);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(latestProduct);
            detail.setQuantity(item.getQuantity());
            orderDetailRepository.save(detail);
        }

        if (appliedVoucher != null && appliedVoucher.getCode() != null) {
            voucherService.useVoucher(appliedVoucher.getCode());
        }

        if (user != null) {
            rewardPointsCalculationService.addRewardPointsForOrder(user, order.getTotalAmount(), order.getId());
        }

        cartService.clearCart();
        return order;
    }

    private void consumePromotionStock(Product product, int orderedQuantity) {
        String promotionType = product.getPromotionType();
        if (promotionType == null || "NONE".equalsIgnoreCase(promotionType)) {
            return;
        }

        int currentPromotionStock = product.getPromotionStockQuantity() == null
                ? 0 : product.getPromotionStockQuantity();
        int usedPromotionQuantity = Math.min(Math.max(orderedQuantity, 0), Math.max(currentPromotionStock, 0));
        product.setPromotionStockQuantity(Math.max(currentPromotionStock - usedPromotionQuantity, 0));
    }
}

