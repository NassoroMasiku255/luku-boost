<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class LoanApplication extends Model
{
    use UsesUuid;

    protected $fillable = [
        'application_ref', 'customer_id', 'meter_id', 'phone_number', 'meter_number',
        'requested_amount', 'eligible_amount', 'approved_amount', 'currency', 'status',
        'eligibility_ref', 'idempotency_key', 'correlation_id', 'failure_reason',
    ];

    protected $casts = [
        'requested_amount' => 'decimal:4',
        'eligible_amount' => 'decimal:4',
        'approved_amount' => 'decimal:4',
    ];

    public function customer()
    {
        return $this->belongsTo(Customer::class);
    }

    public function meter()
    {
        return $this->belongsTo(Meter::class);
    }

    public function loan()
    {
        return $this->hasOne(Loan::class, 'application_id');
    }
}
