<?php

namespace App\Services;

use App\Exceptions\BusinessException;
use App\Models\AdminUser;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Log;

class AdminAuthService
{
    protected JwtService $jwtService;
    protected AuditService $auditService;

    public function __construct(JwtService $jwtService, AuditService $auditService)
    {
        $this->jwtService = $jwtService;
        $this->auditService = $auditService;
    }

    public function login(string $username, string $password): array
    {
        $user = AdminUser::where('username', $username)->first();

        if (!$user || !Hash::check($password, $user->password_hash)) {
            throw new BusinessException('INVALID_CREDENTIALS', 'Invalid username or password.', 401);
        }

        if ($user->status !== 'ACTIVE') {
            throw new BusinessException('ACCOUNT_DISABLED', 'Your account has been disabled.', 403);
        }

        $user->load('roles.permissions');
        $permissions = $user->getAllPermissions();
        $roles = $user->getRoleNames();

        $accessToken = $this->jwtService->generateAccessToken($user->id, $user->username, $permissions);
        $refreshToken = $this->jwtService->generateRefreshToken($user->id);

        $user->update(['last_login_at' => now()]);

        $this->auditService->logSystemEvent('ADMIN_LOGIN', 'ADMIN_USER', $user->id,
            "Admin user logged in: {$user->username}");

        Log::info("Admin user logged in: {$user->username}");

        return [
            'accessToken' => $accessToken,
            'refreshToken' => $refreshToken,
            'id' => $user->id,
            'username' => $user->username,
            'email' => $user->email,
            'fullName' => $user->full_name,
            'roles' => $roles,
            'permissions' => $permissions,
        ];
    }

    public function refreshToken(string $refreshToken): array
    {
        $claims = $this->jwtService->parseToken($refreshToken);
        if (!$claims || ($claims->type ?? null) !== 'REFRESH') {
            throw new BusinessException('INVALID_REFRESH_TOKEN', 'Invalid or expired refresh token.', 401);
        }

        $user = AdminUser::find($claims->sub);
        if (!$user || $user->status !== 'ACTIVE') {
            throw new BusinessException('ACCOUNT_DISABLED', 'Account not found or disabled.', 403);
        }

        $user->load('roles.permissions');
        $permissions = $user->getAllPermissions();

        $newAccessToken = $this->jwtService->generateAccessToken($user->id, $user->username, $permissions);
        $newRefreshToken = $this->jwtService->generateRefreshToken($user->id);

        return [
            'accessToken' => $newAccessToken,
            'refreshToken' => $newRefreshToken,
        ];
    }
}
