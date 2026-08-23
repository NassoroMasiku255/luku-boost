package com.lukuboost.electricityloan.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRequestDto {

    @NotBlank(message = "Phone number is required")
    @Size(min = 9, max = 20, message = "Phone number must be between 9 and 20 characters")
    private String phoneNumber;

    @NotBlank(message = "Meter number is required")
    @Size(min = 5, max = 30, message = "Meter number must be between 5 and 30 characters")
    private String meterNumber;

    @NotNull(message = "Loan limit is required")
    @Positive(message = "Loan limit must be positive")
    private BigDecimal loanLimit;

    @NotNull(message = "Borrowed amount is required")
    @Positive(message = "Borrowed amount must be positive")
    private BigDecimal borrowedAmount;

    @NotBlank(message = "Application reference is required")
    private String applicationRef;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
