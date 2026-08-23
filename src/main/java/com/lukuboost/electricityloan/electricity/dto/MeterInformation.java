package com.lukuboost.electricityloan.electricity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterInformation {

    private String meterNumber;
    private String customerName;
    private String meterType;
    private String meterStatus;
    private String provider;
    private String lastToken;
    private BigDecimal lastTokenAmount;
    private Instant lastPurchaseDate;
    private BigDecimal averagePurchaseAmount;
    private Integer purchaseCount;
    private String tariffClass;
    private String district;
}
