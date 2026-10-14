package com.lukuboost.electricityloan.loan.repository;

import com.lukuboost.electricityloan.loan.entity.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByApplicationRef(String applicationRef);

    Optional<LoanApplication> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT la FROM LoanApplication la WHERE " +
            "(:ref IS NULL OR la.applicationRef LIKE %:ref%) AND " +
            "(:status IS NULL OR la.status = :status) AND " +
            "(:phone IS NULL OR la.phoneNumber = :phone) AND " +
            "(:meterNumber IS NULL OR la.meterNumber = :meterNumber) AND " +
            "(:from IS NULL OR la.createdAt >= :from) AND " +
            "(:to IS NULL OR la.createdAt <= :to)")
    Page<LoanApplication> searchApplications(
            @Param("ref") String ref,
            @Param("status") String status,
            @Param("phone") String phone,
            @Param("meterNumber") String meterNumber,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT COUNT(la) FROM LoanApplication la WHERE la.createdAt >= :since")
    long countSince(@Param("since") Instant since);
}
