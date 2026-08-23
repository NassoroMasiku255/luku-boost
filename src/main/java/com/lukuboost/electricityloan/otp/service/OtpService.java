package com.lukuboost.electricityloan.otp.service;

import com.lukuboost.electricityloan.common.exception.BusinessException;
import com.lukuboost.electricityloan.common.exception.RateLimitExceededException;
import com.lukuboost.electricityloan.notification.service.NotificationService;
import com.lukuboost.electricityloan.otp.entity.OtpRequest;
import com.lukuboost.electricityloan.otp.entity.OtpVerification;
import com.lukuboost.electricityloan.otp.repository.OtpRequestRepository;
import com.lukuboost.electricityloan.otp.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRequestRepository otpRequestRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final NotificationService notificationService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-seconds:300}")
    private int expirySeconds;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${app.otp.max-requests-per-hour:5}")
    private int maxRequestsPerHour;

    @Transactional
    public OtpRequest generateAndSendOtp(String phoneNumber, String applicationRef, String ipAddress) {
        long recentCount = otpRequestRepository.countByPhoneNumberSince(
                phoneNumber, Instant.now().minus(1, ChronoUnit.HOURS));

        if (recentCount >= maxRequestsPerHour) {
            throw new RateLimitExceededException("Too many OTP requests. Please try again later.");
        }

        otpRequestRepository.supersedePendingByPhoneNumber(phoneNumber);

        String otp = generateOtp();
        String salt = generateSalt();
        String otpHash = hashOtp(otp, salt);

        OtpRequest otpRequest = OtpRequest.builder()
                .phoneNumber(phoneNumber)
                .otpHash(otpHash)
                .salt(salt)
                .otpPlain(otp)
                .purpose("LOAN_APPLICATION")
                .status("PENDING")
                .attemptsRemaining(maxAttempts)
                .applicationRef(applicationRef)
                .ipAddress(ipAddress)
                .expiresAt(Instant.now().plus(expirySeconds, ChronoUnit.SECONDS))
                .build();

        otpRequest = otpRequestRepository.save(otpRequest);

        // Send OTP via notification (never log the OTP value)
        notificationService.sendOtp(phoneNumber, otp);

        log.info("OTP generated for phone={}***, applicationRef={}", maskPhone(phoneNumber), applicationRef);

        return otpRequest;
    }

    @Transactional
    public boolean verifyOtp(String applicationRef, String otpValue, String ipAddress, String userAgent) {
        OtpRequest otpRequest = otpRequestRepository
                .findByApplicationRefAndStatus(applicationRef, "PENDING")
                .orElseThrow(() -> new BusinessException("OTP_NOT_FOUND",
                        "No pending OTP found for this application.", HttpStatus.NOT_FOUND));

        if (otpRequest.getExpiresAt().isBefore(Instant.now())) {
            otpRequest.setStatus("EXPIRED");
            otpRequestRepository.save(otpRequest);
            recordVerification(otpRequest, false, ipAddress, userAgent);
            throw new BusinessException("OTP_EXPIRED", "OTP has expired. Please request a new one.");
        }

        if (otpRequest.getAttemptsRemaining() <= 0) {
            otpRequest.setStatus("BLOCKED");
            otpRequestRepository.save(otpRequest);
            recordVerification(otpRequest, false, ipAddress, userAgent);
            throw new BusinessException("OTP_BLOCKED", "Maximum verification attempts exceeded.");
        }

        String hash = hashOtp(otpValue, otpRequest.getSalt());
        boolean valid = hash.equals(otpRequest.getOtpHash());

        if (valid) {
            otpRequest.setStatus("VERIFIED");
            otpRequest.setVerifiedAt(Instant.now());
            otpRequestRepository.save(otpRequest);
            recordVerification(otpRequest, true, ipAddress, userAgent);
            log.info("OTP verified successfully for applicationRef={}", applicationRef);
            return true;
        } else {
            otpRequest.setAttemptsRemaining(otpRequest.getAttemptsRemaining() - 1);
            otpRequestRepository.save(otpRequest);
            recordVerification(otpRequest, false, ipAddress, userAgent);
            log.warn("OTP verification failed for applicationRef={}, remaining={}",
                    applicationRef, otpRequest.getAttemptsRemaining());
            throw new BusinessException("OTP_INVALID", "Invalid OTP. " +
                    otpRequest.getAttemptsRemaining() + " attempts remaining.");
        }
    }

    private void recordVerification(OtpRequest otpRequest, boolean success, String ipAddress, String userAgent) {
        otpVerificationRepository.save(OtpVerification.builder()
                .otpRequest(otpRequest)
                .success(success)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build());
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int otp = secureRandom.nextInt(bound);
        return String.format("%0" + otpLength + "d", otp);
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashOtp(String otp, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Admin: get OTP details for a given applicationRef (including plain OTP for support).
     */
    @Transactional(readOnly = true)
    public OtpRequest getOtpForAdmin(String applicationRef) {
        return otpRequestRepository.findLatestByApplicationRef(applicationRef)
                .orElseThrow(() -> new BusinessException("OTP_NOT_FOUND",
                        "No OTP found for application: " + applicationRef, HttpStatus.NOT_FOUND));
    }

    /**
     * Resend OTP: supersede the old one and generate a fresh one.
     * Works for both customer-initiated resend and admin-initiated resend.
     */
    @Transactional
    public OtpRequest resendOtp(String applicationRef, String ipAddress) {
        OtpRequest existing = otpRequestRepository.findLatestByApplicationRef(applicationRef)
                .orElseThrow(() -> new BusinessException("OTP_NOT_FOUND",
                        "No OTP found for application: " + applicationRef, HttpStatus.NOT_FOUND));

        String phoneNumber = existing.getPhoneNumber();
        return generateAndSendOtp(phoneNumber, applicationRef, ipAddress);
    }

    public int getExpirySeconds() {
        return expirySeconds;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}
