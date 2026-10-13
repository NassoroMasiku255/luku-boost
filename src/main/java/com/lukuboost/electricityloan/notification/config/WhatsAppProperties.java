package com.lukuboost.electricityloan.notification.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "whatsapp")
public record WhatsAppProperties(
        @NotBlank String apiUrl,
        @NotBlank String accessToken,
        @NotBlank String otpTemplateName,
        @NotBlank String otpTemplateLanguage,
        @NotBlank String loanApprovedTemplateName,
        @NotBlank String loanApprovedTemplateLanguage,
        @NotBlank String defaultCountryCode
) {}