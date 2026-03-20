package com.example.TranThanhHai.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String customerName;
    private String phoneNumber;
    private String address;

    @Column(length = 500)
    private String note;

    private String paymentMethod;
    private String paymentStatus;
    private String momoOrderId;

    private double subTotal;
    private double shippingFee;
    private int rewardPoints;
    private double rewardDiscount;
    private double voucherDiscount;
    private String voucherCode;
    private String voucherType;
    private Double voucherValue;
    private double totalAmount;

    @OneToMany(mappedBy = "order")
    private List<OrderDetail> orderDetails;
}
