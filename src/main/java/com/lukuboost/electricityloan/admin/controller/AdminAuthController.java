package com.lukuboost.electricityloan.admin.controller;

import com.lukuboost.electricityloan.admin.dto.LoginRequest;
import com.lukuboost.electricityloan.admin.dto.LoginResponse;
import com.lukuboost.electricityloan.admin.service.AdminAuthService;
import com.lukuboost.electricityloan.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Auth", description = "Admin authentication endpoints")
public class AdminAuthController {

    private final AdminAuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Admin login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        LoginResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
