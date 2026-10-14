package com.lukuboost.electricityloan.repayment.repository;

import com.lukuboost.electricityloan.repayment.entity.Repayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    Optional<Repayment> findByRepaymentRef(String repaymentRef);

    Optional<Repayment> findByIdempotencyKey(String idempotencyKey);

    List<Repayment> findByLoanId(Long loanId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Repayment r WHERE r.loan.id = :loanId AND r.status = 'COMPLETED'")
    BigDecimal sumCompletedByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT r FROM Repayment r WHERE " +
            "(:ref IS NULL OR r.repaymentRef LIKE %:ref%) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:from IS NULL OR r.createdAt >= :from) AND " +
            "(:to IS NULL OR r.createdAt <= :to)")
    Page<Repayment> searchRepayments(
            @Param("ref") String ref,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
