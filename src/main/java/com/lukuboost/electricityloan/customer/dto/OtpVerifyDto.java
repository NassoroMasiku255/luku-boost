package com.lukuboost.electricityloan.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 8, message = "OTP must be between 4 and 8 characters")
    private int otp;
}
