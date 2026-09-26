<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class Customer extends Model
{
    use UsesUuid;

    protected $fillable = [
        'customer_ref', 'phone_number', 'first_name', 'last_name', 'status',
    ];

    protected $casts = [
        'created_at' => 'datetime',
        'updated_at' => 'datetime',
    ];

    public function meters()
    {
        return $this->hasMany(Meter::class);
    }

    public function loans()
    {
        return $this->hasMany(Loan::class);
    }
}
