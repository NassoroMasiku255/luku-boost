package com.lukuboost.electricityloan.admin.controller;

import com.lukuboost.electricityloan.common.dto.ApiResponse;
import com.lukuboost.electricityloan.common.dto.PagedResponse;
import com.lukuboost.electricityloan.common.entity.FailedTransaction;
import com.lukuboost.electricityloan.common.repository.FailedTransactionRepository;
import com.lukuboost.electricityloan.electricity.entity.ElectricityTransaction;
import com.lukuboost.electricityloan.electricity.repository.ElectricityTransactionRepository;
import com.lukuboost.electricityloan.loan.entity.LoanTransaction;
import com.lukuboost.electricityloan.loan.repository.LoanTransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@Tag(name = "Admin Transactions", description = "Transaction monitoring")
public class AdminTransactionController {

    private final LoanTransactionRepository loanTransactionRepository;
    private final ElectricityTransactionRepository electricityTransactionRepository;
    private final FailedTransactionRepository failedTransactionRepository;

    @GetMapping("/loan")
    @Operation(summary = "Search loan transactions")
    @PreAuthorize("hasAuthority('TRANSACTION_VIEW')")
    public ResponseEntity<ApiResponse<PagedResponse<LoanTransaction>>> searchLoanTransactions(
            @RequestParam(required = false) String ref,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<LoanTransaction> page = loanTransactionRepository.searchTransactions(ref, status, type, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(toPagedResponse(page)));
    }

    @GetMapping("/electricity")
    @Operation(summary = "Search electricity transactions")
    @PreAuthorize("hasAuthority('TRANSACTION_VIEW')")
    public ResponseEntity<ApiResponse<PagedResponse<ElectricityTransaction>>> searchElectricityTransactions(
            @RequestParam(required = false) String ref,
            @RequestParam(required = false) String providerRef,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String meterNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ElectricityTransaction> page = electricityTransactionRepository.searchTransactions(
                ref, providerRef, status, meterNumber, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(toPagedResponse(page)));
    }

    @GetMapping("/failed")
    @Operation(summary = "Search failed transactions")
    @PreAuthorize("hasAuthority('TRANSACTION_VIEW')")
    public ResponseEntity<ApiResponse<PagedResponse<FailedTransaction>>> searchFailedTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<FailedTransaction> page = failedTransactionRepository.searchFailedTransactions(status, type, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(toPagedResponse(page)));
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
