package com.lukuboost.electricityloan.admin.controller;

import com.lukuboost.electricityloan.common.dto.ApiResponse;
import com.lukuboost.electricityloan.common.dto.PagedResponse;
import com.lukuboost.electricityloan.customer.dto.LoanResponseDto;
import com.lukuboost.electricityloan.electricity.entity.ElectricityTransaction;
import com.lukuboost.electricityloan.electricity.repository.ElectricityTransactionRepository;
import com.lukuboost.electricityloan.loan.entity.Loan;
import com.lukuboost.electricityloan.loan.entity.LoanApplication;
import com.lukuboost.electricityloan.loan.repository.LoanApplicationRepository;
import com.lukuboost.electricityloan.loan.repository.LoanRepository;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/loans")
@RequiredArgsConstructor
@Tag(name = "Admin Loans", description = "Loan management")
public class AdminLoanController {

    private final LoanRepository loanRepository;
    private final LoanApplicationRepository applicationRepository;
    private final ElectricityTransactionRepository electricityTransactionRepository;
    private final com.lukuboost.electricityloan.loan.service.LoanService loanService;

    @GetMapping
    @Operation(summary = "Search loans")
    @PreAuthorize("hasAuthority('LOAN_VIEW')")
    public ResponseEntity<ApiResponse<PagedResponse<LoanResponseDto>>> searchLoans(
            @RequestParam(required = false) String ref,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<LoanResponseDto> page = loanRepository.searchLoans(ref, status, customerId, from, to, pageable)
                .map(this::toDto);

        PagedResponse<LoanResponseDto> paged = PagedResponse.<LoanResponseDto>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan details")
    @PreAuthorize("hasAuthority('LOAN_VIEW')")
    public ResponseEntity<ApiResponse<LoanResponseDto>> getLoan(@PathVariable Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new com.lukuboost.electricityloan.common.exception.ResourceNotFoundException("Loan", id.toString()));
        return ResponseEntity.ok(ApiResponse.success(toDto(loan)));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark loan as COMPLETED")
    @PreAuthorize("hasAuthority('LOAN_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> completeLoan(@PathVariable Long id) {
        loanService.completeLoan(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/applications")
    @Operation(summary = "Search loan applications")
    @PreAuthorize("hasAuthority('LOAN_VIEW')")
    public ResponseEntity<ApiResponse<PagedResponse<Object>>> searchApplications(
            @RequestParam(required = false) String ref,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String meterNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<LoanApplication> page = applicationRepository.searchApplications(
                ref, status, phone, meterNumber, from, to, pageable);

        var content = page.getContent().stream()
                .map(app -> java.util.Map.of(
                        "id", app.getId(),
                        "applicationRef", app.getApplicationRef(),
                        "phoneNumber", app.getPhoneNumber(),
                        "meterNumber", app.getMeterNumber(),
                        "status", app.getStatus(),
                        "eligibleAmount", app.getEligibleAmount() != null ? app.getEligibleAmount() : "",
                        "requestedAmount", app.getRequestedAmount() != null ? app.getRequestedAmount() : "",
                        "createdAt", app.getCreatedAt()
                ))
                .toList();

        PagedResponse<Object> paged = PagedResponse.<Object>builder()
                .content(java.util.List.copyOf(content))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    private LoanResponseDto toDto(Loan loan) {
        return LoanResponseDto.builder()
                .id(loan.getId())
                .loanRef(loan.getLoanRef())
                .applicationRef(loan.getApplication() != null ? loan.getApplication().getApplicationRef() : null)
                .status(loan.getStatus())
                .principalAmount(loan.getPrincipalAmount())
                .feeAmount(loan.getFeeAmount())
                .totalDue(loan.getTotalDue())
                .outstandingAmount(loan.getOutstandingAmount())
                .currency(loan.getCurrency())
                .meterNumber(loan.getMeter() != null ? loan.getMeter().getMeterNumber() : null)
                .issuedAt(loan.getIssuedAt())
                .dueAt(loan.getDueAt())
                .build();
    }
}
