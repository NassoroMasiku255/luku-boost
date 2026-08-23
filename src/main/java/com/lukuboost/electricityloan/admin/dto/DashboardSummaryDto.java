package com.lukuboost.electricityloan.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {

    private long totalCustomers;
    private long activeCustomers;
    private long totalLoans;
    private long loansToday;
    private long loansThisMonth;
    private BigDecimal totalAmountIssued;
    private BigDecimal totalOutstanding;
    private BigDecimal totalRepaid;
    private long failedTransactions;
    private long pendingTransactions;
    private long unknownProviderTransactions;
    private long successfulElectricityPurchases;
    private long failedElectricityPurchases;
    private long otpRequestsToday;
    private long pendingReconciliations;
}
