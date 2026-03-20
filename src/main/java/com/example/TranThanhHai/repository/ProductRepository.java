package com.example.TranThanhHai.repository;

import com.example.TranThanhHai.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    /** Sản phẩm theo loại khuyến mãi */
    List<Product> findByPromotionType(String promotionType);
}