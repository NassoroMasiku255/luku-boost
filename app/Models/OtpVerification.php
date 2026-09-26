<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class OtpVerification extends Model
{
    use UsesUuid;

    public $timestamps = false;

    protected $fillable = [
        'otp_request_id', 'success', 'ip_address', 'user_agent', 'created_at',
    ];

    protected $casts = [
        'success' => 'boolean',
        'created_at' => 'datetime',
    ];

    public function otpRequest()
    {
        return $this->belongsTo(OtpRequest::class);
    }
}
