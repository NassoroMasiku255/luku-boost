package com.lukuboost.electricityloan.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String status;
    private Set<String> roles;
    private Instant lastLoginAt;
    private Instant createdAt;
}
