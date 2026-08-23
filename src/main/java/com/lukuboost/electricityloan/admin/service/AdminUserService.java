package com.lukuboost.electricityloan.admin.service;

import com.lukuboost.electricityloan.admin.dto.AdminUserDto;
import com.lukuboost.electricityloan.admin.dto.CreateAdminUserRequest;
import com.lukuboost.electricityloan.admin.entity.AdminUser;
import com.lukuboost.electricityloan.admin.entity.Role;
import com.lukuboost.electricityloan.admin.repository.AdminUserRepository;
import com.lukuboost.electricityloan.admin.repository.RoleRepository;
import com.lukuboost.electricityloan.audit.service.AuditService;
import com.lukuboost.electricityloan.common.exception.BusinessException;
import com.lukuboost.electricityloan.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public AdminUserDto createUser(CreateAdminUserRequest request) {
        if (adminUserRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USERNAME_EXISTS", "Username already exists.", HttpStatus.CONFLICT);
        }
        if (adminUserRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_EXISTS", "Email already exists.", HttpStatus.CONFLICT);
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoleIds() != null) {
            for (UUID roleId : request.getRoleIds()) {
                roles.add(roleRepository.findById(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", roleId.toString())));
            }
        }

        AdminUser user = AdminUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .status("ACTIVE")
                .roles(roles)
                .build();

        user = adminUserRepository.save(user);

        auditService.logAction("CREATE_USER", "ADMIN_USER", user.getId().toString(),
                null, null, "Created admin user: " + user.getUsername());

        log.info("Admin user created: {}", user.getUsername());
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> listUsers(Pageable pageable) {
        return adminUserRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AdminUserDto getUser(UUID id) {
        return toDto(adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", id.toString())));
    }

    @Transactional
    public AdminUserDto updateUserStatus(UUID id, String status) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", id.toString()));
        String oldStatus = user.getStatus();
        user.setStatus(status);
        user = adminUserRepository.save(user);

        auditService.logAction("UPDATE_USER_STATUS", "ADMIN_USER", id.toString(),
                "{\"status\":\"" + oldStatus + "\"}", "{\"status\":\"" + status + "\"}",
                "User status changed from " + oldStatus + " to " + status);

        return toDto(user);
    }

    private AdminUserDto toDto(AdminUser user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
