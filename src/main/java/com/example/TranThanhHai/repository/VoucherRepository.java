package com.example.TranThanhHai.repository;

import com.example.TranThanhHai.model.Voucher;
import com.example.TranThanhHai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);
    List<Voucher> findByUser(User user);
    List<Voucher> findByUserAndStatus(User user, String status);
    List<Voucher> findByUserOrderByCreatedAtDesc(User user);
}

