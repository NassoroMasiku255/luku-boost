package com.lukuboost.electricityloan.eligibility.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukuboost.electricityloan.common.entity.LoanRuleConfiguration;
import com.lukuboost.electricityloan.common.repository.LoanRuleConfigurationRepository;
import com.lukuboost.electricityloan.common.repository.SystemConfigurationRepository;
import com.lukuboost.electricityloan.electricity.dto.MeterInformation;
import com.lukuboost.electricityloan.eligibility.dto.LoanEligibilityResult;
import com.lukuboost.electricityloan.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EligibilityService {

    private final LoanRuleConfigurationRepository ruleRepository;
    private final SystemConfigurationRepository systemConfigRepository;
    private final LoanRepository loanRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.loan.min-amount:1000}")
    private BigDecimal defaultMinAmount;

    @Value("${app.loan.max-amount:500000}")
    private BigDecimal defaultMaxAmount;

    @Value("${app.loan.default-percentage:50}")
    private BigDecimal defaultPercentage;

    @Value("${app.loan.max-outstanding-loans:1}")
    private int maxOutstandingLoans;

    @Value("${app.loan.currency:TZS}")
    private String currency;

    public LoanEligibilityResult calculateEligibility(MeterInformation meterInfo, UUID customerId) {
        log.info("Calculating eligibility for meter={}, customer={}", meterInfo.getMeterNumber(), customerId);

        BigDecimal serviceFee = getServiceFeePercentage();

        LoanEligibilityResult.LoanEligibilityResultBuilder result = LoanEligibilityResult.builder()
                .currency(currency)
                .minAmount(defaultMinAmount)
                .maxAmount(defaultMaxAmount)
                .serviceFeePercentage(serviceFee);

        List<LoanRuleConfiguration> rules = ruleRepository.findActiveRules();

        // Check meter status
        if (!"ACTIVE".equalsIgnoreCase(meterInfo.getMeterStatus())) {
            return result.eligible(false)
                    .failureReasons(List.of("Meter is not active. Status: " + meterInfo.getMeterStatus()))
                    .build();
        }

        // Check outstanding loans
        long outstandingCount = loanRepository.countByCustomerIdAndStatus(customerId, "ACTIVE");
        if (outstandingCount >= maxOutstandingLoans) {
            return result.eligible(false)
                    .failureReasons(List.of("Customer has " + outstandingCount + " outstanding loan(s). Maximum allowed: " + maxOutstandingLoans))
                    .build();
        }

        // Check last purchase data
        if (meterInfo.getLastTokenAmount() == null || meterInfo.getLastTokenAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return result.eligible(false)
                    .failureReasons(List.of("No previous electricity purchase found for this meter."))
                    .build();
        }

        // Apply configurable rules
        BigDecimal percentage = defaultPercentage;
        BigDecimal minPurchase = BigDecimal.ZERO;
        int maxDaysSincePurchase = 90;

        for (LoanRuleConfiguration rule : rules) {
            Map<String, Object> ruleValues = parseRuleValue(rule.getRuleValue());

            switch (rule.getRuleName()) {
                case "DEFAULT_ELIGIBILITY_PERCENTAGE":
                    if (ruleValues.containsKey("percentage")) {
                        percentage = new BigDecimal(ruleValues.get("percentage").toString());
                    }
                    break;
                case "MIN_LAST_PURCHASE_AMOUNT":
                    if (ruleValues.containsKey("minAmount")) {
                        minPurchase = new BigDecimal(ruleValues.get("minAmount").toString());
                    }
                    break;
                case "MAX_DAYS_SINCE_PURCHASE":
                    if (ruleValues.containsKey("maxDays")) {
                        maxDaysSincePurchase = Integer.parseInt(ruleValues.get("maxDays").toString());
                    }
                    break;
            }
        }

        // Validate minimum purchase amount
        if (meterInfo.getLastTokenAmount().compareTo(minPurchase) < 0) {
            return result.eligible(false)
                    .failureReasons(List.of("Last purchase amount (" + meterInfo.getLastTokenAmount() +
                            ") is below minimum required (" + minPurchase + ")."))
                    .build();
        }

        // Validate days since last purchase
        if (meterInfo.getLastPurchaseDate() != null) {
            long daysSince = ChronoUnit.DAYS.between(meterInfo.getLastPurchaseDate(), Instant.now());
            if (daysSince > maxDaysSincePurchase) {
                return result.eligible(false)
                        .failureReasons(List.of("Last purchase was " + daysSince + " days ago. Maximum allowed: " + maxDaysSincePurchase))
                        .build();
            }
        }

        // Calculate eligible amount
        BigDecimal eligibleAmount = meterInfo.getLastTokenAmount()
                .multiply(percentage)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        // Apply limits
        if (eligibleAmount.compareTo(defaultMinAmount) < 0) {
            return result.eligible(false)
                    .failureReasons(List.of("Calculated eligible amount (" + eligibleAmount +
                            ") is below minimum loan amount (" + defaultMinAmount + ")."))
                    .build();
        }

        if (eligibleAmount.compareTo(defaultMaxAmount) > 0) {
            eligibleAmount = defaultMaxAmount;
        }

        return result
                .eligible(true)
                .eligibleAmount(eligibleAmount)
                .reasons(List.of("Eligible based on last purchase of " + meterInfo.getLastTokenAmount() +
                        " at " + percentage + "% = " + eligibleAmount))
                .build();
    }

    private BigDecimal getServiceFeePercentage() {
        // 1. Check loan rule first
        var fromRule = ruleRepository.findByRuleName("SERVICE_FEE_PERCENTAGE")
                .filter(LoanRuleConfiguration::getEnabled)
                .map(rule -> {
                    Map<String, Object> values = parseRuleValue(rule.getRuleValue());
                    return values.containsKey("percentage")
                            ? new BigDecimal(values.get("percentage").toString())
                            : null;
                });
        if (fromRule.isPresent() && fromRule.get() != null) {
            return fromRule.get();
        }

        // 2. Fallback to system configuration
        return systemConfigRepository.findByConfigKey("loan.service.fee.percentage")
                .map(config -> {
                    try {
                        return new BigDecimal(config.getConfigValue());
                    } catch (Exception e) {
                        log.warn("Invalid service fee config value: {}", config.getConfigValue());
                        return new BigDecimal("10");
                    }
                })
                .orElse(new BigDecimal("10"));
    }

    private Map<String, Object> parseRuleValue(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse rule value: {}", json, e);
            return Map.of();
        }
    }
}
