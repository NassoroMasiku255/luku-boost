<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class Loan extends Model
{
    use UsesUuid;

    protected $fillable = [
        'loan_ref', 'customer_id', 'meter_id', 'application_id',
        'principal_amount', 'fee_amount', 'total_due', 'outstanding_amount',
        'currency', 'status', 'issued_at', 'due_at',
    ];

    protected $casts = [
        'principal_amount' => 'decimal:4',
        'fee_amount' => 'decimal:4',
        'total_due' => 'decimal:4',
        'outstanding_amount' => 'decimal:4',
        'issued_at' => 'datetime',
        'due_at' => 'datetime',
    ];

    public function customer()
    {
        return $this->belongsTo(Customer::class);
    }

    public function meter()
    {
        return $this->belongsTo(Meter::class);
    }

    public function application()
    {
        return $this->belongsTo(LoanApplication::class, 'application_id');
    }

    public function transactions()
    {
        return $this->hasMany(LoanTransaction::class);
    }

    public function electricityTransactions()
    {
        return $this->hasMany(ElectricityTransaction::class);
    }
}
