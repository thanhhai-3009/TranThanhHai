package com.example.TranThanhHai.config;

import com.example.TranThanhHai.repository.IRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleDataInitializer implements CommandLineRunner {

    private final IRoleRepository roleRepository;

    @Override
    public void run(String... args) {
        ensureRole("ADMIN", "Quan tri vien");
        ensureRole("MANAGER", "Quan ly");
        ensureRole("USER", "Nguoi dung");
    }

    private void ensureRole(String name, String description) {
        roleRepository.findByName(name).orElseGet(() -> {
            var role = com.example.TranThanhHai.model.Role.builder()
                    .name(name)
                    .description(description)
                    .build();
            return roleRepository.save(role);
        });
    }
}

