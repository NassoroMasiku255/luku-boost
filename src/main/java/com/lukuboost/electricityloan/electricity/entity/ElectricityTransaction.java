package com.lukuboost.electricityloan.electricity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lukuboost.electricityloan.loan.entity.Loan;
import com.lukuboost.electricityloan.loan.entity.LoanApplication;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "electricity_transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectricityTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internal_ref", nullable = false, unique = true, length = 40)
    private String internalRef;

    @Column(name = "provider_ref", length = 100)
    private String providerRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    @JsonIgnore
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    @JsonIgnore
    private LoanApplication application;

    @Column(name = "meter_number", nullable = false, length = 30)
    private String meterNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "TZS";

    @Column(name = "request_status", nullable = false, length = 20)
    @Builder.Default
    private String requestStatus = "PENDING";

    @Column(name = "provider_status", length = 50)
    private String providerStatus;

    @Column(name = "token")
    private String token;

    @Column(name = "token_units", length = 50)
    private String tokenUnits;

    @Column(name = "request_timestamp")
    private Instant requestTimestamp;

    @Column(name = "response_timestamp")
    private Instant responseTimestamp;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
