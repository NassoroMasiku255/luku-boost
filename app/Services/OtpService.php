<?php

namespace App\Services;

use App\Exceptions\BusinessException;
use App\Exceptions\RateLimitExceededException;
use App\Models\OtpRequest;
use App\Models\OtpVerification;
use Illuminate\Support\Facades\Log;

class OtpService
{
    protected NotificationService $notificationService;
    protected int $otpLength;
    protected int $expirySeconds;
    protected int $maxAttempts;
    protected int $resendCooldownSeconds;
    protected int $maxRequestsPerHour;

    public function __construct(NotificationService $notificationService)
    {
        $this->notificationService = $notificationService;
        $this->otpLength = (int) config('app.otp.length', 6);
        $this->expirySeconds = (int) config('app.otp.expiry_seconds', 300);
        $this->maxAttempts = (int) config('app.otp.max_attempts', 5);
        $this->resendCooldownSeconds = (int) config('app.otp.resend_cooldown_seconds', 60);
        $this->maxRequestsPerHour = (int) config('app.otp.max_requests_per_hour', 5);
    }

    public function generateAndSendOtp(string $phoneNumber, string $applicationRef, string $ipAddress): OtpRequest
    {
        $recentCount = OtpRequest::where('phone_number', $phoneNumber)
            ->where('created_at', '>=', now()->subHour())
            ->count();

        if ($recentCount >= $this->maxRequestsPerHour) {
            throw new RateLimitExceededException('Too many OTP requests. Please try again later.');
        }

        // Supersede pending OTPs
        OtpRequest::where('phone_number', $phoneNumber)
            ->where('status', 'PENDING')
            ->update(['status' => 'SUPERSEDED']);

        $otp = $this->generateOtp();
        $salt = $this->generateSalt();
        $otpHash = $this->hashOtp($otp, $salt);

        $otpRequest = OtpRequest::create([
            'phone_number' => $phoneNumber,
            'otp_hash' => $otpHash,
            'salt' => $salt,
            'otp_plain' => $otp,
            'purpose' => 'LOAN_APPLICATION',
            'status' => 'PENDING',
            'attempts_remaining' => $this->maxAttempts,
            'application_ref' => $applicationRef,
            'ip_address' => $ipAddress,
            'expires_at' => now()->addSeconds($this->expirySeconds),
        ]);

        $this->notificationService->sendOtp($phoneNumber, $otp);

        Log::info("OTP generated for phone={$this->maskPhone($phoneNumber)}, applicationRef={$applicationRef}");

        return $otpRequest;
    }

    public function verifyOtp(string $applicationRef, string $otpValue, string $ipAddress, ?string $userAgent): bool
    {
        $otpRequest = OtpRequest::where('application_ref', $applicationRef)
            ->where('status', 'PENDING')
            ->first();

        if (!$otpRequest) {
            throw new BusinessException('OTP_NOT_FOUND', 'No pending OTP found for this application.', 404);
        }

        if ($otpRequest->expires_at->isPast()) {
            $otpRequest->update(['status' => 'EXPIRED']);
            $this->recordVerification($otpRequest, false, $ipAddress, $userAgent);
            throw new BusinessException('OTP_EXPIRED', 'OTP has expired. Please request a new one.');
        }

        if ($otpRequest->attempts_remaining <= 0) {
            $otpRequest->update(['status' => 'BLOCKED']);
            $this->recordVerification($otpRequest, false, $ipAddress, $userAgent);
            throw new BusinessException('OTP_BLOCKED', 'Maximum verification attempts exceeded.');
        }

        $hash = $this->hashOtp($otpValue, $otpRequest->salt);
        $valid = $hash === $otpRequest->otp_hash;

        if ($valid) {
            $otpRequest->update([
                'status' => 'VERIFIED',
                'verified_at' => now(),
            ]);
            $this->recordVerification($otpRequest, true, $ipAddress, $userAgent);
            Log::info("OTP verified successfully for applicationRef={$applicationRef}");
            return true;
        }

        $otpRequest->update([
            'attempts_remaining' => $otpRequest->attempts_remaining - 1,
        ]);
        $this->recordVerification($otpRequest, false, $ipAddress, $userAgent);

        Log::warning("OTP verification failed for applicationRef={$applicationRef}, remaining={$otpRequest->attempts_remaining}");
        throw new BusinessException('OTP_INVALID', "Invalid OTP. {$otpRequest->attempts_remaining} attempts remaining.");
    }

    public function getOtpForAdmin(string $applicationRef): OtpRequest
    {
        $otp = OtpRequest::where('application_ref', $applicationRef)
            ->orderBy('created_at', 'desc')
            ->first();

        if (!$otp) {
            throw new BusinessException('OTP_NOT_FOUND', "No OTP found for application: {$applicationRef}", 404);
        }

        return $otp;
    }

    public function resendOtp(string $applicationRef, string $ipAddress): OtpRequest
    {
        $existing = OtpRequest::where('application_ref', $applicationRef)
            ->orderBy('created_at', 'desc')
            ->first();

        if (!$existing) {
            throw new BusinessException('OTP_NOT_FOUND', "No OTP found for application: {$applicationRef}", 404);
        }

        return $this->generateAndSendOtp($existing->phone_number, $applicationRef, $ipAddress);
    }

    public function getExpirySeconds(): int
    {
        return $this->expirySeconds;
    }

    protected function recordVerification(OtpRequest $otpRequest, bool $success, string $ipAddress, ?string $userAgent): void
    {
        OtpVerification::create([
            'otp_request_id' => $otpRequest->id,
            'success' => $success,
            'ip_address' => $ipAddress,
            'user_agent' => $userAgent,
            'created_at' => now(),
        ]);
    }

    protected function generateOtp(): string
    {
        $bound = pow(10, $this->otpLength);
        $otp = random_int(0, $bound - 1);
        return str_pad((string)$otp, $this->otpLength, '0', STR_PAD_LEFT);
    }

    protected function generateSalt(): string
    {
        return base64_encode(random_bytes(16));
    }

    protected function hashOtp(string $otp, string $salt): string
    {
        return hash('sha256', $salt . $otp);
    }

    protected function maskPhone(?string $phone): string
    {
        if (!$phone || strlen($phone) < 4) return '***';
        return substr($phone, 0, 3) . '****' . substr($phone, -3);
    }
}
