package com.example.TranThanhHai.service;

import com.example.TranThanhHai.model.User;
import com.example.TranThanhHai.repository.IUserRepository;
import com.example.TranThanhHai.repository.IRoleRepository;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Optional;
@Service
@Slf4j
@Transactional
public class UserService implements UserDetailsService {
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IRoleRepository roleRepository;
    // Lưu người dùng mới vào cơ sở dữ liệu sau khi mã hóa mật khẩu và gán vai trò USER mặc định.
    public void save(@NotNull User user) {
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        var userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Role USER not found in database"));
        user.getRoles().add(userRole);

        userRepository.save(user);
        log.info("Saved user={} with roles={}", user.getUsername(), user.getRoles());
    }

    // Gán vai trò mặc định cho người dùng dựa trên tên người dùng.
    public void setDefaultRole(String username) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    var userRole = roleRepository.findByName("USER")
                            .orElseThrow(() -> new IllegalStateException("Role USER not found in database"));
                    user.getRoles().add(userRole);
                    userRepository.save(user);
                },
                () -> {
                    throw new UsernameNotFoundException("User not found");
                }
        );
    }

    // Gán vai trò MANAGER cho người dùng.
    public void setManagerRole(String username) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    roleRepository.findByName("MANAGER").ifPresent(role -> user.getRoles().add(role));
                    userRepository.save(user);
                },
                () -> { throw new UsernameNotFoundException("User not found"); }
        );
    }
    // Tải thông tin chi tiết người dùng để xác thực.
    @Override
    public UserDetails loadUserByUsername(String username) throws
            UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getAuthorities())
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled())
                .build();
    }
    // Tìm kiếm người dùng dựa trên tên đăng nhập.
    public Optional<User> findByUsername(String username) throws
            UsernameNotFoundException {
        return userRepository.findByUsername(username);
    }
}
