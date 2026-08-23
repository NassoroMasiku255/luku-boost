package com.lukuboost.electricityloan.admin.service;

import com.lukuboost.electricityloan.admin.dto.LoginRequest;
import com.lukuboost.electricityloan.admin.dto.LoginResponse;
import com.lukuboost.electricityloan.admin.entity.AdminUser;
import com.lukuboost.electricityloan.admin.entity.Permission;
import com.lukuboost.electricityloan.admin.entity.Role;
import com.lukuboost.electricityloan.admin.repository.AdminUserRepository;
import com.lukuboost.electricityloan.audit.service.AuditService;
import com.lukuboost.electricityloan.common.exception.BusinessException;
import com.lukuboost.electricityloan.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS",
                        "Invalid username or password.", HttpStatus.UNAUTHORIZED));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("ACCOUNT_DISABLED",
                    "Your account has been disabled.", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for username={}", request.getUsername());
            throw new BusinessException("INVALID_CREDENTIALS",
                    "Invalid username or password.", HttpStatus.UNAUTHORIZED);
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), permissions);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        user.setLastLoginAt(Instant.now());
        adminUserRepository.save(user);

        auditService.logSystemEvent("LOGIN", "ADMIN_USER", user.getId().toString(),
                "Admin user logged in: " + user.getUsername());

        log.info("Admin user logged in: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(roles)
                .permissions(permissions)
                .expiresIn(900) // 15 minutes
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken) || !"REFRESH".equals(jwtService.getTokenType(refreshToken))) {
            throw new BusinessException("INVALID_TOKEN", "Invalid or expired refresh token.", HttpStatus.UNAUTHORIZED);
        }

        var userId = jwtService.getUserId(refreshToken);
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found.", HttpStatus.UNAUTHORIZED));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("ACCOUNT_DISABLED", "Account has been disabled.", HttpStatus.FORBIDDEN);
        }

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), permissions);
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(roles)
                .permissions(permissions)
                .expiresIn(900)
                .build();
    }
}
