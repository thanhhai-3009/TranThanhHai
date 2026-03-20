package com.example.TranThanhHai.repository;

import com.example.TranThanhHai.model.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
}