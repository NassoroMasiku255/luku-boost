<?php

namespace App\Http\Controllers\Api\Admin;

use App\Http\Controllers\Controller;
use App\Services\DashboardService;
use Illuminate\Http\JsonResponse;

class AdminDashboardController extends Controller
{
    protected DashboardService $dashboardService;

    public function __construct(DashboardService $dashboardService)
    {
        $this->dashboardService = $dashboardService;
    }

    public function summary(): JsonResponse
    {
        $data = $this->dashboardService->getSummary();
        return response()->json(['success' => true, 'error' => null, 'data' => $data]);
    }
}
