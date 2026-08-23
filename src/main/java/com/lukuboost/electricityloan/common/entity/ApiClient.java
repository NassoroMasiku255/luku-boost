package com.lukuboost.electricityloan.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiClient extends BaseEntity {

    @Column(name = "client_name", nullable = false, length = 100)
    private String clientName;

    @Column(name = "client_id", nullable = false, unique = true, length = 64)
    private String clientId;

    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "allowed_ips")
    private String allowedIps;

    @Column(name = "rate_limit_per_minute")
    @Builder.Default
    private Integer rateLimitPerMinute = 60;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_by")
    private UUID createdBy;
}
