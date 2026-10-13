package com.lukuboost.electricityloan.notification.service;

import com.lukuboost.electricityloan.notification.config.WhatsAppProperties;
import com.lukuboost.electricityloan.notification.entity.Notification;
import com.lukuboost.electricityloan.notification.repository.NotificationRepository;
import com.lukuboost.electricityloan.electricity.entity.ElectricityTransaction;
import com.lukuboost.electricityloan.loan.entity.Loan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WhatsAppProperties whatsAppProps;

    private final RestClient restClient = buildRestClient();

    private static RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory).build();
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy")
            .withZone(ZoneId.of("UTC"));
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm O")
            .withZone(ZoneId.of("Africa/Dar_es_Salaam"));

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
        boolean resp = sendOtpWhatsApp(phoneNumber, otp);
        log.info("whatsap message response: {}",resp);
        notificationRepository.save(notification);
        log.info("OTP notification queued for delivery to phone={}",phoneNumber);
    }

    /**
     * Sends the OTP through the WhatsApp authentication template.
     * Not @Transactional on purpose: no DB connection is held during the HTTP call.
     *
     * @return true if WhatsApp accepted the message
     */
    public boolean sendOtpWhatsApp(String phoneNumber, String otp) {
        String to = normalizePhone(phoneNumber);

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", to,
                "type", "template",
                "template", Map.of(
                        "name", whatsAppProps.otpTemplateName(),
                        "language", Map.of("code", whatsAppProps.otpTemplateLanguage()),
                        "components", List.of(
                                Map.of(
                                        "type", "body",
                                        "parameters", List.of(Map.of("type", "text", "text", otp))),
                                Map.of(
                                        "type", "button",
                                        "sub_type", "url",
                                        "index", "0",
                                        "parameters", List.of(Map.of("type", "text", "text", otp)))
                        )
                )
        );

        String status = "SENT";
        try {
            restClient.post()
                    .uri(whatsAppProps.apiUrl())
                    .headers(h -> h.setBearerAuth(whatsAppProps.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("WhatsApp OTP accepted by Meta for phone=***{}", last3(to));
        } catch (RestClientResponseException e) {
            status = "FAILED";
            log.error("WhatsApp OTP rejected: status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            status = "FAILED";
            log.error("WhatsApp OTP call failed: {}", e.getMessage());
        }

        // Never store the OTP value in the notification record
        notificationRepository.save(Notification.builder()
                .recipient(to)
                .channel("WHATSAPP")
                .template(whatsAppProps.otpTemplateName())
                .content("OTP sent via WhatsApp template")
                .status(status)
                .correlationId(MDC.get("correlationId"))
                .sentAt(Instant.now())
                .build());

        return "SENT".equals(status);
    }

    /** Returns digits only, in international format, e.g. 0750960863 / +255750960863 -> 255750960863 */
    private String normalizePhone(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        String cc = whatsAppProps.defaultCountryCode();
        if (digits.startsWith("0")) {
            digits = cc + digits.substring(1);      // local format with leading 0
        } else if (digits.length() == 9) {
            digits = cc + digits;                   // local format without leading 0
        }
        if (digits.length() < 11 || digits.length() > 15) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        return digits;
    }

    private String last3(String s) {
        return s.substring(s.length() - 3);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendLoanApproval(Loan loan, ElectricityTransaction elecTxn) {
        String phoneNumber = loan.getApplication().getPhoneNumber();
        String loanRef = loan.getLoanRef();
        String token = elecTxn.getToken();

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

        boolean resp = sendLoanApprovalWhatsApp(loan, elecTxn);
        log.info("WhatsApp loan approval message response: {}", resp);

        notificationRepository.save(notification);
        log.info("Loan approval notification sent for loanRef={}, token={}****", loanRef, maskedToken);
    }

    public boolean sendLoanApprovalWhatsApp(Loan loan, ElectricityTransaction elecTxn) {
        String to = normalizePhone(loan.getApplication().getPhoneNumber());

        List<Map<String, String>> parameters = List.of(
                parameter(loan.getCustomer().getFirstName() != null ? loan.getCustomer().getFirstName() : "Customer"),
                parameter(loan.getLoanRef()),
                parameter(loan.getApplication().getApplicationRef()),
                parameter(loan.getStatus()),
                parameter(String.format("%,.0f", loan.getPrincipalAmount())),
                parameter(String.format("%,.0f", loan.getFeeAmount())),
                parameter(String.format("%,.0f", loan.getTotalDue())),
                parameter(String.format("%,.0f", loan.getOutstandingAmount())),
                parameter(DATE_FORMATTER.format(loan.getDueAt())),
                parameter(loan.getMeter().getMeterNumber()),
                parameter(elecTxn.getToken()),
                parameter(elecTxn.getTokenUnits() != null ? elecTxn.getTokenUnits() : ""),
                parameter(DATE_TIME_FORMATTER.format(loan.getIssuedAt()))
        );

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", to,
                "type", "template",
                "template", Map.of(
                        "name", whatsAppProps.loanApprovedTemplateName(),
                        "language", Map.of("code", whatsAppProps.loanApprovedTemplateLanguage()),
                        "components", List.of(
                                Map.of(
                                        "type", "body",
                                        "parameters", parameters))
                )
        );

        String status = "SENT";
        try {
            restClient.post()
                    .uri(whatsAppProps.apiUrl())
                    .headers(h -> h.setBearerAuth(whatsAppProps.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("WhatsApp Loan Approval accepted by Meta for phone=***{}", last3(to));
        } catch (RestClientResponseException e) {
            status = "FAILED";
            log.error("WhatsApp Loan Approval rejected: status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            status = "FAILED";
            log.error("WhatsApp Loan Approval call failed: {}", e.getMessage());
        }

        notificationRepository.save(Notification.builder()
                .recipient(to)
                .channel("WHATSAPP")
                .template(whatsAppProps.loanApprovedTemplateName())
                .content("Loan approval sent via WhatsApp template")
                .status(status)
                .correlationId(MDC.get("correlationId"))
                .sentAt(Instant.now())
                .build());

        return "SENT".equals(status);
    }

    private Map<String, String> parameter(String text) {
        return Map.of("type", "text", "text", text != null ? text : "");
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