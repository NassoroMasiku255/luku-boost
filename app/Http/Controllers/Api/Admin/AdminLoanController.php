<?php

namespace App\Http\Controllers\Api\Admin;

use App\Http\Controllers\Controller;
use App\Models\ElectricityTransaction;
use App\Models\Loan;
use App\Models\LoanApplication;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminLoanController extends Controller
{
    public function searchLoans(Request $request): JsonResponse
    {
        $query = Loan::query();

        if ($request->filled('ref')) $query->where('loan_ref', 'like', '%' . $request->input('ref') . '%');
        if ($request->filled('status')) $query->where('status', $request->input('status'));
        if ($request->filled('phone')) $query->whereHas('customer', fn($q) => $q->where('phone_number', 'like', '%' . $request->input('phone') . '%'));
        if ($request->filled('from')) $query->where('created_at', '>=', $request->input('from'));
        if ($request->filled('to')) $query->where('created_at', '<=', $request->input('to'));

        $page = $query->orderBy('created_at', 'desc')
            ->paginate($request->input('size', 20), ['*'], 'page', ($request->input('page', 0)) + 1);

        return response()->json(['success' => true, 'error' => null, 'data' => $this->toPagedResponse($page)]);
    }

    public function searchApplications(Request $request): JsonResponse
    {
        $query = LoanApplication::query();

        if ($request->filled('ref')) $query->where('application_ref', 'like', '%' . $request->input('ref') . '%');
        if ($request->filled('status')) $query->where('status', $request->input('status'));
        if ($request->filled('phone')) $query->where('phone_number', 'like', '%' . $request->input('phone') . '%');
        if ($request->filled('from')) $query->where('created_at', '>=', $request->input('from'));
        if ($request->filled('to')) $query->where('created_at', '<=', $request->input('to'));

        $page = $query->orderBy('created_at', 'desc')
            ->paginate($request->input('size', 20), ['*'], 'page', ($request->input('page', 0)) + 1);

        return response()->json(['success' => true, 'error' => null, 'data' => $this->toPagedResponse($page)]);
    }

    public function getLoanDetails(string $id): JsonResponse
    {
        $loan = Loan::with(['customer', 'meter', 'application'])->findOrFail($id);
        $elecTxn = ElectricityTransaction::where('loan_id', $id)->first();

        return response()->json([
            'success' => true, 'error' => null,
            'data' => [
                'loan' => $loan,
                'electricityTransaction' => $elecTxn,
            ],
        ]);
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
