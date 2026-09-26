<?php

namespace App\Http\Controllers\Api\Admin;

use App\Http\Controllers\Controller;
use App\Services\AdminUserService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminUserController extends Controller
{
    protected AdminUserService $adminUserService;

    public function __construct(AdminUserService $adminUserService)
    {
        $this->adminUserService = $adminUserService;
    }

    public function index(Request $request): JsonResponse
    {
        $page = $this->adminUserService->listUsers(
            (int) $request->input('page', 0),
            (int) $request->input('size', 20)
        );

        return response()->json([
            'success' => true, 'error' => null,
            'data' => [
                'content' => $page->items(),
                'page' => $page->currentPage() - 1,
                'size' => $page->perPage(),
                'totalElements' => $page->total(),
                'totalPages' => $page->lastPage(),
                'first' => $page->currentPage() === 1,
                'last' => $page->currentPage() === $page->lastPage(),
            ],
        ]);
    }

    public function show(string $id): JsonResponse
    {
        $user = $this->adminUserService->getUser($id);
        return response()->json(['success' => true, 'error' => null, 'data' => $user]);
    }

    public function store(Request $request): JsonResponse
    {
        $request->validate([
            'username' => 'required|string|min:3',
            'email' => 'required|email',
            'password' => 'required|string|min:8',
            'fullName' => 'required|string',
        ]);

        $user = $this->adminUserService->createUser($request->all());
        return response()->json(['success' => true, 'error' => null, 'data' => $user], 201);
    }

    public function updateStatus(Request $request, string $id): JsonResponse
    {
        $user = $this->adminUserService->updateUserStatus($id, $request->input('status'));
        return response()->json(['success' => true, 'error' => null, 'data' => $user]);
    }
}
