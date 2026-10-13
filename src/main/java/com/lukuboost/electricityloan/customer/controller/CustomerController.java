package com.lukuboost.electricityloan.customer.controller;

import com.lukuboost.electricityloan.common.dto.ApiResponse;
import com.lukuboost.electricityloan.customer.dto.*;
import com.lukuboost.electricityloan.electricity.dto.MeterInformation;
import com.lukuboost.electricityloan.electricity.entity.ElectricityTransaction;
import com.lukuboost.electricityloan.eligibility.dto.LoanEligibilityResult;
import com.lukuboost.electricityloan.loan.entity.Loan;
import com.lukuboost.electricityloan.loan.entity.LoanApplication;
import com.lukuboost.electricityloan.customer.service.CustomerService;
import com.lukuboost.electricityloan.loan.repository.LoanRepository;
import com.lukuboost.electricityloan.loan.service.LoanService;
import com.lukuboost.electricityloan.otp.entity.OtpRequest;
import com.lukuboost.electricityloan.otp.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Customer API", description = "Customer-facing loan application endpoints")
public class CustomerController {

    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final CustomerService customerService;
    private final OtpService otpService;

    // ========================================================================
    // STEP 1: Eligibility Check
    // Customer provides phone number and meter number.
    // System checks for existing loans, verifies both, calls third party for
    // meter info, and returns meter details + available loan limit.
    // ========================================================================
    @PostMapping("/eligibility/check")
    @Operation(
            summary = "Step 1: Check loan eligibility",
            description = "Validates phone and meter numbers, checks for existing active loans, " +
                    "verifies meter with third-party provider, and returns meter information with available loan limit. " +
                    "This is the first step in the 3-step loan application flow."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Eligibility check completed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid phone or meter number format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Phone or meter already has an active loan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Third-party provider unavailable")
    })
    public ResponseEntity<ApiResponse<EligibilityResponseDto>> checkEligibility(
            @Valid @RequestBody EligibilityCheckDto request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);

        LoanService.EligibilityCheckResult result = loanService.checkEligibility(
                request.getPhoneNumber().trim(),
                request.getMeterNumber().trim(),
                ip);

        MeterInformation meterInfo = result.getMeterInfo();
        LoanEligibilityResult eligibility = result.getEligibilityResult();

        EligibilityResponseDto response = EligibilityResponseDto.builder()
                .applicationRef(result.getApplicationRef())
                .eligible(result.isEligible())
                .phoneNumber(maskPhone(request.getPhoneNumber()))
                .meterNumber(request.getMeterNumber().trim())
                .customerName(meterInfo.getCustomerName())
                .meterType(meterInfo.getMeterType())
                .meterStatus(meterInfo.getMeterStatus())
                .provider(meterInfo.getProvider())
                .district(meterInfo.getDistrict())
                .tariffClass(meterInfo.getTariffClass())
                .lastPurchaseAmount(meterInfo.getLastTokenAmount())
                .averagePurchaseAmount(meterInfo.getAveragePurchaseAmount())
                .purchaseCount(meterInfo.getPurchaseCount())
                .eligibleAmount(eligibility.getEligibleAmount())
                .minAmount(eligibility.getMinAmount())
                .maxAmount(eligibility.getMaxAmount())
                .serviceFeePercentage(eligibility.getServiceFeePercentage())
                .currency(eligibility.getCurrency())
                .message(result.isEligible()
                        ? "You are eligible for a loan."
                        : String.join("; ", eligibility.getFailureReasons()))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========================================================================
    // STEP 2: Loan Request
    // Customer submits loan request with phone, meter, limit, borrowed amount,
    // applicationRef, and idempotency key. System saves transaction and sends OTP.
    // ========================================================================
    @PostMapping("/loans")
    @Operation(
            summary = "Step 2: Request loan and receive OTP",
            description = "Submits the loan request with borrowed amount and saves the transaction. " +
                    "An OTP is sent to the customer's phone number for verification. " +
                    "Requires a valid applicationRef from Step 1 (eligibility check)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Loan request saved, OTP sent to customer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters or amount out of range"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate request (idempotency key already used)")
    })
    public ResponseEntity<ApiResponse<OtpResponseDto>> requestLoan(
            @Valid @RequestBody LoanRequestDto request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);

        LoanApplication app = loanService.requestLoan(
                request.getPhoneNumber().trim(),
                request.getMeterNumber().trim(),
                request.getLoanLimit(),
                request.getBorrowedAmount(),
                request.getApplicationRef(),
                request.getIdempotencyKey(),
                ip);

        OtpResponseDto response = OtpResponseDto.builder()
                .applicationRef(app.getApplicationRef())
                .maskedPhone(maskPhone(request.getPhoneNumber()))
                .expiresInSeconds(300)
                .message("OTP sent to your phone number. Please verify to complete loan disbursement.")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ========================================================================
    // STEP 3: OTP Verification & Disbursement
    // Customer submits OTP and applicationRef. System verifies OTP, sends
    // purchase request to third party, updates transaction, and saves loan.
    // ========================================================================
    @PostMapping("/loans/verify-otp")
    @Operation(
            summary = "Step 3: Verify OTP and disburse loan",
            description = "Verifies the OTP submitted by the customer. On success, purchases the electricity token " +
                    "from the third-party provider, creates the loan record, and delivers the token. " +
                    "This is the final step in the loan application flow."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "OTP verified, loan created, electricity token delivered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Application or pending transaction not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Token purchase failed or status unknown")
    })
    public ResponseEntity<ApiResponse<LoanResponseDto>> verifyOtpAndDisburse(
            @Valid @RequestBody OtpVerifyDto request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        Loan loan = loanService.verifyOtpAndDisburse(
                request.getApplicationRef(),
                request.getOtp(),
                ip,
                userAgent);

        ElectricityTransaction elecTxn = loanService.getElectricityTransactionByLoanId(loan.getId());

        LoanResponseDto response = LoanResponseDto.builder()
                .loanRef(loan.getLoanRef())
                .applicationRef(request.getApplicationRef())
                .status(loan.getStatus())
                .principalAmount(loan.getPrincipalAmount())
                .feeAmount(loan.getFeeAmount())
                .totalDue(loan.getTotalDue())
                .outstandingAmount(loan.getOutstandingAmount())
                .currency(loan.getCurrency())
                .meterNumber(loan.getMeter().getMeterNumber())
                .token(elecTxn != null ? elecTxn.getToken() : null)
                .tokenUnits(elecTxn != null ? elecTxn.getTokenUnits() : null)
                .issuedAt(loan.getIssuedAt())
                .dueAt(loan.getDueAt())
                .message("Loan approved. Electricity token delivered.")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ========================================================================
    // Resend OTP
    // Customer can request a new OTP if the previous one expired or was not received.
    // ========================================================================
    @PostMapping("/loans/resend-otp")
    @Operation(
            summary = "Resend OTP",
            description = "Generates and sends a new OTP to the customer's phone number. " +
                    "The previous OTP is invalidated. Use this when the OTP expired or was not received."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New OTP sent to customer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Application reference not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many OTP requests")
    })
    public ResponseEntity<ApiResponse<OtpResponseDto>> resendOtp(
            @RequestParam String applicationRef,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        OtpRequest newOtp = otpService.resendOtp(applicationRef, ip);

        OtpResponseDto response = OtpResponseDto.builder()
                .applicationRef(applicationRef)
                .maskedPhone(maskPhone(newOtp.getPhoneNumber()))
                .expiresInSeconds(otpService.getExpirySeconds())
                .message("New OTP sent to your phone number.")
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========================================================================
    // Query endpoints
    // ========================================================================

    @GetMapping("/loans/{reference}")
    @Operation(
            summary = "Get loan details by reference",
            description = "Retrieves the full loan details including token information by loan reference."
    )
    public ResponseEntity<ApiResponse<LoanResponseDto>> getLoan(@PathVariable String reference) {
        Loan loan = loanService.getLoanByRef(reference);
        ElectricityTransaction elecTxn = loanService.getElectricityTransactionByLoanId(loan.getId());

        LoanResponseDto response = LoanResponseDto.builder()
                .loanRef(loan.getLoanRef())
                .applicationRef(loan.getApplication().getApplicationRef())
                .status(loan.getStatus())
                .principalAmount(loan.getPrincipalAmount())
                .feeAmount(loan.getFeeAmount())
                .totalDue(loan.getTotalDue())
                .outstandingAmount(loan.getOutstandingAmount())
                .currency(loan.getCurrency())
                .meterNumber(loan.getMeter().getMeterNumber())
                .token(elecTxn != null ? elecTxn.getToken() : null)
                .tokenUnits(elecTxn != null ? elecTxn.getTokenUnits() : null)
                .issuedAt(loan.getIssuedAt())
                .dueAt(loan.getDueAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/loans/history")
    @Operation(
            summary = "Get loan history",
            description = "Retrieves all loans for a customer by their phone number."
    )
    public ResponseEntity<ApiResponse<List<LoanResponseDto>>> getLoanHistory(
            @RequestParam String phoneNumber) {

        var customer = customerService.getByPhone(phoneNumber);
        List<Loan> loans = loanRepository.findByCustomerIdWithMeter(customer.getId());

        List<LoanResponseDto> response = loans.stream()
                .map(loan -> LoanResponseDto.builder()
                        .loanRef(loan.getLoanRef())
                        .status(loan.getStatus())
                        .principalAmount(loan.getPrincipalAmount())
                        .feeAmount(loan.getFeeAmount())
                        .totalDue(loan.getTotalDue())
                        .outstandingAmount(loan.getOutstandingAmount())
                        .currency(loan.getCurrency())
                        .meterNumber(loan.getMeter().getMeterNumber())
                        .issuedAt(loan.getIssuedAt())
                        .dueAt(loan.getDueAt())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}
