<?php

namespace App\Services;

use App\Models\Loan;
use App\Models\LoanRuleConfiguration;
use App\Models\SystemConfiguration;
use Illuminate\Support\Facades\Log;

class EligibilityService
{
    public function calculateEligibility(array $meterInfo, string $customerId): array
    {
        Log::info("Calculating eligibility for meter={$meterInfo['meterNumber']}, customer={$customerId}");

        $defaultMinAmount = (float) config('app.loan.min_amount', 1000);
        $defaultMaxAmount = (float) config('app.loan.max_amount', 500000);
        $defaultPercentage = (float) config('app.loan.default_percentage', 50);
        $maxOutstandingLoans = (int) config('app.loan.max_outstanding_loans', 1);
        $currency = config('app.loan.currency', 'TZS');
        $serviceFee = $this->getServiceFeePercentage();

        $result = [
            'currency' => $currency,
            'minAmount' => $defaultMinAmount,
            'maxAmount' => $defaultMaxAmount,
            'serviceFeePercentage' => $serviceFee,
            'eligible' => false,
            'eligibleAmount' => null,
            'failureReasons' => [],
            'reasons' => [],
        ];

        // Check meter status
        if (strtoupper($meterInfo['meterStatus'] ?? '') !== 'ACTIVE') {
            $result['failureReasons'][] = 'Meter is not active. Status: ' . ($meterInfo['meterStatus'] ?? 'UNKNOWN');
            return $result;
        }

        // Check outstanding loans
        $outstandingCount = Loan::where('customer_id', $customerId)->where('status', 'ACTIVE')->count();
        if ($outstandingCount >= $maxOutstandingLoans) {
            $result['failureReasons'][] = "Customer has {$outstandingCount} outstanding loan(s). Maximum allowed: {$maxOutstandingLoans}";
            return $result;
        }

        // Check last purchase
        $lastTokenAmount = $meterInfo['lastTokenAmount'] ?? 0;
        if ($lastTokenAmount <= 0) {
            $result['failureReasons'][] = 'No previous electricity purchase found for this meter.';
            return $result;
        }

        // Apply configurable rules
        $percentage = $defaultPercentage;
        $minPurchase = 0;
        $maxDaysSincePurchase = 90;

        $rules = LoanRuleConfiguration::active()->get();
        foreach ($rules as $rule) {
            $ruleValues = json_decode($rule->rule_value, true) ?? [];
            switch ($rule->rule_name) {
                case 'DEFAULT_ELIGIBILITY_PERCENTAGE':
                    if (isset($ruleValues['percentage'])) $percentage = (float)$ruleValues['percentage'];
                    break;
                case 'MIN_LAST_PURCHASE_AMOUNT':
                    if (isset($ruleValues['minAmount'])) $minPurchase = (float)$ruleValues['minAmount'];
                    break;
                case 'MAX_DAYS_SINCE_PURCHASE':
                    if (isset($ruleValues['maxDays'])) $maxDaysSincePurchase = (int)$ruleValues['maxDays'];
                    break;
            }
        }

        if ($lastTokenAmount < $minPurchase) {
            $result['failureReasons'][] = "Last purchase amount ({$lastTokenAmount}) is below minimum required ({$minPurchase}).";
            return $result;
        }

        if (isset($meterInfo['lastPurchaseDate']) && $meterInfo['lastPurchaseDate']) {
            $daysSince = now()->diffInDays($meterInfo['lastPurchaseDate']);
            if ($daysSince > $maxDaysSincePurchase) {
                $result['failureReasons'][] = "Last purchase was {$daysSince} days ago. Maximum allowed: {$maxDaysSincePurchase}";
                return $result;
            }
        }

        $eligibleAmount = round($lastTokenAmount * $percentage / 100, 4);

        if ($eligibleAmount < $defaultMinAmount) {
            $result['failureReasons'][] = "Calculated eligible amount ({$eligibleAmount}) is below minimum loan amount ({$defaultMinAmount}).";
            return $result;
        }

        if ($eligibleAmount > $defaultMaxAmount) {
            $eligibleAmount = $defaultMaxAmount;
        }

        $result['eligible'] = true;
        $result['eligibleAmount'] = $eligibleAmount;
        $result['reasons'][] = "Eligible based on last purchase of {$lastTokenAmount} at {$percentage}% = {$eligibleAmount}";

        return $result;
    }

    protected function getServiceFeePercentage(): float
    {
        $rule = LoanRuleConfiguration::where('rule_name', 'SERVICE_FEE_PERCENTAGE')
            ->where('enabled', true)->first();

        if ($rule) {
            $values = json_decode($rule->rule_value, true) ?? [];
            if (isset($values['percentage'])) {
                return (float)$values['percentage'];
            }
        }

        $config = SystemConfiguration::where('config_key', 'loan.service.fee.percentage')->first();
        if ($config) {
            return (float)$config->config_value;
        }

        return 10.0;
    }
}
