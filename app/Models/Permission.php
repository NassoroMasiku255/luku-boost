<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class Permission extends Model
{
    use UsesUuid;

    public $timestamps = false;

    protected $fillable = ['name', 'description', 'module', 'created_at'];

    protected $casts = [
        'created_at' => 'datetime',
    ];
}
