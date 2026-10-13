package com.lukuboost.electricityloan.otp.service;

import com.lukuboost.electricityloan.common.exception.BusinessException;
import com.lukuboost.electricityloan.common.exception.RateLimitExceededException;
import com.lukuboost.electricityloan.notification.service.NotificationService;
import com.lukuboost.electricityloan.otp.entity.OtpRequest;
import com.lukuboost.electricityloan.otp.repository.OtpRequestRepository;
import com.lukuboost.electricityloan.otp.repository.OtpVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;
    @Mock
    private OtpVerificationRepository otpVerificationRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "expirySeconds", 300);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 5);
        ReflectionTestUtils.setField(otpService, "resendCooldownSeconds", 60);
        ReflectionTestUtils.setField(otpService, "maxRequestsPerHour", 5);
    }

    @Test
    void generateAndSendOtp_shouldCreateAndSendOtp() {
        when(otpRequestRepository.countByPhoneNumberSince(eq("255712345678"), any()))
                .thenReturn(0L);
        when(otpRequestRepository.save(any(OtpRequest.class)))
                .thenAnswer(inv -> {
                    OtpRequest req = inv.getArgument(0);
                    req.setId(UUID.randomUUID());
                    return req;
                });

        otpService.generateAndSendOtp("255712345678", "APP-20260810-001", "127.0.0.1");

        ArgumentCaptor<OtpRequest> captor = ArgumentCaptor.forClass(OtpRequest.class);
        verify(otpRequestRepository).save(captor.capture());
        OtpRequest saved = captor.getValue();

        assertThat(saved.getPhoneNumber()).isEqualTo("255712345678");
        assertThat(saved.getApplicationRef()).isEqualTo("APP-20260810-001");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getOtpHash()).isNotBlank();
        assertThat(saved.getSalt()).isNotBlank();
        assertThat(saved.getAttemptsRemaining()).isEqualTo(5);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());

        verify(notificationService).sendOtp(eq("255712345678"), anyString());
        verify(otpRequestRepository).supersedePendingByPhoneNumber("255712345678");
    }

    @Test
    void generateAndSendOtp_shouldThrowWhenRateLimitExceeded() {
        when(otpRequestRepository.countByPhoneNumberSince(eq("255712345678"), any()))
                .thenReturn(5L);

        assertThatThrownBy(() ->
                otpService.generateAndSendOtp("255712345678", "APP-001", "127.0.0.1"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(notificationService, never()).sendOtp(anyString(), anyString());
    }

    @Test
    void verifyOtp_shouldFailWhenNoOtpFound() {
        when(otpRequestRepository.findByApplicationRefAndStatus("APP-001", "PENDING"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                otpService.verifyOtp("APP-001", 123456, "127.0.0.1", "Chrome"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No pending OTP");
    }

    @Test
    void verifyOtp_shouldFailWhenOtpExpired() {
        OtpRequest otp = OtpRequest.builder()
                .phoneNumber("255712345678")
                .applicationRef("APP-001")
                .otpHash("hash")
                .salt("salt")
                .status("PENDING")
                .attemptsRemaining(5)
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();
        otp.setId(UUID.randomUUID());

        when(otpRequestRepository.findByApplicationRefAndStatus("APP-001", "PENDING"))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() ->
                otpService.verifyOtp("APP-001", 123456, "127.0.0.1", "Chrome"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyOtp_shouldFailWhenNoAttemptsRemaining() {
        OtpRequest otp = OtpRequest.builder()
                .phoneNumber("255712345678")
                .applicationRef("APP-001")
                .otpHash("hash")
                .salt("salt")
                .status("PENDING")
                .attemptsRemaining(0)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();
        otp.setId(UUID.randomUUID());

        when(otpRequestRepository.findByApplicationRefAndStatus("APP-001", "PENDING"))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() ->
                otpService.verifyOtp("APP-001", 123456, "127.0.0.1", "Chrome"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("attempts");
    }
}
