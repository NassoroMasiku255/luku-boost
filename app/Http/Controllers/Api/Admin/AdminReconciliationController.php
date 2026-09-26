<?php

namespace App\Http\Controllers\Api\Admin;

use App\Http\Controllers\Controller;
use App\Models\ReconciliationRecord;
use App\Services\AuditService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminReconciliationController extends Controller
{
    protected AuditService $auditService;

    public function __construct(AuditService $auditService)
    {
        $this->auditService = $auditService;
    }

    public function search(Request $request): JsonResponse
    {
        $query = ReconciliationRecord::query();
        if ($request->filled('status')) $query->where('resolution_status', $request->input('status'));
        if ($request->filled('mismatchType')) $query->where('mismatch_type', $request->input('mismatchType'));
        if ($request->filled('from')) $query->where('created_at', '>=', $request->input('from'));
        if ($request->filled('to')) $query->where('created_at', '<=', $request->input('to'));

        $page = $query->orderBy('created_at', 'desc')
            ->paginate($request->input('size', 20), ['*'], 'page', ($request->input('page', 0)) + 1);

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

    public function resolve(Request $request, string $id): JsonResponse
    {
        $record = ReconciliationRecord::findOrFail($id);
        $admin = $request->attributes->get('admin_user');

        $record->update([
            'resolution_status' => $request->input('status', 'RESOLVED'),
            'resolution_notes' => $request->input('notes'),
            'resolved_by' => $admin?->id,
            'resolved_at' => now(),
        ]);

        $this->auditService->logAction('MANUAL_RECONCILIATION', 'RECONCILIATION', $id,
            null, null, "Resolved reconciliation: {$record->recon_ref}");

        return response()->json(['success' => true, 'error' => null, 'data' => $record]);
    }
}
