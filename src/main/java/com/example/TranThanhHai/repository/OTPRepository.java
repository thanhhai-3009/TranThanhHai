package com.example.TranThanhHai.repository;

import com.example.TranThanhHai.model.OTP;
import com.example.TranThanhHai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTP, Long> {
    Optional<OTP> findByCodeAndUserAndStatusAndPurpose(String code, User user, String status, String purpose);
    Optional<OTP> findByUserAndPurposeAndStatus(User user, String purpose, String status);
    Optional<OTP> findTopByUserAndPurposeOrderByCreatedAtDesc(User user, String purpose);

    // Để find OTP theo email khi user.id = null (SIGNUP case)
    Optional<OTP> findByCodeAndEmailAndStatusAndPurpose(String code, String email, String status, String purpose);
    Optional<OTP> findByEmailAndPurposeAndStatus(String email, String purpose, String status);
    Optional<OTP> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, String purpose);
}





