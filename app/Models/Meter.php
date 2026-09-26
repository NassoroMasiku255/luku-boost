<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class Meter extends Model
{
    use UsesUuid;

    protected $fillable = [
        'meter_number', 'customer_id', 'meter_type', 'provider', 'status',
        'last_token_amount', 'last_token_date',
    ];

    protected $casts = [
        'last_token_amount' => 'decimal:4',
        'last_token_date' => 'datetime',
    ];

    public function customer()
    {
        return $this->belongsTo(Customer::class);
    }
}
