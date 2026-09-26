<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class OtpRequest extends Model
{
    use UsesUuid;

    protected $fillable = [
        'phone_number', 'otp_hash', 'salt', 'otp_plain', 'purpose', 'status',
        'attempts_remaining', 'application_ref', 'ip_address', 'expires_at', 'verified_at',
    ];

    protected $casts = [
        'expires_at' => 'datetime',
        'verified_at' => 'datetime',
    ];

    public function verifications()
    {
        return $this->hasMany(OtpVerification::class);
    }
}
