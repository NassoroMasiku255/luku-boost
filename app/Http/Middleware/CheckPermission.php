<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;

class CheckPermission
{
    public function handle(Request $request, Closure $next, string $permission)
    {
        $permissions = $request->attributes->get('admin_permissions', []);

        if (!in_array($permission, $permissions)) {
            return response()->json([
                'success' => false,
                'error' => ['code' => 'FORBIDDEN', 'message' => 'You do not have the required permission.'],
                'data' => null,
            ], 403);
        }

        return $next($request);
    }
}
