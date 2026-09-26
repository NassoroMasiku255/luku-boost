<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Loan;
use App\Services\CustomerService;
use App\Services\LoanService;
use App\Services\OtpService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class CustomerController extends Controller
{
    protected LoanService $loanService;
    protected CustomerService $customerService;
    protected OtpService $otpService;

    public function __construct(LoanService $loanService, CustomerService $customerService, OtpService $otpService)
    {
        $this->loanService = $loanService;
        $this->customerService = $customerService;
        $this->otpService = $otpService;
    }

    // STEP 1: Eligibility Check
    public function checkEligibility(Request $request): JsonResponse
    {
        $request->validate([
            'phoneNumber' => 'required|string',
            'meterNumber' => 'required|string',
        ]);

        $ip = $this->getClientIp($request);

        $result = $this->loanService->checkEligibility(
            trim($request->input('phoneNumber')),
            trim($request->input('meterNumber')),
            $ip
        );

        $meterInfo = $result['meterInfo'];
        $eligibility = $result['eligibilityResult'];

        $response = [
            'applicationRef' => $result['applicationRef'],
            'eligible' => $result['eligible'],
            'phoneNumber' => $this->maskPhone($request->input('phoneNumber')),
            'meterNumber' => trim($request->input('meterNumber')),
            'customerName' => $meterInfo['customerName'] ?? null,
            'meterType' => $meterInfo['meterType'] ?? null,
            'meterStatus' => $meterInfo['meterStatus'] ?? null,
            'provider' => $meterInfo['provider'] ?? null,
            'district' => $meterInfo['district'] ?? null,
            'tariffClass' => $meterInfo['tariffClass'] ?? null,
            'lastPurchaseAmount' => $meterInfo['lastTokenAmount'] ?? null,
            'averagePurchaseAmount' => $meterInfo['averagePurchaseAmount'] ?? null,
            'purchaseCount' => $meterInfo['purchaseCount'] ?? null,
            'eligibleAmount' => $eligibility['eligibleAmount'] ?? null,
            'minAmount' => $eligibility['minAmount'] ?? null,
            'maxAmount' => $eligibility['maxAmount'] ?? null,
            'serviceFeePercentage' => $eligibility['serviceFeePercentage'] ?? null,
            'currency' => $eligibility['currency'] ?? null,
            'message' => $result['eligible']
                ? 'You are eligible for a loan.'
                : implode('; ', $eligibility['failureReasons'] ?? []),
        ];

        return response()->json(['success' => true, 'error' => null, 'data' => $response]);
    }

    // STEP 2: Loan Request
    public function requestLoan(Request $request): JsonResponse
    {
        $request->validate([
            'phoneNumber' => 'required|string',
            'meterNumber' => 'required|string',
            'loanLimit' => 'required|numeric',
            'borrowedAmount' => 'required|numeric',
            'applicationRef' => 'required|string',
            'idempotencyKey' => 'required|string',
        ]);

        $ip = $this->getClientIp($request);

        $app = $this->loanService->requestLoan(
            trim($request->input('phoneNumber')),
            trim($request->input('meterNumber')),
            (float) $request->input('loanLimit'),
            (float) $request->input('borrowedAmount'),
            $request->input('applicationRef'),
            $request->input('idempotencyKey'),
            $ip
        );

        $response = [
            'applicationRef' => $app->application_ref,
            'maskedPhone' => $this->maskPhone($request->input('phoneNumber')),
            'expiresInSeconds' => 300,
            'message' => 'OTP sent to your phone number. Please verify to complete loan disbursement.',
        ];

        return response()->json(['success' => true, 'error' => null, 'data' => $response], 201);
    }

    // STEP 3: OTP Verification & Disbursement
    public function verifyOtpAndDisburse(Request $request): JsonResponse
    {
        $request->validate([
            'applicationRef' => 'required|string',
            'otp' => 'required|string',
        ]);

        $ip = $this->getClientIp($request);
        $userAgent = $request->userAgent();

        $loan = $this->loanService->verifyOtpAndDisburse(
            $request->input('applicationRef'),
            $request->input('otp'),
            $ip,
            $userAgent
        );

        $elecTxn = $this->loanService->getElectricityTransactionByLoanId($loan->id);

        $response = [
            'loanRef' => $loan->loan_ref,
            'applicationRef' => $request->input('applicationRef'),
            'status' => $loan->status,
            'principalAmount' => (float) $loan->principal_amount,
            'feeAmount' => (float) $loan->fee_amount,
            'totalDue' => (float) $loan->total_due,
            'outstandingAmount' => (float) $loan->outstanding_amount,
            'currency' => $loan->currency,
            'meterNumber' => $loan->meter->meter_number,
            'token' => $elecTxn?->token,
            'tokenUnits' => $elecTxn?->token_units,
            'issuedAt' => $loan->issued_at?->toISOString(),
            'dueAt' => $loan->due_at?->toISOString(),
            'message' => 'Loan approved. Electricity token delivered.',
        ];

        return response()->json(['success' => true, 'error' => null, 'data' => $response], 201);
    }

    // Resend OTP
    public function resendOtp(Request $request): JsonResponse
    {
        $request->validate(['applicationRef' => 'required|string']);

        $ip = $this->getClientIp($request);
        $newOtp = $this->otpService->resendOtp($request->input('applicationRef'), $ip);

        $response = [
            'applicationRef' => $request->input('applicationRef'),
            'maskedPhone' => $this->maskPhone($newOtp->phone_number),
            'expiresInSeconds' => $this->otpService->getExpirySeconds(),
            'message' => 'New OTP sent to your phone number.',
        ];

        return response()->json(['success' => true, 'error' => null, 'data' => $response]);
    }

    // Get loan details
    public function getLoan(string $reference): JsonResponse
    {
        $loan = $this->loanService->getLoanByRef($reference);
        $loan->load(['meter', 'application']);
        $elecTxn = $this->loanService->getElectricityTransactionByLoanId($loan->id);

        $response = [
            'loanRef' => $loan->loan_ref,
            'applicationRef' => $loan->application->application_ref,
            'status' => $loan->status,
            'principalAmount' => (float) $loan->principal_amount,
            'feeAmount' => (float) $loan->fee_amount,
            'totalDue' => (float) $loan->total_due,
            'outstandingAmount' => (float) $loan->outstanding_amount,
            'currency' => $loan->currency,
            'meterNumber' => $loan->meter->meter_number,
            'token' => $elecTxn?->token,
            'tokenUnits' => $elecTxn?->token_units,
            'issuedAt' => $loan->issued_at?->toISOString(),
            'dueAt' => $loan->due_at?->toISOString(),
        ];

        return response()->json(['success' => true, 'error' => null, 'data' => $response]);
    }

    // Get loan history
    public function getLoanHistory(Request $request): JsonResponse
    {
        $request->validate(['phoneNumber' => 'required|string']);

        $customer = $this->customerService->getByPhone($request->input('phoneNumber'));
        $loans = Loan::where('customer_id', $customer->id)->with('meter')->get();

        $response = $loans->map(fn($loan) => [
            'loanRef' => $loan->loan_ref,
            'status' => $loan->status,
            'principalAmount' => (float) $loan->principal_amount,
            'feeAmount' => (float) $loan->fee_amount,
            'totalDue' => (float) $loan->total_due,
            'outstandingAmount' => (float) $loan->outstanding_amount,
            'currency' => $loan->currency,
            'meterNumber' => $loan->meter->meter_number,
            'issuedAt' => $loan->issued_at?->toISOString(),
            'dueAt' => $loan->due_at?->toISOString(),
        ]);

        return response()->json(['success' => true, 'error' => null, 'data' => $response]);
    }

    protected function getClientIp(Request $request): string
    {
        $xff = $request->header('X-Forwarded-For');
        if ($xff) {
            return trim(explode(',', $xff)[0]);
        }
        return $request->ip();
    }

    protected function maskPhone(?string $phone): string
    {
        if (!$phone || strlen($phone) < 4) return '***';
        return substr($phone, 0, 3) . '****' . substr($phone, -3);
    }
}
