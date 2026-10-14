package com.lukuboost.electricityloan.reconciliation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reconciliation_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recon_ref", nullable = false, unique = true, length = 40)
    private String reconRef;

    @Column(name = "internal_ref", nullable = false, length = 40)
    private String internalRef;

    @Column(name = "provider_ref", length = 100)
    private String providerRef;

    @Column(name = "internal_status", nullable = false, length = 20)
    private String internalStatus;

    @Column(name = "provider_status", length = 50)
    private String providerStatus;

    @Column(name = "internal_amount", precision = 19, scale = 4)
    private BigDecimal internalAmount;

    @Column(name = "provider_amount", precision = 19, scale = 4)
    private BigDecimal providerAmount;

    @Column(name = "mismatch_type", nullable = false, length = 50)
    private String mismatchType;

    @Column(name = "resolution_status", nullable = false, length = 20)
    @Builder.Default
    private String resolutionStatus = "PENDING";

    @Column(name = "resolution_notes")
    private String resolutionNotes;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
