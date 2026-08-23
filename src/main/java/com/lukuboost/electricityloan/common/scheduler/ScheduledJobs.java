package com.lukuboost.electricityloan.common.scheduler;

import com.lukuboost.electricityloan.common.repository.IdempotencyRecordRepository;
import com.lukuboost.electricityloan.common.utility.ReferenceGenerator;
import com.lukuboost.electricityloan.electricity.client.ElectricityProviderClient;
import com.lukuboost.electricityloan.electricity.dto.TokenTransactionStatus;
import com.lukuboost.electricityloan.electricity.entity.ElectricityTransaction;
import com.lukuboost.electricityloan.electricity.repository.ElectricityTransactionRepository;
import com.lukuboost.electricityloan.otp.repository.OtpRequestRepository;
import com.lukuboost.electricityloan.reconciliation.entity.ReconciliationRecord;
import com.lukuboost.electricityloan.reconciliation.repository.ReconciliationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobs {

    private final OtpRequestRepository otpRequestRepository;
    private final ElectricityTransactionRepository electricityTransactionRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ElectricityProviderClient providerClient;

    @Scheduled(fixedDelayString = "${app.scheduler.otp-cleanup-ms:60000}")
    @Transactional
    public void cleanupExpiredOtps() {
        int expired = otpRequestRepository.expireOldOtps(Instant.now());
        if (expired > 0) {
            log.info("Expired {} OTP requests", expired);
        }
    }

    @Scheduled(fixedDelayString = "${app.scheduler.reconciliation-ms:900000}")
    @Transactional
    public void reconcileUnknownTransactions() {
        Instant threshold = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<ElectricityTransaction> unresolved = electricityTransactionRepository.findUnresolvedOlderThan(threshold);

        if (unresolved.isEmpty()) return;

        log.info("Reconciling {} unresolved electricity transactions", unresolved.size());

        for (ElectricityTransaction txn : unresolved) {
            try {
                TokenTransactionStatus status = providerClient.getTransactionStatus(txn.getInternalRef());

                String providerStatus = status.getStatus();
                String internalStatus = txn.getRequestStatus();

                if ("SUCCESS".equals(providerStatus) && !"SUCCESS".equals(internalStatus)) {
                    txn.setRequestStatus("SUCCESS");
                    txn.setProviderRef(status.getProviderTransactionRef());
                    txn.setToken(status.getToken());
                    txn.setTokenUnits(status.getTokenUnits());
                    txn.setProviderStatus(providerStatus);
                    txn.setResponseTimestamp(Instant.now());
                    electricityTransactionRepository.save(txn);

                    createReconciliationRecord(txn, internalStatus, providerStatus, "STATUS_MISMATCH");
                    log.info("Reconciled transaction {} as SUCCESS", txn.getInternalRef());

                } else if ("FAILED".equals(providerStatus) && !"FAILED".equals(internalStatus)) {
                    txn.setRequestStatus("FAILED");
                    txn.setProviderStatus(providerStatus);
                    txn.setResponseTimestamp(Instant.now());
                    electricityTransactionRepository.save(txn);

                    createReconciliationRecord(txn, internalStatus, providerStatus, "STATUS_MISMATCH");
                    log.info("Reconciled transaction {} as FAILED", txn.getInternalRef());

                } else if ("UNKNOWN".equals(providerStatus)) {
                    txn.setRetryCount(txn.getRetryCount() + 1);
                    electricityTransactionRepository.save(txn);

                    if (txn.getRetryCount() >= 10) {
                        createReconciliationRecord(txn, internalStatus, providerStatus, "TIMEOUT_UNRESOLVED");
                        log.warn("Transaction {} remains unresolved after {} retries",
                                txn.getInternalRef(), txn.getRetryCount());
                    }
                }

            } catch (Exception e) {
                log.error("Failed to reconcile transaction {}: {}", txn.getInternalRef(), e.getMessage());
                txn.setRetryCount(txn.getRetryCount() + 1);
                electricityTransactionRepository.save(txn);
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupExpiredIdempotencyRecords() {
        int deleted = idempotencyRecordRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency records", deleted);
        }
    }

    private void createReconciliationRecord(ElectricityTransaction txn, String internalStatus,
                                             String providerStatus, String mismatchType) {
        reconciliationRecordRepository.save(ReconciliationRecord.builder()
                .reconRef(ReferenceGenerator.generateReconReference())
                .internalRef(txn.getInternalRef())
                .providerRef(txn.getProviderRef())
                .internalStatus(internalStatus)
                .providerStatus(providerStatus)
                .internalAmount(txn.getAmount())
                .mismatchType(mismatchType)
                .resolutionStatus("PENDING")
                .build());
    }
}
