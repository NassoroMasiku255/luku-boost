package com.lukuboost.electricityloan.electricity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenTransactionStatus {

    private String transactionReference;
    private String status; // SUCCESS, FAILED, PENDING, UNKNOWN
    private String providerTransactionRef;
    private String token;
    private String tokenUnits;
    private String statusMessage;
}
