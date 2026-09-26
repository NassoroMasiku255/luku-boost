<?php

namespace App\Http\Middleware;

use App\Models\AdminUser;
use App\Services\JwtService;
use Closure;
use Illuminate\Http\Request;

class JwtAuthentication
{
    protected JwtService $jwtService;

    public function __construct(JwtService $jwtService)
    {
        $this->jwtService = $jwtService;
    }

    public function handle(Request $request, Closure $next)
    {
        $authHeader = $request->header('Authorization');

        if (!$authHeader || !str_starts_with($authHeader, 'Bearer ')) {
            return response()->json([
                'success' => false,
                'error' => ['code' => 'UNAUTHORIZED', 'message' => 'Missing or invalid Authorization header.'],
                'data' => null,
            ], 401);
        }

        $token = substr($authHeader, 7);
        $claims = $this->jwtService->parseToken($token);

        if (!$claims || ($claims->type ?? null) !== 'ACCESS') {
            return response()->json([
                'success' => false,
                'error' => ['code' => 'UNAUTHORIZED', 'message' => 'Invalid or expired token.'],
                'data' => null,
            ], 401);
        }

        $user = AdminUser::find($claims->sub);
        if (!$user || $user->status !== 'ACTIVE') {
            return response()->json([
                'success' => false,
                'error' => ['code' => 'UNAUTHORIZED', 'message' => 'User not found or inactive.'],
                'data' => null,
            ], 401);
        }

        $request->attributes->set('admin_user', $user);
        $request->attributes->set('admin_permissions', $claims->permissions ?? []);

        return $next($request);
    }
}
