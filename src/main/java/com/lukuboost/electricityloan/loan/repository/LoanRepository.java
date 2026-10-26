package com.lukuboost.electricityloan.loan.repository;

import com.lukuboost.electricityloan.loan.entity.Loan;
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
public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Query("SELECT l FROM Loan l JOIN FETCH l.meter JOIN FETCH l.application WHERE l.loanRef = :loanRef")
    Optional<Loan> findByLoanRefWithMeter(@Param("loanRef") String loanRef);

    Optional<Loan> findByLoanRef(String loanRef);

    List<Loan> findByCustomerIdAndStatus(Long customerId, String status);

    long countByCustomerIdAndStatus(Long customerId, String status);

    @Query("SELECT COUNT(l) > 0 FROM Loan l WHERE l.customer.phoneNumber = :phoneNumber AND l.status = 'ACTIVE'")
    boolean existsActiveByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT COUNT(l) > 0 FROM Loan l WHERE l.meter.meterNumber = :meterNumber AND l.status = 'ACTIVE'")
    boolean existsActiveByMeterNumber(@Param("meterNumber") String meterNumber);

    @Query("SELECT l FROM Loan l JOIN FETCH l.meter LEFT JOIN FETCH l.application WHERE " +
            "(:ref IS NULL OR l.loanRef LIKE %:ref%) AND " +
            "(:status IS NULL OR l.status = :status) AND " +
            "(:customerId IS NULL OR l.customer.id = :customerId) AND " +
            "(:from IS NULL OR l.createdAt >= :from) AND " +
            "(:to IS NULL OR l.createdAt <= :to)")
    Page<Loan> searchLoans(
            @Param("ref") String ref,
            @Param("status") String status,
            @Param("customerId") Long customerId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.status = 'ACTIVE'")
    BigDecimal sumOutstandingAmount();

    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l")
    BigDecimal sumTotalIssued();

    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.status = 'COMPLETED'")
    BigDecimal sumTotalRepaid();

    long countByStatus(String status);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.createdAt >= :since")
    long countSince(@Param("since") Instant since);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.createdAt >= :from AND l.createdAt < :to")
    long countBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.createdAt >= :from AND l.createdAt < :to")
    BigDecimal sumAmountBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT l FROM Loan l JOIN FETCH l.meter JOIN FETCH l.application WHERE l.meter.meterNumber = :meterNumber ORDER BY l.createdAt DESC")
    List<Loan> findLatestByMeterNumber(@Param("meterNumber") String meterNumber, Pageable pageable);

    @Query("SELECT l FROM Loan l JOIN FETCH l.meter WHERE l.customer.id = :customerId")
    List<Loan> findByCustomerIdWithMeter(@Param("customerId") Long customerId);

    List<Loan> findByCustomerId(Long customerId);
}
