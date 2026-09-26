<?php

namespace App\Services;

use App\Exceptions\BusinessException;
use App\Models\AdminUser;
use App\Models\Role;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Log;

class AdminUserService
{
    protected AuditService $auditService;

    public function __construct(AuditService $auditService)
    {
        $this->auditService = $auditService;
    }

    public function createUser(array $data): array
    {
        if (AdminUser::where('username', $data['username'])->exists()) {
            throw new BusinessException('USERNAME_EXISTS', 'Username already exists.', 409);
        }
        if (AdminUser::where('email', $data['email'])->exists()) {
            throw new BusinessException('EMAIL_EXISTS', 'Email already exists.', 409);
        }

        $user = AdminUser::create([
            'username' => $data['username'],
            'email' => $data['email'],
            'password_hash' => Hash::make($data['password']),
            'full_name' => $data['fullName'],
            'status' => 'ACTIVE',
        ]);

        if (!empty($data['roleIds'])) {
            $roles = Role::whereIn('id', $data['roleIds'])->get();
            $user->roles()->attach($roles->pluck('id'));
        }

        $this->auditService->logAction('CREATE_USER', 'ADMIN_USER', $user->id,
            null, null, "Created admin user: {$user->username}");

        Log::info("Admin user created: {$user->username}");

        return $this->toDto($user);
    }

    public function listUsers(int $page = 0, int $size = 20)
    {
        return AdminUser::with('roles')
            ->orderBy('created_at', 'desc')
            ->paginate($size, ['*'], 'page', $page + 1)
            ->through(fn($user) => $this->toDto($user));
    }

    public function getUser(string $id): array
    {
        $user = AdminUser::with('roles')->findOrFail($id);
        return $this->toDto($user);
    }

    public function updateUserStatus(string $id, string $status): array
    {
        $user = AdminUser::findOrFail($id);
        $oldStatus = $user->status;
        $user->update(['status' => $status]);

        $this->auditService->logAction('UPDATE_USER_STATUS', 'ADMIN_USER', $id,
            json_encode(['status' => $oldStatus]),
            json_encode(['status' => $status]),
            "User status changed from {$oldStatus} to {$status}");

        return $this->toDto($user);
    }

    protected function toDto(AdminUser $user): array
    {
        $user->loadMissing('roles');
        return [
            'id' => $user->id,
            'username' => $user->username,
            'email' => $user->email,
            'fullName' => $user->full_name,
            'status' => $user->status,
            'roles' => $user->roles->pluck('name')->toArray(),
            'lastLoginAt' => $user->last_login_at?->toISOString(),
            'createdAt' => $user->created_at?->toISOString(),
        ];
    }
}
