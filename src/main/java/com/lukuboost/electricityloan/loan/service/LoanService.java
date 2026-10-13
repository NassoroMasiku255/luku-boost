package com.lukuboost.electricityloan.loan.service;

import com.lukuboost.electricityloan.audit.service.AuditService;
import com.lukuboost.electricityloan.common.exception.BusinessException;
import com.lukuboost.electricityloan.common.exception.DuplicateOperationException;
import com.lukuboost.electricityloan.common.exception.ResourceNotFoundException;
import com.lukuboost.electricityloan.common.utility.ReferenceGenerator;
import com.lukuboost.electricityloan.customer.entity.Customer;
import com.lukuboost.electricityloan.customer.service.CustomerService;
import com.lukuboost.electricityloan.electricity.client.ElectricityProviderClient;
import com.lukuboost.electricityloan.electricity.dto.ElectricityTokenPurchase;
import com.lukuboost.electricityloan.electricity.dto.MeterInformation;
import com.lukuboost.electricityloan.electricity.entity.ElectricityTransaction;
import com.lukuboost.electricityloan.electricity.repository.ElectricityTransactionRepository;
import com.lukuboost.electricityloan.eligibility.dto.LoanEligibilityResult;
import com.lukuboost.electricityloan.eligibility.service.EligibilityService;
import com.lukuboost.electricityloan.loan.entity.Loan;
import com.lukuboost.electricityloan.loan.entity.LoanApplication;
import com.lukuboost.electricityloan.loan.entity.LoanTransaction;
import com.lukuboost.electricityloan.loan.repository.LoanApplicationRepository;
import com.lukuboost.electricityloan.loan.repository.LoanRepository;
import com.lukuboost.electricityloan.loan.repository.LoanTransactionRepository;
import com.lukuboost.electricityloan.meter.entity.Meter;
import com.lukuboost.electricityloan.notification.service.NotificationService;
import com.lukuboost.electricityloan.otp.service.OtpService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
public class LoanService {

    private final LoanApplicationRepository applicationRepository;
    private final LoanRepository loanRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final ElectricityTransactionRepository electricityTransactionRepository;
    private final CustomerService customerService;
    private final OtpService otpService;
    private final EligibilityService eligibilityService;
    private final ElectricityProviderClient providerClient;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final LoanStateMachine stateMachine;

    private final Counter loansCreatedCounter;
    private final Counter loansCompletedCounter;
    private final Counter loansFailedCounter;

    @Value("${app.loan.min-amount:1000}")
    private BigDecimal minAmount;

    @Value("${app.loan.max-amount:500000}")
    private BigDecimal maxAmount;

    @Value("${app.loan.loan-expiry-days:30}")
    private int loanExpiryDays;

    @Value("${app.loan.currency:TZS}")
    private String currency;

    @Value("${app.otp.expiry-seconds:300}")
    private int otpExpirySeconds;

    public LoanService(LoanApplicationRepository applicationRepository,
                        LoanRepository loanRepository,
                        LoanTransactionRepository loanTransactionRepository,
                        ElectricityTransactionRepository electricityTransactionRepository,
                        CustomerService customerService,
                        OtpService otpService,
                        EligibilityService eligibilityService,
                        ElectricityProviderClient providerClient,
                        NotificationService notificationService,
                        AuditService auditService,
                        LoanStateMachine stateMachine,
                        MeterRegistry meterRegistry) {
        this.applicationRepository = applicationRepository;
        this.loanRepository = loanRepository;
        this.loanTransactionRepository = loanTransactionRepository;
        this.electricityTransactionRepository = electricityTransactionRepository;
        this.customerService = customerService;
        this.otpService = otpService;
        this.eligibilityService = eligibilityService;
        this.providerClient = providerClient;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.stateMachine = stateMachine;

        this.loansCreatedCounter = Counter.builder("loan.created").register(meterRegistry);
        this.loansCompletedCounter = Counter.builder("loan.completed").register(meterRegistry);
        this.loansFailedCounter = Counter.builder("loan.failed").register(meterRegistry);
    }

