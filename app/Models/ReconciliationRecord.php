<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class ReconciliationRecord extends Model
{
    use UsesUuid;

    protected $fillable = [
        'recon_ref', 'internal_ref', 'provider_ref', 'internal_status',
        'provider_status', 'internal_amount', 'provider_amount', 'mismatch_type',
        'resolution_status', 'resolution_notes', 'resolved_by', 'resolved_at',
    ];

    protected $casts = [
        'internal_amount' => 'decimal:4',
        'provider_amount' => 'decimal:4',
        'resolved_at' => 'datetime',
    ];
}
