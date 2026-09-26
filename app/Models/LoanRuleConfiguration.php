<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class LoanRuleConfiguration extends Model
{
    use UsesUuid;

    protected $fillable = [
        'rule_name', 'rule_type', 'rule_value', 'priority', 'enabled',
        'description', 'effective_from', 'effective_to', 'updated_by',
    ];

    protected $casts = [
        'enabled' => 'boolean',
        'effective_from' => 'datetime',
        'effective_to' => 'datetime',
    ];

    public function scopeActive($query)
    {
        return $query->where('enabled', true)
            ->where(function ($q) {
                $q->whereNull('effective_from')->orWhere('effective_from', '<=', now());
            })
            ->where(function ($q) {
                $q->whereNull('effective_to')->orWhere('effective_to', '>=', now());
            })
            ->orderBy('priority', 'desc');
    }
}
