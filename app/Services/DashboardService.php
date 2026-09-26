<?php

namespace App\Services;

use App\Models\Customer;
use App\Models\ElectricityTransaction;
use App\Models\FailedTransaction;
use App\Models\Loan;
use App\Models\OtpRequest;
use App\Models\ReconciliationRecord;

class DashboardService
{
    public function getSummary(): array
    {
        return [
            'totalCustomers' => Customer::count(),
            'activeLoans' => Loan::where('status', 'ACTIVE')->count(),
            'completedLoans' => Loan::where('status', 'COMPLETED')->count(),
            'totalLoans' => Loan::count(),
            'totalDisbursed' => (float) Loan::sum('principal_amount'),
            'totalOutstanding' => (float) Loan::where('status', 'ACTIVE')->sum('outstanding_amount'),
            'totalElectricityTransactions' => ElectricityTransaction::count(),
            'successfulTokenPurchases' => ElectricityTransaction::where('request_status', 'SUCCESS')->count(),
            'pendingOtps' => OtpRequest::where('status', 'PENDING')->count(),
            'failedTransactions' => FailedTransaction::where('status', 'PENDING')->count(),
            'pendingReconciliations' => ReconciliationRecord::where('resolution_status', 'PENDING')->count(),
        ];
    }
}
