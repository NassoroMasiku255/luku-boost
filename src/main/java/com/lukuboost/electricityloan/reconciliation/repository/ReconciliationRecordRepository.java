package com.lukuboost.electricityloan.reconciliation.repository;

import com.lukuboost.electricityloan.reconciliation.entity.ReconciliationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ReconciliationRecordRepository extends JpaRepository<ReconciliationRecord, Long> {

    Optional<ReconciliationRecord> findByReconRef(String reconRef);

    @Query("SELECT r FROM ReconciliationRecord r WHERE " +
            "(:status IS NULL OR r.resolutionStatus = :status) AND " +
            "(:mismatchType IS NULL OR r.mismatchType = :mismatchType) AND " +
            "(:from IS NULL OR r.createdAt >= :from) AND " +
            "(:to IS NULL OR r.createdAt <= :to)")
    Page<ReconciliationRecord> searchRecords(
            @Param("status") String status,
            @Param("mismatchType") String mismatchType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    long countByResolutionStatus(String status);
}
