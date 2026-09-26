<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class FailedTransaction extends Model
{
    use UsesUuid;

    protected $fillable = [
        'original_ref', 'transaction_type', 'entity_type', 'entity_id',
        'failure_reason', 'retry_count', 'max_retries', 'last_retry_at',
        'next_retry_at', 'status', 'resolution_notes', 'resolved_by', 'resolved_at',
    ];

    protected $casts = [
        'last_retry_at' => 'datetime',
        'next_retry_at' => 'datetime',
        'resolved_at' => 'datetime',
    ];
}
