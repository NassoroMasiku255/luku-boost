<?php

namespace App\Services;

use App\Exceptions\BusinessException;

class LoanStateMachine
{
    protected static array $allowedTransitions = [
        'INITIATED' => ['ELIGIBILITY_CHECKED', 'FAILED', 'CANCELLED', 'EXPIRED'],
        'ELIGIBILITY_CHECKED' => ['OTP_PENDING', 'CANCELLED', 'EXPIRED'],
        'OTP_PENDING' => ['OTP_VERIFIED', 'CANCELLED', 'EXPIRED'],
        'OTP_VERIFIED' => ['PROCESSING', 'CANCELLED', 'EXPIRED'],
        'PROCESSING' => ['TOKEN_REQUESTED', 'FAILED', 'EXPIRED'],
        'TOKEN_REQUESTED' => ['TOKEN_RECEIVED', 'FAILED', 'TIMEOUT', 'UNKNOWN'],
        'TOKEN_RECEIVED' => ['COMPLETED'],
        'TIMEOUT' => ['TOKEN_RECEIVED', 'FAILED', 'UNKNOWN'],
        'UNKNOWN' => ['TOKEN_RECEIVED', 'FAILED', 'TIMEOUT'],
        'COMPLETED' => ['REVERSED'],
        'FAILED' => [],
        'CANCELLED' => [],
        'EXPIRED' => [],
        'REVERSED' => [],
    ];

    public function validateTransition(string $currentStatus, string $targetStatus): void
    {
        $allowed = self::$allowedTransitions[$currentStatus] ?? null;
        if ($allowed === null || !in_array($targetStatus, $allowed)) {
            throw new BusinessException(
                'INVALID_STATE_TRANSITION',
                "Cannot transition from {$currentStatus} to {$targetStatus}",
                400
            );
        }
    }

    public function canTransition(string $currentStatus, string $targetStatus): bool
    {
        $allowed = self::$allowedTransitions[$currentStatus] ?? [];
        return in_array($targetStatus, $allowed);
    }
}
