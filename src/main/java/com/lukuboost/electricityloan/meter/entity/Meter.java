package com.lukuboost.electricityloan.meter.entity;

import com.lukuboost.electricityloan.common.entity.BaseEntity;
import com.lukuboost.electricityloan.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "meters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meter extends BaseEntity {

    @Column(name = "meter_number", nullable = false, unique = true, length = 30)
    private String meterNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "meter_type", length = 30)
    private String meterType;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "last_token_amount", precision = 19, scale = 4)
    private BigDecimal lastTokenAmount;

    @Column(name = "last_token_date")
    private Instant lastTokenDate;
}
