<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class Notification extends Model
{
    use UsesUuid;

    public $timestamps = false;

    protected $fillable = [
        'recipient', 'channel', 'template', 'content', 'status',
        'correlation_id', 'sent_at', 'created_at',
    ];

    protected $casts = [
        'sent_at' => 'datetime',
        'created_at' => 'datetime',
    ];
}
