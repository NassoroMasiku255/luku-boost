<?php

namespace App\Helpers;

use Illuminate\Support\Str;

class ReferenceGenerator
{
    private static int $sequence = 0;

    private static function nextSequence(): string
    {
        if (self::$sequence === 0) {
            self::$sequence = (int)(microtime(true) * 1000) % 1000000;
        }
        self::$sequence = (self::$sequence + 1) % 1000000000;
        return str_pad((string)self::$sequence, 9, '0', STR_PAD_LEFT);
    }

    public static function generateLoanReference(): string
    {
        return 'LOAN-' . date('Ymd') . '-' . self::nextSequence();
    }

    public static function generateApplicationReference(): string
    {
        return 'APP-' . date('Ymd') . '-' . self::nextSequence();
    }

    public static function generateTokenReference(): string
    {
        return 'TOKEN-' . date('Ymd') . '-' . self::nextSequence();
    }

    public static function generateRepaymentReference(): string
    {
        return 'PAY-' . date('Ymd') . '-' . self::nextSequence();
    }

    public static function generateTransactionReference(): string
    {
        return 'TXN-' . date('Ymd') . '-' . self::nextSequence();
    }

    public static function generateReconReference(): string
    {
        return 'RECON-' . date('Ymd') . '-' . self::nextSequence();
    }

    public static function generateCustomerReference(): string
    {
        return 'CUST-' . date('Ymd') . '-' . self::nextSequence();
    }

    public static function generateErrorReference(): string
    {
        return 'ERR-' . date('Ymd') . '-' . self::nextSequence();
    }

    public static function generateCorrelationId(): string
    {
        return str_replace('-', '', (string) Str::uuid());
    }
}
