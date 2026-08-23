package com.lukuboost.electricityloan.admin.controller;

import com.lukuboost.electricityloan.audit.service.AuditService;
import com.lukuboost.electricityloan.common.dto.ApiResponse;
import com.lukuboost.electricityloan.common.dto.PagedResponse;
import com.lukuboost.electricityloan.common.exception.ResourceNotFoundException;
import com.lukuboost.electricityloan.common.security.AdminPrincipal;
import com.lukuboost.electricityloan.reconciliation.entity.ReconciliationRecord;
import com.lukuboost.electricityloan.reconciliation.repository.ReconciliationRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reconciliation")
@RequiredArgsConstructor
@Tag(name = "Admin Reconciliation", description = "Reconciliation management")
public class AdminReconciliationController {

    private final ReconciliationRecordRepository reconciliationRepository;
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Search reconciliation records")
    @PreAuthorize("hasAuthority('RECONCILIATION_VIEW')")
    public ResponseEntity<ApiResponse<PagedResponse<ReconciliationRecord>>> searchRecords(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mismatchType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ReconciliationRecord> page = reconciliationRepository.searchRecords(status, mismatchType, from, to, pageable);

        PagedResponse<ReconciliationRecord> paged = PagedResponse.<ReconciliationRecord>builder()
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

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Resolve reconciliation record")
    @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
    public ResponseEntity<ApiResponse<ReconciliationRecord>> resolve(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AdminPrincipal principal) {

        ReconciliationRecord record = reconciliationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReconciliationRecord", id.toString()));

        record.setResolutionStatus(body.getOrDefault("status", "RESOLVED"));
        record.setResolutionNotes(body.get("notes"));
        record.setResolvedBy(principal.userId());
        record.setResolvedAt(Instant.now());

        record = reconciliationRepository.save(record);

        auditService.logAction("MANUAL_RECONCILIATION", "RECONCILIATION", id.toString(),
                null, null, "Resolved reconciliation: " + record.getReconRef());

        return ResponseEntity.ok(ApiResponse.success(record));
    }
}
