package com.example.TranThanhHai.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double price;
    private String description;
    private String image;

    /** Loại khuyến mãi: NONE (không), DISCOUNT (giảm giá %), GIFT (quà tặng) */
    @Column(name = "promotion_type")
    private String promotionType = "NONE";

    /** Phần trăm giảm giá (khi promotionType = DISCOUNT) */
    @Column(name = "discount_percent")
    private Double discountPercent = 0.0;

    /** Mô tả quà tặng (khi promotionType = GIFT) */
    @Column(name = "gift_description")
    private String giftDescription = "";

    /** Số lượng đơn vị còn được áp dụng khuyến mãi cho sản phẩm này */
    @Column(name = "promotion_stock_quantity")
    private Integer promotionStockQuantity = 0;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
