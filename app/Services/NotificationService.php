<?php

namespace App\Services;

use App\Models\Notification;
use Illuminate\Support\Facades\Log;

class NotificationService
{
    public function sendOtp(string $phoneNumber, string $otp): void
    {
        Notification::create([
            'recipient' => $phoneNumber,
            'channel' => 'SMS',
            'template' => 'OTP_VERIFICATION',
            'content' => "Your LUKU Boost verification code is: {$otp}. Valid for 5 minutes.",
            'status' => 'SENT',
            'correlation_id' => request()->header('X-Correlation-Id'),
            'sent_at' => now(),
            'created_at' => now(),
        ]);

        Log::info('OTP notification queued for delivery to phone=***');
    }

    public function sendLoanApproval(string $phoneNumber, string $loanRef, ?string $token): void
    {
        Notification::create([
            'recipient' => $phoneNumber,
            'channel' => 'SMS',
            'template' => 'LOAN_APPROVAL',
            'content' => "Your LUKU Boost loan {$loanRef} has been approved. Token: {$token}",
            'status' => 'SENT',
            'correlation_id' => request()->header('X-Correlation-Id'),
            'sent_at' => now(),
            'created_at' => now(),
        ]);

        Log::info("Loan approval notification sent for loanRef={$loanRef}");
    }

    public function sendLoanFailure(string $phoneNumber, string $reason): void
    {
        Notification::create([
            'recipient' => $phoneNumber,
            'channel' => 'SMS',
            'template' => 'LOAN_FAILURE',
            'content' => "Your LUKU Boost loan request could not be processed. {$reason}",
            'status' => 'SENT',
            'correlation_id' => request()->header('X-Correlation-Id'),
            'sent_at' => now(),
            'created_at' => now(),
        ]);

        Log::info('Loan failure notification sent');
    }

    public function sendRepaymentConfirmation(string $phoneNumber, string $loanRef, string $amount): void
    {
        Notification::create([
            'recipient' => $phoneNumber,
            'channel' => 'SMS',
            'template' => 'REPAYMENT_CONFIRMATION',
            'content' => "Payment of {$amount} received for loan {$loanRef}. Thank you!",
            'status' => 'SENT',
            'correlation_id' => request()->header('X-Correlation-Id'),
            'sent_at' => now(),
            'created_at' => now(),
        ]);

        Log::info("Repayment confirmation notification sent for loanRef={$loanRef}");
    }
}
