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
public class ElectricityTokenPurchase {

    private boolean success;
    private String providerTransactionRef;
    private String token;
    private String tokenUnits;
    private BigDecimal amount;
    private String meterNumber;
    private String statusMessage;
    private Instant transactionTimestamp;
}