    // ========================================================================
    // STEP 1: Eligibility Check
    // Receives phoneNumber and meterNumber. Validates no existing active loan
    // for both, verifies meter via third party, returns meter info + loan limit.
    // ========================================================================
    @Transactional
    public EligibilityCheckResult checkEligibility(String phoneNumber, String meterNumber, String ipAddress) {
        log.info("Eligibility check requested for phone={}***, meter={}", maskPhone(phoneNumber), meterNumber);

        // 1. Check if phone number already has an active loan
        if (loanRepository.existsActiveByPhoneNumber(phoneNumber)) {
            throw new BusinessException("PHONE_HAS_ACTIVE_LOAN",
                    "This phone number already has an outstanding loan. Please repay first.", HttpStatus.CONFLICT);
        }

        // 2. Check if meter number already has an active loan
        if (loanRepository.existsActiveByMeterNumber(meterNumber)) {
            throw new BusinessException("METER_HAS_ACTIVE_LOAN",
                    "This meter number already has an outstanding loan. Please repay first.", HttpStatus.CONFLICT);
        }

        // 3. Validate phone number format
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new BusinessException("INVALID_PHONE_NUMBER",
                    "The provided phone number is not valid.", HttpStatus.BAD_REQUEST);
        }

        // 4. Validate meter number format
        if (!isValidMeterNumber(meterNumber)) {
            throw new BusinessException("INVALID_METER_NUMBER",
                    "The provided meter number is not valid.", HttpStatus.BAD_REQUEST);
        }

        // 5. Find or create customer and meter
        Customer customer = customerService.findOrCreateCustomer(phoneNumber);
        if (!"ACTIVE".equals(customer.getStatus())) {
            throw new BusinessException("CUSTOMER_NOT_ACTIVE",
                    "Customer account is not active.", HttpStatus.FORBIDDEN);
        }
        Meter meter = customerService.findOrCreateMeter(meterNumber, customer);

