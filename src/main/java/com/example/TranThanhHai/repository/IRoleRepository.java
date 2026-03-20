package com.example.TranThanhHai.repository;

import java.util.Optional;
import com.example.TranThanhHai.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IRoleRepository extends JpaRepository<Role, Long> {
    Role findRoleById(Long id);
    Optional<Role> findByName(String name);
}
