<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class ElectricityTransaction extends Model
{
    use UsesUuid;

    protected $fillable = [
        'internal_ref', 'loan_id', 'application_id', 'meter_number', 'amount',
        'currency', 'request_status', 'request_timestamp', 'response_timestamp',
        'provider_ref', 'provider_status', 'token', 'token_units',
        'failure_reason', 'idempotency_key', 'correlation_id',
    ];

    protected $casts = [
        'amount' => 'decimal:4',
        'request_timestamp' => 'datetime',
        'response_timestamp' => 'datetime',
    ];

    public function loan()
    {
        return $this->belongsTo(Loan::class);
    }

    public function application()
    {
        return $this->belongsTo(LoanApplication::class, 'application_id');
    }
}
