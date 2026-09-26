<?php

namespace App\Services;

use App\Exceptions\BusinessException;
use App\Helpers\ReferenceGenerator;
use App\Models\ElectricityTransaction;
use App\Models\Loan;
use App\Models\LoanApplication;
use App\Models\LoanTransaction;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class LoanService
{
    protected CustomerService $customerService;
    protected OtpService $otpService;
    protected EligibilityService $eligibilityService;
    protected ElectricityProviderService $providerClient;
    protected NotificationService $notificationService;
    protected AuditService $auditService;
    protected LoanStateMachine $stateMachine;

    protected float $minAmount;
    protected float $maxAmount;
    protected int $loanExpiryDays;
    protected string $currency;
    protected int $otpExpirySeconds;

    public function __construct(
        CustomerService $customerService,
        OtpService $otpService,
        EligibilityService $eligibilityService,
        ElectricityProviderService $providerClient,
        NotificationService $notificationService,
        AuditService $auditService,
        LoanStateMachine $stateMachine
    ) {
        $this->customerService = $customerService;
        $this->otpService = $otpService;
        $this->eligibilityService = $eligibilityService;
        $this->providerClient = $providerClient;
        $this->notificationService = $notificationService;
        $this->auditService = $auditService;
        $this->stateMachine = $stateMachine;

        $this->minAmount = (float) config('app.loan.min_amount', 1000);
        $this->maxAmount = (float) config('app.loan.max_amount', 500000);
        $this->loanExpiryDays = (int) config('app.loan.loan_expiry_days', 30);
        $this->currency = config('app.loan.currency', 'TZS');
        $this->otpExpirySeconds = (int) config('app.otp.expiry_seconds', 300);
    }

    // STEP 1: Eligibility Check
    public function checkEligibility(string $phoneNumber, string $meterNumber, string $ipAddress): array
    {
        Log::info("Eligibility check requested for phone={$this->maskPhone($phoneNumber)}, meter={$meterNumber}");

        if (Loan::whereHas('customer', fn($q) => $q->where('phone_number', $phoneNumber))
            ->where('status', 'ACTIVE')->exists()) {
            throw new BusinessException('PHONE_HAS_ACTIVE_LOAN',
                'This phone number already has an outstanding loan. Please repay first.', 409);
        }

        if (Loan::whereHas('meter', fn($q) => $q->where('meter_number', $meterNumber))
            ->where('status', 'ACTIVE')->exists()) {
            throw new BusinessException('METER_HAS_ACTIVE_LOAN',
                'This meter number already has an outstanding loan. Please repay first.', 409);
        }

        if (!$this->isValidPhoneNumber($phoneNumber)) {
            throw new BusinessException('INVALID_PHONE_NUMBER', 'The provided phone number is not valid.', 400);
        }

        if (!$this->isValidMeterNumber($meterNumber)) {
            throw new BusinessException('INVALID_METER_NUMBER', 'The provided meter number is not valid.', 400);
        }

        $customer = $this->customerService->findOrCreateCustomer($phoneNumber);
        if ($customer->status !== 'ACTIVE') {
            throw new BusinessException('CUSTOMER_NOT_ACTIVE', 'Customer account is not active.', 403);
        }

        $meter = $this->customerService->findOrCreateMeter($meterNumber, $customer);

        try {
            $meterInfo = $this->providerClient->getMeterInformation($meterNumber);
        } catch (\Exception $e) {
            Log::error("Failed to get meter information for meter={$meterNumber}", ['error' => $e->getMessage()]);
            throw new BusinessException('PROVIDER_ERROR',
                'Unable to retrieve meter information. Please try again later.', 503);
        }

        $meter->update([
            'last_token_amount' => $meterInfo['lastTokenAmount'] ?? null,
            'last_token_date' => $meterInfo['lastPurchaseDate'] ?? null,
            'meter_type' => $meterInfo['meterType'] ?? null,
            'provider' => $meterInfo['provider'] ?? null,
        ]);

        $eligibilityResult = $this->eligibilityService->calculateEligibility($meterInfo, $customer->id);

        $appRef = ReferenceGenerator::generateApplicationReference();
        $application = LoanApplication::create([
            'application_ref' => $appRef,
            'customer_id' => $customer->id,
            'meter_id' => $meter->id,
            'phone_number' => $phoneNumber,
            'meter_number' => $meterNumber,
            'currency' => $this->currency,
            'status' => 'INITIATED',
            'correlation_id' => request()->header('X-Correlation-Id'),
        ]);

        if ($eligibilityResult['eligible']) {
            $application->update(['eligible_amount' => $eligibilityResult['eligibleAmount'],
                'eligibility_ref' => ReferenceGenerator::generateCorrelationId()]);
            $this->transitionApplication($application, 'ELIGIBILITY_CHECKED');
        } else {
            $this->transitionApplication($application, 'FAILED');
            $application->update(['failure_reason' => implode('; ', $eligibilityResult['failureReasons'])]);
        }

        $this->auditService->logSystemEvent('ELIGIBILITY_CHECK', 'LOAN_APPLICATION',
            $application->id,
            "Eligibility check for meter {$meterNumber}: " . ($eligibilityResult['eligible'] ? 'ELIGIBLE' : 'NOT_ELIGIBLE'));

        Log::info("Eligibility check completed: ref={$appRef}, eligible=" . ($eligibilityResult['eligible'] ? 'true' : 'false'));

        return [
            'applicationRef' => $appRef,
            'eligible' => $eligibilityResult['eligible'],
            'meterInfo' => $meterInfo,
            'eligibilityResult' => $eligibilityResult,
        ];
    }

    // STEP 2: Loan Request
    public function requestLoan(
        string $phoneNumber, string $meterNumber, float $loanLimit,
        float $borrowedAmount, string $applicationRef,
        string $idempotencyKey, string $ipAddress
    ): LoanApplication {
        Log::info("Loan request for applicationRef={$applicationRef}, amount={$borrowedAmount}");

        $existingApp = LoanApplication::where('idempotency_key', $idempotencyKey)->first();
        if ($existingApp) {
            throw new BusinessException('DUPLICATE_REQUEST', 'This loan request has already been processed.', 409);
        }

        $application = $this->getApplicationByRef($applicationRef);

        if ($application->status !== 'ELIGIBILITY_CHECKED') {
            throw new BusinessException('INVALID_APPLICATION_STATE',
                "Application must be in ELIGIBILITY_CHECKED state. Current: {$application->status}");
        }

        if ($application->phone_number !== trim($phoneNumber)) {
            throw new BusinessException('PHONE_MISMATCH', 'Phone number does not match the application.', 400);
        }
        if ($application->meter_number !== trim($meterNumber)) {
            throw new BusinessException('METER_MISMATCH', 'Meter number does not match the application.', 400);
        }

        if ($borrowedAmount < $this->minAmount) {
            throw new BusinessException('AMOUNT_BELOW_MINIMUM',
                "Amount {$borrowedAmount} is below minimum {$this->minAmount}");
        }
        if ($borrowedAmount > $loanLimit) {
            throw new BusinessException('LOAN_LIMIT_EXCEEDED',
                "Requested amount {$borrowedAmount} exceeds the loan limit of {$loanLimit}");
        }
        if ($borrowedAmount > $this->maxAmount) {
            throw new BusinessException('AMOUNT_ABOVE_MAXIMUM',
                "Amount {$borrowedAmount} exceeds maximum {$this->maxAmount}");
        }

        $application->update([
            'requested_amount' => $borrowedAmount,
            'eligible_amount' => $loanLimit,
            'idempotency_key' => $idempotencyKey,
        ]);

        $tokenRef = ReferenceGenerator::generateTokenReference();
        ElectricityTransaction::create([
            'internal_ref' => $tokenRef,
            'application_id' => $application->id,
            'meter_number' => trim($meterNumber),
            'amount' => $borrowedAmount,
            'currency' => $this->currency,
            'request_status' => 'PENDING',
            'idempotency_key' => $idempotencyKey . '-token',
            'correlation_id' => request()->header('X-Correlation-Id'),
        ]);

        $this->transitionApplication($application, 'OTP_PENDING');
        $this->otpService->generateAndSendOtp(trim($phoneNumber), $applicationRef, $ipAddress);

        $this->auditService->logSystemEvent('LOAN_REQUESTED', 'LOAN_APPLICATION',
            $application->id, "Loan requested: amount={$borrowedAmount}, OTP sent");

        Log::info("Loan request saved, OTP sent for applicationRef={$applicationRef}");
        return $application;
    }

    // STEP 3: OTP Verification & Disbursement
    public function verifyOtpAndDisburse(string $applicationRef, string $otp, string $ipAddress, ?string $userAgent): Loan
    {
        Log::info("OTP verification and disbursement for applicationRef={$applicationRef}");

        $application = $this->getApplicationByRef($applicationRef);

        if ($application->status !== 'OTP_PENDING') {
            throw new BusinessException('INVALID_APPLICATION_STATE',
                "Application is not in OTP_PENDING state. Current: {$application->status}");
        }

        $this->otpService->verifyOtp($applicationRef, $otp, $ipAddress, $userAgent);
        $this->transitionApplication($application, 'OTP_VERIFIED');

        $elecTxn = ElectricityTransaction::where('application_id', $application->id)->first();
        if (!$elecTxn) {
            throw new BusinessException('TRANSACTION_NOT_FOUND',
                'No pending transaction found for this application.', 404);
        }

        $borrowedAmount = $application->requested_amount;
        $idempotencyKey = $application->idempotency_key;

        $feePercentage = 10;
        $feeAmount = round($borrowedAmount * $feePercentage / 100, 4);
        $totalDue = $borrowedAmount + $feeAmount;

        $this->transitionApplication($application, 'PROCESSING');

        $elecTxn->update([
            'request_status' => 'SENT',
            'request_timestamp' => now(),
        ]);

        $this->transitionApplication($application, 'TOKEN_REQUESTED');

        try {
            $purchase = $this->providerClient->purchaseElectricity(
                $application->meter_number, $borrowedAmount, $elecTxn->internal_ref);

            $elecTxn->update(['response_timestamp' => now()]);

            if ($purchase['success']) {
                $elecTxn->update([
                    'request_status' => 'SUCCESS',
                    'provider_ref' => $purchase['providerTransactionRef'],
                    'token' => $purchase['token'],
                    'token_units' => $purchase['tokenUnits'],
                    'provider_status' => 'SUCCESS',
                ]);

                $loanRef = ReferenceGenerator::generateLoanReference();
                $loan = Loan::create([
                    'loan_ref' => $loanRef,
                    'customer_id' => $application->customer_id,
                    'meter_id' => $application->meter_id,
                    'application_id' => $application->id,
                    'principal_amount' => $borrowedAmount,
                    'fee_amount' => $feeAmount,
                    'total_due' => $totalDue,
                    'outstanding_amount' => $totalDue,
                    'currency' => $this->currency,
                    'status' => 'ACTIVE',
                    'issued_at' => now(),
                    'due_at' => now()->addDays($this->loanExpiryDays),
                ]);

                $elecTxn->update(['loan_id' => $loan->id]);

                LoanTransaction::create([
                    'transaction_ref' => ReferenceGenerator::generateTransactionReference(),
                    'loan_id' => $loan->id,
                    'transaction_type' => 'DISBURSEMENT',
                    'amount' => $borrowedAmount,
                    'currency' => $this->currency,
                    'status' => 'COMPLETED',
                    'idempotency_key' => $idempotencyKey . '-disbursement',
                    'description' => 'Electricity token purchase',
                ]);

                $application->update(['approved_amount' => $borrowedAmount]);
                $this->transitionApplication($application, 'TOKEN_RECEIVED');
                $this->transitionApplication($application, 'COMPLETED');

                $this->notificationService->sendLoanApproval(
                    $application->phone_number, $loanRef, $purchase['token']);

                $this->auditService->logSystemEvent('LOAN_COMPLETED', 'LOAN', $loan->id,
                    "Loan completed: {$loanRef}, token delivered");

                Log::info("Loan completed: ref={$loanRef}, tokenRef={$elecTxn->internal_ref}");
                return $loan;
            } else {
                $this->handlePurchaseFailure($elecTxn, $application, null,
                    $purchase['statusMessage'] ?? 'Provider returned failure');
                throw new BusinessException('TOKEN_PURCHASE_FAILED',
                    'Electricity token purchase failed. Please try again.', 503);
            }
        } catch (BusinessException $e) {
            throw $e;
        } catch (\Exception $e) {
            Log::error("Electricity purchase failed for applicationRef={$applicationRef}", ['error' => $e->getMessage()]);

            $elecTxn->update([
                'response_timestamp' => now(),
                'request_status' => 'UNKNOWN',
                'failure_reason' => $e->getMessage(),
            ]);

            $this->transitionApplication($application, 'UNKNOWN');

            $this->auditService->logSystemEvent('LOAN_PURCHASE_UNKNOWN', 'LOAN_APPLICATION',
                $application->id, 'Electricity purchase outcome unknown: ' . $e->getMessage());

            throw new BusinessException('TOKEN_PURCHASE_UNKNOWN',
                'Token purchase status unknown. We will update you shortly.', 503);
        }
    }

    public function getApplicationByRef(string $applicationRef): LoanApplication
    {
        $app = LoanApplication::where('application_ref', $applicationRef)->first();
        if (!$app) {
            throw new BusinessException('APPLICATION_NOT_FOUND', "Application not found: {$applicationRef}", 404);
        }
        return $app;
    }

    public function getLoanByRef(string $loanRef): Loan
    {
        $loan = Loan::where('loan_ref', $loanRef)->first();
        if (!$loan) {
            throw new BusinessException('LOAN_NOT_FOUND', "Loan not found: {$loanRef}", 404);
        }
        return $loan;
    }

    public function getElectricityTransactionByLoanId(string $loanId): ?ElectricityTransaction
    {
        return ElectricityTransaction::where('loan_id', $loanId)->first();
    }

    protected function handlePurchaseFailure(
        ElectricityTransaction $elecTxn, LoanApplication $application, ?Loan $loan, string $reason
    ): void {
        $elecTxn->update(['request_status' => 'FAILED', 'failure_reason' => $reason]);

        $this->transitionApplication($application, 'FAILED');
        $application->update(['failure_reason' => $reason]);

        $this->notificationService->sendLoanFailure($application->phone_number,
            'Electricity token purchase failed. Please try again.');

        if ($loan) {
            $this->auditService->logSystemEvent('LOAN_PURCHASE_FAILED', 'LOAN', $loan->id,
                "Electricity purchase failed for loan {$loan->loan_ref}: {$reason}");
        } else {
            $this->auditService->logSystemEvent('LOAN_PURCHASE_FAILED', 'LOAN_APPLICATION',
                $application->id,
                "Electricity purchase failed for application {$application->application_ref}: {$reason}");
        }
    }

    protected function transitionApplication(LoanApplication $application, string $targetStatus): void
    {
        $this->stateMachine->validateTransition($application->status, $targetStatus);
        $application->update(['status' => $targetStatus]);
    }

    protected function isValidPhoneNumber(?string $phone): bool
    {
        if (!$phone || trim($phone) === '') return false;
        $cleaned = preg_replace('/[\s\-+]/', '', $phone);
        return (bool) preg_match('/^\d{9,15}$/', $cleaned);
    }

    protected function isValidMeterNumber(?string $meter): bool
    {
        if (!$meter || trim($meter) === '') return false;
        return (bool) preg_match('/^[A-Za-z0-9\-]{5,30}$/', trim($meter));
    }

    protected function maskPhone(?string $phone): string
    {
        if (!$phone || strlen($phone) < 4) return '***';
        return substr($phone, 0, 3) . '****' . substr($phone, -3);
    }
}
