package com.lukuboost.electricityloan.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanResponseDto {

    private Long id;
    private String loanRef;
    private String applicationRef;
    private String status;
    private BigDecimal principalAmount;
    private BigDecimal feeAmount;
    private BigDecimal totalDue;
    private BigDecimal outstandingAmount;
    private String currency;
    private String meterNumber;
    private String token;
    private String tokenUnits;
    private Instant issuedAt;
    private Instant dueAt;
    private String message;
}
