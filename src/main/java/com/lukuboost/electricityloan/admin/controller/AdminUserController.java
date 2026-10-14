package com.lukuboost.electricityloan.admin.controller;

import com.lukuboost.electricityloan.admin.dto.AdminUserDto;
import com.lukuboost.electricityloan.admin.dto.CreateAdminUserRequest;
import com.lukuboost.electricityloan.admin.service.AdminUserService;
import com.lukuboost.electricityloan.common.dto.ApiResponse;
import com.lukuboost.electricityloan.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Admin user management")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "List admin users")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserDto>>> listUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminUserDto> page = adminUserService.listUsers(pageable);
        PagedResponse<AdminUserDto> paged = PagedResponse.<AdminUserDto>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get admin user by ID")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<AdminUserDto>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUser(id)));
    }

    @PostMapping
    @Operation(summary = "Create admin user")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<ApiResponse<AdminUserDto>> createUser(
            @Valid @RequestBody CreateAdminUserRequest request) {
        AdminUserDto user = adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update admin user status")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<ApiResponse<AdminUserDto>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        AdminUserDto user = adminUserService.updateUserStatus(id, body.get("status"));
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
