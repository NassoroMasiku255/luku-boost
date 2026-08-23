package com.lukuboost.electricityloan.loan.repository;

import com.lukuboost.electricityloan.loan.entity.LoanTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanTransactionRepository extends JpaRepository<LoanTransaction, UUID> {

    Optional<LoanTransaction> findByTransactionRef(String transactionRef);

    List<LoanTransaction> findByLoanId(UUID loanId);

    @Query("SELECT lt FROM LoanTransaction lt WHERE " +
            "(:ref IS NULL OR lt.transactionRef LIKE %:ref%) AND " +
            "(:status IS NULL OR lt.status = :status) AND " +
            "(:type IS NULL OR lt.transactionType = :type) AND " +
            "(:from IS NULL OR lt.createdAt >= :from) AND " +
            "(:to IS NULL OR lt.createdAt <= :to)")
    Page<LoanTransaction> searchTransactions(
            @Param("ref") String ref,
            @Param("status") String status,
            @Param("type") String type,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
