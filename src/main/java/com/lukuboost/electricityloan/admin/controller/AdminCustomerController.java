package com.lukuboost.electricityloan.admin.controller;

import com.lukuboost.electricityloan.common.dto.ApiResponse;
import com.lukuboost.electricityloan.common.dto.PagedResponse;
import com.lukuboost.electricityloan.customer.entity.Customer;
import com.lukuboost.electricityloan.customer.service.CustomerService;
import com.lukuboost.electricityloan.loan.repository.LoanRepository;
import com.lukuboost.electricityloan.meter.repository.MeterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@Tag(name = "Admin Customers", description = "Customer management")
public class AdminCustomerController {

    private final CustomerService customerService;
    private final MeterRepository meterRepository;
    private final LoanRepository loanRepository;

    @GetMapping
    @Operation(summary = "Search customers")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<ApiResponse<PagedResponse<Customer>>> searchCustomers(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Customer> page = customerService.searchCustomers(phone, status, name, pageable);
        PagedResponse<Customer> paged = PagedResponse.<Customer>builder()
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
    @Operation(summary = "Get customer details")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomer(@PathVariable UUID id) {
        Customer customer = customerService.getById(id);
        var meters = meterRepository.findByCustomerId(id);
        var loans = loanRepository.findByCustomerId(id);

        Map<String, Object> detail = Map.of(
                "customer", customer,
                "meters", meters,
                "loans", loans
        );
        return ResponseEntity.ok(ApiResponse.success(detail));
    }
}
