package com.lukuboost.electricityloan.eligibility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukuboost.electricityloan.common.repository.LoanRuleConfigurationRepository;
import com.lukuboost.electricityloan.electricity.dto.MeterInformation;
import com.lukuboost.electricityloan.eligibility.dto.LoanEligibilityResult;
import com.lukuboost.electricityloan.loan.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

    @Mock
    private LoanRuleConfigurationRepository ruleRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eligibilityService, "defaultMinAmount", new BigDecimal("1000"));
        ReflectionTestUtils.setField(eligibilityService, "defaultMaxAmount", new BigDecimal("500000"));
        ReflectionTestUtils.setField(eligibilityService, "defaultPercentage", new BigDecimal("50"));
        ReflectionTestUtils.setField(eligibilityService, "maxOutstandingLoans", 1);
        ReflectionTestUtils.setField(eligibilityService, "currency", "TZS");
    }

    @Test
    void calculateEligibility_shouldReturnEligibleForValidMeter() {
        Long customerId = 1L;
        MeterInformation meterInfo = MeterInformation.builder()
                .meterNumber("12345678901")
                .customerName("John Doe")
                .meterType("PREPAID")
                .meterStatus("ACTIVE")
                .provider("TANESCO")
                .lastTokenAmount(new BigDecimal("25000"))
                .lastPurchaseDate(Instant.now().minus(10, ChronoUnit.DAYS))
                .averagePurchaseAmount(new BigDecimal("30000"))
                .build();

        when(ruleRepository.findActiveRules()).thenReturn(Collections.emptyList());
        when(loanRepository.countByCustomerIdAndStatus(eq(customerId), eq("ACTIVE")))
                .thenReturn(0L);

        LoanEligibilityResult result = eligibilityService.calculateEligibility(meterInfo, customerId);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getEligibleAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.getMinAmount()).isEqualTo(new BigDecimal("1000"));
        assertThat(result.getCurrency()).isEqualTo("TZS");
    }

    @Test
    void calculateEligibility_shouldReturnNotEligibleWhenTooManyActiveLoans() {
        Long customerId = 1L;
        MeterInformation meterInfo = MeterInformation.builder()
                .meterNumber("12345678901")
                .customerName("John Doe")
                .meterType("PREPAID")
                .meterStatus("ACTIVE")
                .provider("TANESCO")
                .lastTokenAmount(new BigDecimal("25000"))
                .lastPurchaseDate(Instant.now().minus(10, ChronoUnit.DAYS))
                .averagePurchaseAmount(new BigDecimal("30000"))
                .build();

        when(ruleRepository.findActiveRules()).thenReturn(Collections.emptyList());
        when(loanRepository.countByCustomerIdAndStatus(eq(customerId), eq("ACTIVE")))
                .thenReturn(1L);

        LoanEligibilityResult result = eligibilityService.calculateEligibility(meterInfo, customerId);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getFailureReasons()).isNotEmpty();
    }

    @Test
    void calculateEligibility_shouldReturnNotEligibleForInactiveMeter() {
        Long customerId = 1L;
        MeterInformation meterInfo = MeterInformation.builder()
                .meterNumber("12345678901")
                .customerName("John Doe")
                .meterType("PREPAID")
                .meterStatus("DISCONNECTED")
                .provider("TANESCO")
                .lastTokenAmount(new BigDecimal("5000"))
                .lastPurchaseDate(Instant.now().minus(180, ChronoUnit.DAYS))
                .averagePurchaseAmount(new BigDecimal("5000"))
                .build();

        when(ruleRepository.findActiveRules()).thenReturn(Collections.emptyList());

        LoanEligibilityResult result = eligibilityService.calculateEligibility(meterInfo, customerId);

        assertThat(result.isEligible()).isFalse();
    }
}
