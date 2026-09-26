<?php

namespace App\Http\Controllers\Api\Admin;

use App\Http\Controllers\Controller;
use App\Models\ElectricityTransaction;
use App\Models\FailedTransaction;
use App\Models\LoanTransaction;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminTransactionController extends Controller
{
    public function searchLoanTransactions(Request $request): JsonResponse
    {
        $query = LoanTransaction::query();
        if ($request->filled('ref')) $query->where('transaction_ref', 'like', '%' . $request->input('ref') . '%');
        if ($request->filled('status')) $query->where('status', $request->input('status'));
        if ($request->filled('type')) $query->where('transaction_type', $request->input('type'));
        if ($request->filled('from')) $query->where('created_at', '>=', $request->input('from'));
        if ($request->filled('to')) $query->where('created_at', '<=', $request->input('to'));

        $page = $query->orderBy('created_at', 'desc')
            ->paginate($request->input('size', 20), ['*'], 'page', ($request->input('page', 0)) + 1);

        return response()->json(['success' => true, 'error' => null, 'data' => $this->toPagedResponse($page)]);
    }

    public function searchElectricityTransactions(Request $request): JsonResponse
    {
        $query = ElectricityTransaction::query();
        if ($request->filled('ref')) $query->where('internal_ref', 'like', '%' . $request->input('ref') . '%');
        if ($request->filled('providerRef')) $query->where('provider_ref', 'like', '%' . $request->input('providerRef') . '%');
        if ($request->filled('status')) $query->where('request_status', $request->input('status'));
        if ($request->filled('meterNumber')) $query->where('meter_number', $request->input('meterNumber'));
        if ($request->filled('from')) $query->where('created_at', '>=', $request->input('from'));
        if ($request->filled('to')) $query->where('created_at', '<=', $request->input('to'));

        $page = $query->orderBy('created_at', 'desc')
            ->paginate($request->input('size', 20), ['*'], 'page', ($request->input('page', 0)) + 1);

        return response()->json(['success' => true, 'error' => null, 'data' => $this->toPagedResponse($page)]);
    }

    public function searchFailedTransactions(Request $request): JsonResponse
    {
        $query = FailedTransaction::query();
        if ($request->filled('status')) $query->where('status', $request->input('status'));
        if ($request->filled('type')) $query->where('transaction_type', $request->input('type'));
        if ($request->filled('from')) $query->where('created_at', '>=', $request->input('from'));
        if ($request->filled('to')) $query->where('created_at', '<=', $request->input('to'));

        $page = $query->orderBy('created_at', 'desc')
            ->paginate($request->input('size', 20), ['*'], 'page', ($request->input('page', 0)) + 1);

        return response()->json(['success' => true, 'error' => null, 'data' => $this->toPagedResponse($page)]);
    }

    protected function toPagedResponse($page): array
    {
        return [
            'content' => $page->items(),
            'page' => $page->currentPage() - 1,
            'size' => $page->perPage(),
            'totalElements' => $page->total(),
            'totalPages' => $page->lastPage(),
            'first' => $page->currentPage() === 1,
            'last' => $page->currentPage() === $page->lastPage(),
        ];
    }
}
