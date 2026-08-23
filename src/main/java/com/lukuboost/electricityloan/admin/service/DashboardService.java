package com.lukuboost.electricityloan.admin.service;

import com.lukuboost.electricityloan.admin.dto.DashboardSummaryDto;
import com.lukuboost.electricityloan.common.repository.FailedTransactionRepository;
import com.lukuboost.electricityloan.customer.repository.CustomerRepository;
import com.lukuboost.electricityloan.electricity.repository.ElectricityTransactionRepository;
import com.lukuboost.electricityloan.loan.repository.LoanRepository;
import com.lukuboost.electricityloan.otp.repository.OtpRequestRepository;
import com.lukuboost.electricityloan.reconciliation.repository.ReconciliationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final ElectricityTransactionRepository electricityTransactionRepository;
    private final OtpRequestRepository otpRequestRepository;
    private final FailedTransactionRepository failedTransactionRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary() {
        Instant todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        return DashboardSummaryDto.builder()
                .totalCustomers(customerRepository.count())
                .activeCustomers(customerRepository.countByStatus("ACTIVE"))
                .totalLoans(loanRepository.count())
                .loansToday(loanRepository.countSince(todayStart))
                .loansThisMonth(loanRepository.countSince(monthStart))
                .totalAmountIssued(loanRepository.sumTotalIssued())
                .totalOutstanding(loanRepository.sumOutstandingAmount())
                .totalRepaid(loanRepository.sumTotalRepaid())
                .failedTransactions(failedTransactionRepository.countByStatus("PENDING"))
                .pendingTransactions(electricityTransactionRepository.countByRequestStatus("PENDING"))
                .unknownProviderTransactions(electricityTransactionRepository.countByRequestStatus("UNKNOWN"))
                .successfulElectricityPurchases(electricityTransactionRepository.countByRequestStatus("SUCCESS"))
                .failedElectricityPurchases(electricityTransactionRepository.countByRequestStatus("FAILED"))
                .otpRequestsToday(otpRequestRepository.countSince(todayStart))
                .pendingReconciliations(reconciliationRecordRepository.countByResolutionStatus("PENDING"))
                .build();
    }
}
