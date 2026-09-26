<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class AuditLog extends Model
{
    public $timestamps = false;

    protected $fillable = [
        'user_id', 'user_type', 'username', 'action', 'entity_type',
        'entity_id', 'old_value', 'new_value', 'description',
        'ip_address', 'user_agent', 'correlation_id', 'created_at',
    ];

    protected $casts = [
        'created_at' => 'datetime',
    ];
}
