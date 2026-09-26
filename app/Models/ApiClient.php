<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class ApiClient extends Model
{
    use UsesUuid;

    protected $fillable = [
        'client_name', 'client_id', 'api_key_hash', 'description',
        'enabled', 'allowed_ips', 'rate_limit_per_minute', 'last_used_at', 'created_by',
    ];

    protected $casts = [
        'enabled' => 'boolean',
        'last_used_at' => 'datetime',
    ];
}
