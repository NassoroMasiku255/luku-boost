package com.lukuboost.electricityloan.common.repository;

import com.lukuboost.electricityloan.common.entity.FailedTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface FailedTransactionRepository extends JpaRepository<FailedTransaction, Long> {

    @Query("SELECT f FROM FailedTransaction f WHERE f.status = 'PENDING' " +
            "AND f.retryCount < f.maxRetries " +
            "AND (f.nextRetryAt IS NULL OR f.nextRetryAt <= :now)")
    List<FailedTransaction> findRetryable(@Param("now") Instant now);

    @Query("SELECT f FROM FailedTransaction f WHERE " +
            "(:status IS NULL OR f.status = :status) AND " +
            "(:type IS NULL OR f.transactionType = :type) AND " +
            "(:from IS NULL OR f.createdAt >= :from) AND " +
            "(:to IS NULL OR f.createdAt <= :to)")
    Page<FailedTransaction> searchFailedTransactions(
            @Param("status") String status,
            @Param("type") String type,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    long countByStatus(String status);
}
