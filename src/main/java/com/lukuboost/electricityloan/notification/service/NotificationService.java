package com.lukuboost.electricityloan.notification.service;

import com.lukuboost.electricityloan.notification.entity.Notification;
import com.lukuboost.electricityloan.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendOtp(String phoneNumber, String otp) {
        // In production, integrate with an SMS gateway
        // Never log the OTP value
        Notification notification = Notification.builder()
                .recipient(phoneNumber)
                .channel("SMS")
                .template("OTP_VERIFICATION")
                .content("Your LUKU Boost verification code is: " + otp + ". Valid for 5 minutes.")
                .status("SENT")
                .correlationId(MDC.get("correlationId"))
                .sentAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("OTP notification queued for delivery to phone=***");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendLoanApproval(String phoneNumber, String loanRef, String token) {
        String maskedToken = token != null && token.length() > 8
                ? token.substring(0, 4) + "****" + token.substring(token.length() - 4)
                : "****";

        Notification notification = Notification.builder()
                .recipient(phoneNumber)
                .channel("SMS")
                .template("LOAN_APPROVAL")
                .content("Your LUKU Boost loan " + loanRef + " has been approved. Token: " + token)
                .status("SENT")
                .correlationId(MDC.get("correlationId"))
                .sentAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Loan approval notification sent for loanRef={}, token={}****", loanRef, maskedToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendLoanFailure(String phoneNumber, String reason) {
        Notification notification = Notification.builder()
                .recipient(phoneNumber)
                .channel("SMS")
                .template("LOAN_FAILURE")
                .content("Your LUKU Boost loan request could not be processed. " + reason)
                .status("SENT")
                .correlationId(MDC.get("correlationId"))
                .sentAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Loan failure notification sent");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendRepaymentConfirmation(String phoneNumber, String loanRef, String amount) {
        Notification notification = Notification.builder()
                .recipient(phoneNumber)
                .channel("SMS")
                .template("REPAYMENT_CONFIRMATION")
                .content("Payment of " + amount + " received for loan " + loanRef + ". Thank you!")
                .status("SENT")
                .correlationId(MDC.get("correlationId"))
                .sentAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Repayment confirmation notification sent for loanRef={}", loanRef);
    }
}
