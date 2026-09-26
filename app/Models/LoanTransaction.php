<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class LoanTransaction extends Model
{
    use UsesUuid;

    protected $fillable = [
        'transaction_ref', 'loan_id', 'transaction_type', 'amount',
        'currency', 'status', 'idempotency_key', 'description',
    ];

    protected $casts = [
        'amount' => 'decimal:4',
    ];

    public function loan()
    {
        return $this->belongsTo(Loan::class);
    }
}
