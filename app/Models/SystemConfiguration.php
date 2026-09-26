<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class SystemConfiguration extends Model
{
    use UsesUuid;

    protected $fillable = [
        'config_key', 'config_value', 'category', 'description',
        'value_type', 'is_sensitive', 'updated_by',
    ];

    protected $casts = [
        'is_sensitive' => 'boolean',
    ];
}
