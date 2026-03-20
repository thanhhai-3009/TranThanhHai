package com.example.TranThanhHai.repository;

import com.example.TranThanhHai.model.RewardPoints;
import com.example.TranThanhHai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RewardPointsRepository extends JpaRepository<RewardPoints, Long> {
    Optional<RewardPoints> findByUser(User user);
    Optional<RewardPoints> findByUserId(Long userId);
}

