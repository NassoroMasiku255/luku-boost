<?php

namespace App\Services;

use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;

class ElectricityProviderService
{
    public function getMeterInformation(string $meterNumber): array
    {
        Log::info("MOCK PROVIDER: Getting meter information for meter={$meterNumber}");

        usleep(500000 + rand(0, 1000000));

        return [
            'meterNumber' => $meterNumber,
            'customerName' => 'Mock Customer',
            'meterType' => 'PREPAID',
            'meterStatus' => 'ACTIVE',
            'provider' => 'MOCK_PROVIDER',
            'lastToken' => '1234-5678-9012-3456-7890',
            'lastTokenAmount' => 50000.0000,
            'lastPurchaseDate' => now()->subDays(5)->toISOString(),
            'averagePurchaseAmount' => 45000.0000,
            'purchaseCount' => 12,
            'tariffClass' => 'DOMESTIC',
            'district' => 'Dar es Salaam',
        ];
    }

    public function purchaseElectricity(string $meterNumber, float $amount, string $transactionReference): array
    {
        Log::info("MOCK PROVIDER: Purchasing electricity for meter={$meterNumber}, amount={$amount}, ref={$transactionReference}");

        usleep(500000 + rand(0, 1000000));

        $token = $this->generateMockToken();
        $providerRef = 'MOCK-' . strtoupper(substr(Str::uuid(), 0, 8));

        return [
            'success' => true,
            'providerTransactionRef' => $providerRef,
            'token' => $token,
            'tokenUnits' => ($amount / 100) . ' kWh',
            'amount' => $amount,
            'meterNumber' => $meterNumber,
            'statusMessage' => 'Token generated successfully',
            'transactionTimestamp' => now()->toISOString(),
        ];
    }

    public function getTransactionStatus(string $transactionReference): array
    {
        Log::info("MOCK PROVIDER: Checking transaction status for ref={$transactionReference}");

        return [
            'transactionReference' => $transactionReference,
            'status' => 'SUCCESS',
            'statusMessage' => 'Transaction completed successfully',
        ];
    }

    protected function generateMockToken(): string
    {
        $token = '';
        for ($i = 0; $i < 20; $i++) {
            if ($i > 0 && $i % 4 === 0) $token .= '-';
            $token .= rand(0, 9);
        }
        return $token;
    }
}
