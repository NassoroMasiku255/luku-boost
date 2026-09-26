<?php

namespace App\Models;

use App\Models\Concerns\UsesUuid;
use Illuminate\Database\Eloquent\Model;

class AdminUser extends Model
{
    use UsesUuid;

    protected $fillable = [
        'username', 'email', 'password_hash', 'full_name', 'status', 'last_login_at',
    ];

    protected $hidden = ['password_hash'];

    protected $casts = [
        'last_login_at' => 'datetime',
    ];

    public function roles()
    {
        return $this->belongsToMany(Role::class, 'admin_user_roles', 'user_id', 'role_id');
    }

    public function getAllPermissions(): array
    {
        return $this->roles->flatMap(function ($role) {
            return $role->permissions->pluck('name');
        })->unique()->values()->toArray();
    }

    public function getRoleNames(): array
    {
        return $this->roles->pluck('name')->toArray();
    }
}
