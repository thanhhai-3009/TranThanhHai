package com.example.TranThanhHai.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity để lưu trữ voucher được tạo từ điểm tích lũy
 */
@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Mã voucher (unique) */
    @Column(unique = true, nullable = false)
    private String code;

    /** Loại voucher: FIXED (cố định VND) hoặc PERCENTAGE (% giảm) */
    @Column(nullable = false)
    private String type = "FIXED"; // FIXED hoặc PERCENTAGE

    /** Giá trị voucher
     * - Nếu type=FIXED: giá trị theo VND (VD: 50000)
     * - Nếu type=PERCENTAGE: giá trị theo % (VD: 10 = 10%)
     */
    @Column(nullable = false)
    private Double value;

    /** Giá trị tối đa khi giảm (cho type=PERCENTAGE)
     * VD: Giảm 10% nhưng tối đa 100.000 VND
     */
    private Double maxDiscount;

    /** Giá trị tối thiểu đơn hàng để sử dụng voucher */
    private Double minOrderValue;

    /** Điểm đã dùng để đổi */
    @Column(nullable = false)
    private Integer pointsUsed;

    /** Trạng thái: ACTIVE, USED, EXPIRED */
    @Column(nullable = false)
    private String status = "ACTIVE";

    /** Ngày hết hạn */
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    /** Ngày sử dụng */
    private LocalDateTime usedDate;

    /** Ngày tạo */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Ghi chú */
    private String notes;

    /**
     * Kiểm tra voucher còn hiệu lực không
     */
    public boolean isValid() {
        return "ACTIVE".equals(status) && LocalDateTime.now().isBefore(expiryDate);
    }

    /**
     * Sử dụng voucher
     */
    public void use() {
        if (isValid()) {
            this.status = "USED";
            this.usedDate = LocalDateTime.now();
        }
    }

    /**
     * Kiểm tra voucher hết hạn
     */
    public void checkExpiry() {
        if ("ACTIVE".equals(status) && LocalDateTime.now().isAfter(expiryDate)) {
            this.status = "EXPIRED";
        }
    }
}

