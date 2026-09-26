<?php

namespace App\Http\Controllers\Api\Admin;

use App\Http\Controllers\Controller;
use App\Models\Loan;
use App\Models\Meter;
use App\Services\CustomerService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminCustomerController extends Controller
{
    protected CustomerService $customerService;

    public function __construct(CustomerService $customerService)
    {
        $this->customerService = $customerService;
    }

    public function search(Request $request): JsonResponse
    {
        $page = $this->customerService->searchCustomers(
            $request->input('phone'),
            $request->input('status'),
            $request->input('name'),
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
        $customer = $this->customerService->getById($id);
        $meters = Meter::where('customer_id', $id)->get();
        $loans = Loan::where('customer_id', $id)->get();

        return response()->json([
            'success' => true, 'error' => null,
            'data' => [
                'customer' => $customer,
                'meters' => $meters,
                'loans' => $loans,
            ],
        ]);
    }
}
