package com.lukuboost.electricityloan.electricity.repository;

import com.lukuboost.electricityloan.electricity.entity.ElectricityTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ElectricityTransactionRepository extends JpaRepository<ElectricityTransaction, Long> {

    Optional<ElectricityTransaction> findByInternalRef(String internalRef);

    Optional<ElectricityTransaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT et FROM ElectricityTransaction et WHERE et.application.id = :applicationId ORDER BY et.createdAt DESC")
    Optional<ElectricityTransaction> findByApplicationId(@Param("applicationId") Long applicationId);

    List<ElectricityTransaction> findByRequestStatusIn(List<String> statuses);

    @Query("SELECT et FROM ElectricityTransaction et WHERE " +
            "et.requestStatus IN ('UNKNOWN', 'SENT', 'TIMEOUT') AND " +
            "et.createdAt < :threshold")
    List<ElectricityTransaction> findUnresolvedOlderThan(@Param("threshold") Instant threshold);

    @Query("SELECT et FROM ElectricityTransaction et WHERE " +
            "(:ref IS NULL OR et.internalRef LIKE %:ref%) AND " +
            "(:providerRef IS NULL OR et.providerRef LIKE %:providerRef%) AND " +
            "(:status IS NULL OR et.requestStatus = :status) AND " +
            "(:meterNumber IS NULL OR et.meterNumber = :meterNumber) AND " +
            "(:from IS NULL OR et.createdAt >= :from) AND " +
            "(:to IS NULL OR et.createdAt <= :to)")
    Page<ElectricityTransaction> searchTransactions(
            @Param("ref") String ref,
            @Param("providerRef") String providerRef,
            @Param("status") String status,
            @Param("meterNumber") String meterNumber,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    long countByRequestStatus(String status);
}
