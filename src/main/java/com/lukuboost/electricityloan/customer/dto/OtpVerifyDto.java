package com.lukuboost.electricityloan.customer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyDto {

    @NotBlank(message = "Application reference is required")
    private String applicationRef;

    @NotNull(message = "OTP is required")
    @Min(value = 1000, message = "OTP must be at least 4 digits")
    @Max(value = 99999999, message = "OTP must be at most 8 digits")
    private int otp;
}
