package com.lukuboost.electricityloan.eligibility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanEligibilityResult {

    private boolean eligible;
    private BigDecimal eligibleAmount;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String currency;
    private BigDecimal serviceFeePercentage;

    @Builder.Default
    private List<String> reasons = new ArrayList<>();
    @Builder.Default
    private List<String> failureReasons = new ArrayList<>();
}
