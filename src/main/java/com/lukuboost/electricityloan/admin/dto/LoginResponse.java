package com.lukuboost.electricityloan.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String username;
    private String fullName;
    private Set<String> roles;
    private Set<String> permissions;
    private long expiresIn;
}
