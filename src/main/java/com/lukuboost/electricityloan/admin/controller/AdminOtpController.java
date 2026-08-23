package com.lukuboost.electricityloan.admin.controller;

import com.lukuboost.electricityloan.audit.service.AuditService;
import com.lukuboost.electricityloan.common.dto.ApiResponse;
import com.lukuboost.electricityloan.otp.entity.OtpRequest;
import com.lukuboost.electricityloan.otp.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/otp")
@RequiredArgsConstructor
@Tag(name = "Admin OTP Management", description = "View and resend OTPs for customer support")
public class AdminOtpController {

    private final OtpService otpService;
    private final AuditService auditService;

    @GetMapping("/{applicationRef}")
    @Operation(summary = "View OTP for an application",
            description = "Retrieves the current OTP details for a loan application. " +
                    "Use this when a customer did not receive the OTP via SMS.")
    @PreAuthorize("hasAuthority('LOAN_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOtp(
            @PathVariable String applicationRef) {

        OtpRequest otpRequest = otpService.getOtpForAdmin(applicationRef);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationRef", otpRequest.getApplicationRef());
        result.put("phoneNumber", otpRequest.getPhoneNumber());
        result.put("otp", otpRequest.getOtpPlain());
        result.put("status", otpRequest.getStatus());
        result.put("attemptsRemaining", otpRequest.getAttemptsRemaining());
        result.put("expiresAt", otpRequest.getExpiresAt());
        result.put("expired", otpRequest.getExpiresAt().isBefore(Instant.now()));
        result.put("createdAt", otpRequest.getCreatedAt());

        auditService.logAction("OTP_VIEWED", "OTP", otpRequest.getId().toString(),
                null, null, "Admin viewed OTP for application: " + applicationRef);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{applicationRef}/resend")
    @Operation(summary = "Resend OTP for an application",
            description = "Generates a new OTP and sends it to the customer's phone. " +
                    "The old OTP is invalidated. Use this when the OTP has expired or was not received.")
    @PreAuthorize("hasAuthority('LOAN_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendOtp(
            @PathVariable String applicationRef) {

        OtpRequest newOtp = otpService.resendOtp(applicationRef, "admin-portal");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationRef", newOtp.getApplicationRef());
        result.put("phoneNumber", newOtp.getPhoneNumber());
        result.put("otp", newOtp.getOtpPlain());
        result.put("status", newOtp.getStatus());
        result.put("expiresAt", newOtp.getExpiresAt());
        result.put("message", "New OTP generated and sent to customer.");

        auditService.logAction("OTP_RESENT", "OTP", newOtp.getId().toString(),
                null, null, "Admin resent OTP for application: " + applicationRef);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
