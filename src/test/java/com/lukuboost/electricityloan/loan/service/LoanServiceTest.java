package com.lukuboost.electricityloan.loan.service;

import com.lukuboost.electricityloan.audit.service.AuditService;
import com.lukuboost.electricityloan.common.exception.BusinessException;
import com.lukuboost.electricityloan.customer.entity.Customer;
import com.lukuboost.electricityloan.customer.service.CustomerService;
import com.lukuboost.electricityloan.electricity.client.ElectricityProviderClient;
import com.lukuboost.electricityloan.electricity.dto.MeterInformation;
import com.lukuboost.electricityloan.electricity.entity.ElectricityTransaction;
import com.lukuboost.electricityloan.electricity.repository.ElectricityTransactionRepository;
import com.lukuboost.electricityloan.eligibility.dto.LoanEligibilityResult;
import com.lukuboost.electricityloan.eligibility.service.EligibilityService;
import com.lukuboost.electricityloan.loan.entity.Loan;
import com.lukuboost.electricityloan.loan.entity.LoanApplication;
import com.lukuboost.electricityloan.loan.repository.LoanApplicationRepository;
import com.lukuboost.electricityloan.loan.repository.LoanRepository;
import com.lukuboost.electricityloan.loan.repository.LoanTransactionRepository;
import com.lukuboost.electricityloan.meter.entity.Meter;
import com.lukuboost.electricityloan.notification.service.NotificationService;
import com.lukuboost.electricityloan.otp.service.OtpService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanApplicationRepository applicationRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private LoanTransactionRepository loanTransactionRepository;
    @Mock private ElectricityTransactionRepository electricityTransactionRepository;
    @Mock private CustomerService customerService;
    @Mock private OtpService otpService;
    @Mock private EligibilityService eligibilityService;
    @Mock private ElectricityProviderClient providerClient;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private LoanStateMachine stateMachine;

    private LoanService loanService;

    private Customer testCustomer;
    private Meter testMeter;
    private LoanApplication testApp;

    @BeforeEach
    void setUp() {
        MeterRegistry registry = new SimpleMeterRegistry();
        loanService = new LoanService(
                applicationRepository, loanRepository, loanTransactionRepository,
                electricityTransactionRepository, customerService, otpService,
                eligibilityService, providerClient, notificationService,
                auditService, stateMachine, registry);

        ReflectionTestUtils.setField(loanService, "minAmount", new BigDecimal("1000"));
        ReflectionTestUtils.setField(loanService, "maxAmount", new BigDecimal("500000"));
        ReflectionTestUtils.setField(loanService, "loanExpiryDays", 30);
        ReflectionTestUtils.setField(loanService, "currency", "TZS");
        ReflectionTestUtils.setField(loanService, "otpExpirySeconds", 300);

        testCustomer = Customer.builder()
                .customerRef("CUST-001")
                .phoneNumber("255712345678")
                .status("ACTIVE")
                .build();
        testCustomer.setId(1L);

        testMeter = Meter.builder()
                .meterNumber("12345678901")
                .customer(testCustomer)
                .status("ACTIVE")
                .build();
        testMeter.setId(1L);

        testApp = LoanApplication.builder()
                .applicationRef("APP-20260810-001")
                .customer(testCustomer)
                .meter(testMeter)
                .phoneNumber("255712345678")
                .meterNumber("12345678901")
                .currency("TZS")
                .status("INITIATED")
                .build();
        testApp.setId(1L);
    }

    @Test
    void checkEligibility_shouldReturnResultWithMeterInfoAndLoanLimit() {
        when(loanRepository.existsActiveByPhoneNumber("255712345678")).thenReturn(false);
        when(loanRepository.existsActiveByMeterNumber("12345678901")).thenReturn(false);
        when(customerService.findOrCreateCustomer("255712345678")).thenReturn(testCustomer);
        when(customerService.findOrCreateMeter("12345678901", testCustomer)).thenReturn(testMeter);
        when(applicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(inv -> {
                    LoanApplication app = inv.getArgument(0);
                    app.setId(1L);
                    return app;
                });

        MeterInformation meterInfo = MeterInformation.builder()
                .meterNumber("12345678901")
                .customerName("Test Customer")
                .meterType("PREPAID")
                .meterStatus("ACTIVE")
                .provider("TANESCO")
                .lastTokenAmount(new BigDecimal("50000"))
                .build();
        when(providerClient.getMeterInformation("12345678901")).thenReturn(meterInfo);

        LoanEligibilityResult eligibility = LoanEligibilityResult.builder()
                .eligible(true)
                .eligibleAmount(new BigDecimal("25000"))
                .minAmount(new BigDecimal("1000"))
                .maxAmount(new BigDecimal("500000"))
                .serviceFeePercentage(new BigDecimal("10"))
                .currency("TZS")
                .reasons(List.of("Eligible"))
                .build();
        when(eligibilityService.calculateEligibility(any(), any())).thenReturn(eligibility);

        LoanService.EligibilityCheckResult result = loanService.checkEligibility("255712345678", "12345678901", "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.isEligible()).isTrue();
        assertThat(result.getMeterInfo().getCustomerName()).isEqualTo("Test Customer");
        assertThat(result.getEligibilityResult().getEligibleAmount()).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(result.getEligibilityResult().getServiceFeePercentage()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    void checkEligibility_shouldRejectWhenPhoneHasActiveLoan() {
        when(loanRepository.existsActiveByPhoneNumber("255712345678")).thenReturn(true);

        assertThatThrownBy(() ->
                loanService.checkEligibility("255712345678", "12345678901", "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void checkEligibility_shouldRejectWhenMeterHasActiveLoan() {
        when(loanRepository.existsActiveByPhoneNumber("255712345678")).thenReturn(false);
        when(loanRepository.existsActiveByMeterNumber("12345678901")).thenReturn(true);

        assertThatThrownBy(() ->
                loanService.checkEligibility("255712345678", "12345678901", "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void requestLoan_shouldRejectBelowMinimumAmount() {
        testApp.setStatus("ELIGIBILITY_CHECKED");
        testApp.setEligibleAmount(new BigDecimal("50000"));
        when(applicationRepository.findByApplicationRef("APP-20260810-001"))
                .thenReturn(Optional.of(testApp));
        when(applicationRepository.findByIdempotencyKey("idem-key-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                loanService.requestLoan("255712345678", "12345678901",
                        new BigDecimal("50000"), new BigDecimal("500"),
                        "APP-20260810-001", "idem-key-1", "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void requestLoan_shouldRejectAboveEligibleAmount() {
        testApp.setStatus("ELIGIBILITY_CHECKED");
        testApp.setEligibleAmount(new BigDecimal("50000"));
        when(applicationRepository.findByApplicationRef("APP-20260810-001"))
                .thenReturn(Optional.of(testApp));
        when(applicationRepository.findByIdempotencyKey("idem-key-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                loanService.requestLoan("255712345678", "12345678901",
                        new BigDecimal("50000"), new BigDecimal("60000"),
                        "APP-20260810-001", "idem-key-1", "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void verifyOtpAndDisburse_shouldRejectWhenNotInOtpPendingState() {
        testApp.setStatus("ELIGIBILITY_CHECKED");
        when(applicationRepository.findByApplicationRef("APP-20260810-001"))
                .thenReturn(Optional.of(testApp));

        assertThatThrownBy(() ->
                loanService.verifyOtpAndDisburse("APP-20260810-001", 123456, "127.0.0.1", "Chrome"))
                .isInstanceOf(BusinessException.class);
    }
}
