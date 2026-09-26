<?php

namespace App\Http\Controllers\Api\Admin;

use App\Http\Controllers\Controller;
use App\Services\AdminAuthService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminAuthController extends Controller
{
    protected AdminAuthService $authService;

    public function __construct(AdminAuthService $authService)
    {
        $this->authService = $authService;
    }

    public function login(Request $request): JsonResponse
    {
        $request->validate([
            'username' => 'required|string',
            'password' => 'required|string',
        ]);

        $result = $this->authService->login(
            $request->input('username'),
            $request->input('password')
        );

        return response()->json([
            'success' => true,
            'error' => null,
            'data' => $result,
        ]);
    }

    public function refresh(Request $request): JsonResponse
    {
        $request->validate(['refreshToken' => 'required|string']);

        $result = $this->authService->refreshToken($request->input('refreshToken'));

        return response()->json([
            'success' => true,
            'error' => null,
            'data' => $result,
        ]);
    }
}
