<?php

namespace App\Http\Controllers\Api\Admin;

use App\Http\Controllers\Controller;
use App\Services\AuditService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminAuditController extends Controller
{
    protected AuditService $auditService;

    public function __construct(AuditService $auditService)
    {
        $this->auditService = $auditService;
    }

    public function search(Request $request): JsonResponse
    {
        $page = $this->auditService->searchAuditLogs(
            $request->input('userId'),
            $request->input('action'),
            $request->input('entityType'),
            $request->input('entityId'),
            $request->input('from'),
            $request->input('to'),
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
}