        // 6. Fetch meter information from third party
        MeterInformation meterInfo;
        try {
            meterInfo = providerClient.getMeterInformation(meterNumber);
        } catch (Exception e) {
            log.error("Failed to get meter information for meter={}", meterNumber, e);
            throw new BusinessException("PROVIDER_ERROR",
                    "Unable to retrieve meter information. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        // 7. Update meter with latest info from provider
        meter.setLastTokenAmount(meterInfo.getLastTokenAmount());
        meter.setLastTokenDate(meterInfo.getLastPurchaseDate());
        meter.setMeterType(meterInfo.getMeterType());
        meter.setProvider(meterInfo.getProvider());

        // 8. Calculate eligibility using rules engine
        LoanEligibilityResult eligibilityResult = eligibilityService.calculateEligibility(
                meterInfo, customer.getId());

        // 9. Create loan application record
        String appRef = ReferenceGenerator.generateApplicationReference();
        LoanApplication application = LoanApplication.builder()
                .applicationRef(appRef)
                .customer(customer)
                .meter(meter)
                .phoneNumber(phoneNumber)
                .meterNumber(meterNumber)
                .currency(currency)
                .status("INITIATED")
                .correlationId(MDC.get("correlationId"))
                .build();
        application = applicationRepository.save(application);

        if (eligibilityResult.isEligible()) {
            application.setEligibleAmount(eligibilityResult.getEligibleAmount());
            application.setEligibilityRef(ReferenceGenerator.generateCorrelationId());
            transitionApplication(application, "ELIGIBILITY_CHECKED");
        } else {
            transitionApplication(application, "FAILED");
            application.setFailureReason(String.join("; ", eligibilityResult.getFailureReasons()));
            applicationRepository.save(application);
        }

        auditService.logSystemEvent("ELIGIBILITY_CHECK", "LOAN_APPLICATION",
                application.getId().toString(),
                "Eligibility check for meter " + meterNumber + ": " +
                        (eligibilityResult.isEligible() ? "ELIGIBLE" : "NOT_ELIGIBLE"));

        log.info("Eligibility check completed: ref={}, eligible={}", appRef, eligibilityResult.isEligible());

        return EligibilityCheckResult.builder()
                .applicationRef(appRef)
                .eligible(eligibilityResult.isEligible())
                .meterInfo(meterInfo)
                .eligibilityResult(eligibilityResult)
                .build();
    }

    // ========================================================================
    // STEP 2: Loan Request
    // Receives phone, meter, limit, borrowedAmount, applicationRef, idempotencyKey.
    // Saves into transaction, sends OTP to customer phone.
    // ========================================================================
    @Transactional
    public LoanApplication requestLoan(String phoneNumber, String meterNumber, BigDecimal loanLimit,
                                        BigDecimal borrowedAmount, String applicationRef,
                                        String idempotencyKey, String ipAddress) {
        log.info("Loan request for applicationRef={}, amount={}", applicationRef, borrowedAmount);

        // Idempotency check
        LoanApplication existingApp = applicationRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existingApp != null) {
            log.info("Idempotent request detected for key={}", idempotencyKey);
            throw new DuplicateOperationException("This loan request has already been processed.");
        }

        LoanApplication application = getApplicationByRef(applicationRef);

        // Validate application state
        if (!"ELIGIBILITY_CHECKED".equals(application.getStatus())) {
            throw new BusinessException("INVALID_APPLICATION_STATE",
                    "Application must be in ELIGIBILITY_CHECKED state. Current: " + application.getStatus());
        }

        // Validate phone and meter match the application
        if (!application.getPhoneNumber().equals(phoneNumber.trim())) {
            throw new BusinessException("PHONE_MISMATCH",
                    "Phone number does not match the application.", HttpStatus.BAD_REQUEST);
        }
        if (!application.getMeterNumber().equals(meterNumber.trim())) {
            throw new BusinessException("METER_MISMATCH",
                    "Meter number does not match the application.", HttpStatus.BAD_REQUEST);
        }

        // Validate borrowed amount
        if (borrowedAmount.compareTo(minAmount) < 0) {
            throw new BusinessException("AMOUNT_BELOW_MINIMUM",
                    "Amount " + borrowedAmount + " is below minimum " + minAmount);
        }
        if (borrowedAmount.compareTo(loanLimit) > 0) {
            throw new BusinessException("LOAN_LIMIT_EXCEEDED",
                    "Requested amount " + borrowedAmount + " exceeds the loan limit of " + loanLimit);
        }
        if (borrowedAmount.compareTo(maxAmount) > 0) {
            throw new BusinessException("AMOUNT_ABOVE_MAXIMUM",
                    "Amount " + borrowedAmount + " exceeds maximum " + maxAmount);
        }

        // Update application with request details
        application.setRequestedAmount(borrowedAmount);
        application.setEligibleAmount(loanLimit);
        application.setIdempotencyKey(idempotencyKey);

        // Save electricity transaction in PENDING state (pre-OTP)
        String tokenRef = ReferenceGenerator.generateTokenReference();
        ElectricityTransaction elecTxn = ElectricityTransaction.builder()
                .internalRef(tokenRef)
                .application(application)
                .meterNumber(meterNumber.trim())
                .amount(borrowedAmount)
                .currency(currency)
                .requestStatus("PENDING")
                .idempotencyKey(idempotencyKey + "-token")
                .correlationId(MDC.get("correlationId"))
                .build();
        electricityTransactionRepository.save(elecTxn);

        // Transition to OTP_PENDING and send OTP
        transitionApplication(application, "OTP_PENDING");
        otpService.generateAndSendOtp(phoneNumber.trim(), applicationRef, ipAddress);

        loansCreatedCounter.increment();
        auditService.logSystemEvent("LOAN_REQUESTED", "LOAN_APPLICATION",
                application.getId().toString(),
                "Loan requested: amount=" + borrowedAmount + ", OTP sent");

        log.info("Loan request saved, OTP sent for applicationRef={}", applicationRef);
        return application;
    }

