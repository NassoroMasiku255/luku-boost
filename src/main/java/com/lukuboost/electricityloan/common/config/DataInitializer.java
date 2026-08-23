package com.lukuboost.electricityloan.common.config;

import com.lukuboost.electricityloan.admin.entity.AdminUser;
import com.lukuboost.electricityloan.admin.entity.Permission;
import com.lukuboost.electricityloan.admin.entity.Role;
import com.lukuboost.electricityloan.admin.repository.AdminUserRepository;
import com.lukuboost.electricityloan.admin.repository.PermissionRepository;
import com.lukuboost.electricityloan.admin.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (adminUserRepository.findByUsername("superadmin").isPresent()) {
            log.info("Superadmin already exists, skipping initialization");
            return;
        }

        log.info("Initializing default admin data...");

        // Create permissions
        List<String[]> permDefs = List.of(
                new String[]{"DASHBOARD_VIEW", "View dashboard", "DASHBOARD"},
                new String[]{"LOAN_VIEW", "View loans", "LOAN"},
                new String[]{"LOAN_MANAGE", "Manage loans", "LOAN"},
                new String[]{"CUSTOMER_VIEW", "View customers", "CUSTOMER"},
                new String[]{"CUSTOMER_MANAGE", "Manage customers", "CUSTOMER"},
                new String[]{"TRANSACTION_VIEW", "View transactions", "TRANSACTION"},
                new String[]{"RECONCILIATION_VIEW", "View reconciliation", "RECONCILIATION"},
                new String[]{"RECONCILIATION_MANAGE", "Manage reconciliation", "RECONCILIATION"},
                new String[]{"AUDIT_VIEW", "View audit logs", "AUDIT"},
                new String[]{"USER_VIEW", "View admin users", "USER"},
                new String[]{"USER_MANAGE", "Manage admin users", "USER"},
                new String[]{"CONFIG_VIEW", "View configurations", "CONFIG"},
                new String[]{"CONFIG_MANAGE", "Manage configurations", "CONFIG"}
        );

        Set<Permission> allPermissions = new HashSet<>();
        for (String[] def : permDefs) {
            Permission perm = permissionRepository.findByName(def[0])
                    .orElseGet(() -> permissionRepository.save(Permission.builder()
                            .name(def[0])
                            .description(def[1])
                            .module(def[2])
                            .build()));
            allPermissions.add(perm);
        }

        // Create SUPER_ADMIN role
        Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .name("SUPER_ADMIN")
                            .description("Super Administrator with all permissions")
                            .status("ACTIVE")
                            .permissions(allPermissions)
                            .build();
                    return roleRepository.save(role);
                });

        // Create superadmin user
        AdminUser superAdmin = AdminUser.builder()
                .username("superadmin")
                .email("admin@lukuboost.co.tz")
                .passwordHash(passwordEncoder.encode("Admin@2026!"))
                .fullName("Super Administrator")
                .status("ACTIVE")
                .roles(Set.of(superAdminRole))
                .build();

        adminUserRepository.save(superAdmin);
        log.info("Superadmin user created successfully. Username: superadmin, Password: Admin@2026!");
    }
}
