package com.lukuboost.electricityloan.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EligibilityResponseDto {

    private String applicationRef;
    private boolean eligible;
    private String phoneNumber;
    private String meterNumber;
    private String customerName;
    private String meterType;
    private String meterStatus;
    private String provider;
    private String district;
    private String tariffClass;
    private BigDecimal lastPurchaseAmount;
    private BigDecimal averagePurchaseAmount;
    private Integer purchaseCount;
    private BigDecimal eligibleAmount;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal serviceFeePercentage;
    private String currency;
    private String message;
}