    // ========================================================================
    // STEP 3: OTP Verification & Disbursement
    // Receives OTP and applicationRef. Verifies OTP, then sends purchase request
    // to third party, updates transaction, and saves loan on success.
    // ========================================================================
    @Transactional
    public Loan verifyOtpAndDisburse(String applicationRef, String otp, String ipAddress, String userAgent) {
        log.info("OTP verification and disbursement for applicationRef={}", applicationRef);

        LoanApplication application = getApplicationByRef(applicationRef);

        // Validate state
        if (!"OTP_PENDING".equals(application.getStatus())) {
            throw new BusinessException("INVALID_APPLICATION_STATE",
                    "Application is not in OTP_PENDING state. Current: " + application.getStatus());
        }

        // Verify OTP
        otpService.verifyOtp(applicationRef, otp, ipAddress, userAgent);
        transitionApplication(application, "OTP_VERIFIED");

        // Get the pending electricity transaction for this application
        ElectricityTransaction elecTxn = electricityTransactionRepository
                .findByApplicationId(application.getId())
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND",
                        "No pending transaction found for this application.", HttpStatus.NOT_FOUND));

        BigDecimal borrowedAmount = application.getRequestedAmount();
        String idempotencyKey = application.getIdempotencyKey();

        // Calculate fee
        BigDecimal feePercentage = new BigDecimal("10");
        BigDecimal feeAmount = borrowedAmount.multiply(feePercentage)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal totalDue = borrowedAmount.add(feeAmount);

        // Transition to PROCESSING
        transitionApplication(application, "PROCESSING");

        // Send request to third party
        elecTxn.setRequestStatus("SENT");
        elecTxn.setRequestTimestamp(Instant.now());
        electricityTransactionRepository.save(elecTxn);

        transitionApplication(application, "TOKEN_REQUESTED");

        try {
            ElectricityTokenPurchase purchase = providerClient.purchaseElectricity(
                    application.getMeterNumber(), borrowedAmount, elecTxn.getInternalRef());

            elecTxn.setResponseTimestamp(Instant.now());

            if (purchase.isSuccess()) {
                // Update transaction with success
                elecTxn.setRequestStatus("SUCCESS");
                elecTxn.setProviderRef(purchase.getProviderTransactionRef());
                elecTxn.setToken(purchase.getToken());
                elecTxn.setTokenUnits(purchase.getTokenUnits());
                elecTxn.setProviderStatus("SUCCESS");

                // Create loan record
                String loanRef = ReferenceGenerator.generateLoanReference();
                Loan loan = Loan.builder()
                        .loanRef(loanRef)
                        .customer(application.getCustomer())
                        .meter(application.getMeter())
                        .application(application)
                        .principalAmount(borrowedAmount)
                        .feeAmount(feeAmount)
                        .totalDue(totalDue)
                        .outstandingAmount(totalDue)
                        .currency(currency)
                        .status("ACTIVE")
                        .issuedAt(Instant.now())
                        .dueAt(Instant.now().plus(loanExpiryDays, ChronoUnit.DAYS))
                        .build();
                loan = loanRepository.save(loan);

                // Link transaction to loan
                elecTxn.setLoan(loan);
                electricityTransactionRepository.save(elecTxn);

                // Create loan transaction record
                loanTransactionRepository.save(LoanTransaction.builder()
                        .transactionRef(ReferenceGenerator.generateTransactionReference())
                        .loan(loan)
                        .transactionType("DISBURSEMENT")
                        .amount(borrowedAmount)
                        .currency(currency)
                        .status("COMPLETED")
                        .idempotencyKey(idempotencyKey + "-disbursement")
                        .description("Electricity token purchase")
                        .build());

                // Update application
                application.setApprovedAmount(borrowedAmount);
                transitionApplication(application, "TOKEN_RECEIVED");
                transitionApplication(application, "COMPLETED");

                // Notify customer
                notificationService.sendLoanApproval(loan, elecTxn);

                loansCompletedCounter.increment();
                auditService.logSystemEvent("LOAN_COMPLETED", "LOAN", loan.getId().toString(),
                        "Loan completed: " + loanRef + ", token delivered");

                log.info("Loan completed: ref={}, tokenRef={}", loanRef, elecTxn.getInternalRef());
                
                // Initialize meter to avoid LazyInitializationException in controller
                if (loan.getMeter() != null) {
                    loan.getMeter().getMeterNumber();
                }
                
                return loan;

            } else {
                handlePurchaseFailure(elecTxn, application, null,
                        purchase.getStatusMessage() != null ? purchase.getStatusMessage() : "Provider returned failure");
                throw new BusinessException("TOKEN_PURCHASE_FAILED",
                        "Electricity token purchase failed. Please try again.", HttpStatus.SERVICE_UNAVAILABLE);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Electricity purchase failed for applicationRef={}", applicationRef, e);

            elecTxn.setResponseTimestamp(Instant.now());
            elecTxn.setRequestStatus("UNKNOWN");
            elecTxn.setFailureReason(e.getMessage());
            electricityTransactionRepository.save(elecTxn);

            transitionApplication(application, "UNKNOWN");
            loansFailedCounter.increment();

            auditService.logSystemEvent("LOAN_PURCHASE_UNKNOWN", "LOAN_APPLICATION",
                    application.getId().toString(),
                    "Electricity purchase outcome unknown: " + e.getMessage());

            throw new BusinessException("TOKEN_PURCHASE_UNKNOWN",
                    "Token purchase status unknown. We will update you shortly.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // ========================================================================
    // Helper / Query methods
    // ========================================================================

    public LoanApplication getApplicationByRef(String applicationRef) {
        return applicationRepository.findByApplicationRef(applicationRef)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", applicationRef));
    }

    public Loan getLoanByRef(String loanRef) {
        return loanRepository.findByLoanRefWithMeter(loanRef)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanRef));
    }

    @Transactional
    public ElectricityTransaction getElectricityTransactionByLoanId(UUID loanId) {
        return electricityTransactionRepository.findAll().stream()
                .filter(et -> et.getLoan() != null && et.getLoan().getId().equals(loanId))
                .findFirst().orElse(null);
    }

    private void handlePurchaseFailure(ElectricityTransaction elecTxn, LoanApplication application,
                                        Loan loan, String reason) {
        elecTxn.setRequestStatus("FAILED");
        elecTxn.setFailureReason(reason);
        electricityTransactionRepository.save(elecTxn);

        transitionApplication(application, "FAILED");
        application.setFailureReason(reason);
        applicationRepository.save(application);

        loansFailedCounter.increment();
        notificationService.sendLoanFailure(application.getPhoneNumber(),
                "Electricity token purchase failed. Please try again.");

        if (loan != null) {
            auditService.logSystemEvent("LOAN_PURCHASE_FAILED", "LOAN", loan.getId().toString(),
                    "Electricity purchase failed for loan " + loan.getLoanRef() + ": " + reason);
        } else {
            auditService.logSystemEvent("LOAN_PURCHASE_FAILED", "LOAN_APPLICATION",
                    application.getId().toString(),
                    "Electricity purchase failed for application " + application.getApplicationRef() + ": " + reason);
        }
    }

    private void transitionApplication(LoanApplication application, String targetStatus) {
        stateMachine.validateTransition(application.getStatus(), targetStatus);
        application.setStatus(targetStatus);
        applicationRepository.save(application);
    }

    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return false;
        String cleaned = phone.replaceAll("[\\s\\-+]", "");
        return cleaned.matches("\\d{9,15}");
    }

    private boolean isValidMeterNumber(String meter) {
        if (meter == null || meter.isBlank()) return false;
        String cleaned = meter.trim();
        return cleaned.matches("[A-Za-z0-9\\-]{5,30}");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    // Inner DTO for eligibility result bundling
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EligibilityCheckResult {
        private String applicationRef;
        private boolean eligible;
        private MeterInformation meterInfo;
        private LoanEligibilityResult eligibilityResult;
    }
}
