<?php

namespace App\Http\Controllers\Api\Admin;

use App\Http\Controllers\Controller;
use App\Services\AuditService;
use App\Services\OtpService;
use Illuminate\Http\JsonResponse;

class AdminOtpController extends Controller
{
    protected OtpService $otpService;
    protected AuditService $auditService;

    public function __construct(OtpService $otpService, AuditService $auditService)
    {
        $this->otpService = $otpService;
        $this->auditService = $auditService;
    }

    public function getOtp(string $applicationRef): JsonResponse
    {
        $otpRequest = $this->otpService->getOtpForAdmin($applicationRef);

        $result = [
            'applicationRef' => $otpRequest->application_ref,
            'phoneNumber' => $otpRequest->phone_number,
            'otp' => $otpRequest->otp_plain,
            'status' => $otpRequest->status,
            'attemptsRemaining' => $otpRequest->attempts_remaining,
            'expiresAt' => $otpRequest->expires_at->toISOString(),
            'expired' => $otpRequest->expires_at->isPast(),
            'createdAt' => $otpRequest->created_at->toISOString(),
        ];

        $this->auditService->logAction('OTP_VIEWED', 'OTP', $otpRequest->id,
            null, null, "Admin viewed OTP for application: {$applicationRef}");

        return response()->json(['success' => true, 'error' => null, 'data' => $result]);
    }

    public function resendOtp(string $applicationRef): JsonResponse
    {
        $newOtp = $this->otpService->resendOtp($applicationRef, 'admin-portal');

        $result = [
            'applicationRef' => $newOtp->application_ref,
            'phoneNumber' => $newOtp->phone_number,
            'otp' => $newOtp->otp_plain,
            'status' => $newOtp->status,
            'expiresAt' => $newOtp->expires_at->toISOString(),
            'message' => 'New OTP generated and sent to customer.',
        ];

        $this->auditService->logAction('OTP_RESENT', 'OTP', $newOtp->id,
            null, null, "Admin resent OTP for application: {$applicationRef}");

        return response()->json(['success' => true, 'error' => null, 'data' => $result]);
    }
}
