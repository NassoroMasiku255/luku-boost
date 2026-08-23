package com.lukuboost.electricityloan.loan.entity;

import com.lukuboost.electricityloan.common.entity.BaseEntity;
import com.lukuboost.electricityloan.customer.entity.Customer;
import com.lukuboost.electricityloan.meter.entity.Meter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication extends BaseEntity {

    @Column(name = "application_ref", nullable = false, unique = true, length = 40)
    private String applicationRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "meter_number", nullable = false, length = 30)
    private String meterNumber;

    @Column(name = "requested_amount", precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    @Column(name = "eligible_amount", precision = 19, scale = 4)
    private BigDecimal eligibleAmount;

    @Column(name = "approved_amount", precision = 19, scale = 4)
    private BigDecimal approvedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "TZS";

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "INITIATED";

    @Column(name = "eligibility_ref", length = 40)
    private String eligibilityRef;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "failure_reason")
    private String failureReason;
}
