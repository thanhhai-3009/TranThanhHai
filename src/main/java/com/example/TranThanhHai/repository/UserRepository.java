package com.example.TranThanhHai.repository;

import com.example.TranThanhHai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}