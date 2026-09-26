<?php

use App\Http\Controllers\Api\Admin\AdminApiClientController;
use App\Http\Controllers\Api\Admin\AdminAuditController;
use App\Http\Controllers\Api\Admin\AdminAuthController;
use App\Http\Controllers\Api\Admin\AdminConfigController;
use App\Http\Controllers\Api\Admin\AdminCustomerController;
use App\Http\Controllers\Api\Admin\AdminDashboardController;
use App\Http\Controllers\Api\Admin\AdminLoanController;
use App\Http\Controllers\Api\Admin\AdminOtpController;
use App\Http\Controllers\Api\Admin\AdminReconciliationController;
use App\Http\Controllers\Api\Admin\AdminTransactionController;
use App\Http\Controllers\Api\Admin\AdminUserController;
use App\Http\Controllers\Api\CustomerController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Customer API Routes (API Key auth)
|--------------------------------------------------------------------------
*/
Route::prefix('v1/customer')->middleware('api.key')->group(function () {
    Route::post('/eligibility/check', [CustomerController::class, 'checkEligibility']);
    Route::post('/loans', [CustomerController::class, 'requestLoan']);
    Route::post('/loans/verify-otp', [CustomerController::class, 'verifyOtpAndDisburse']);
    Route::post('/loans/resend-otp', [CustomerController::class, 'resendOtp']);
    Route::get('/loans/history', [CustomerController::class, 'getLoanHistory']);
    Route::get('/loans/{reference}', [CustomerController::class, 'getLoan']);
});

/*
|--------------------------------------------------------------------------
| Admin Auth Routes (public)
|--------------------------------------------------------------------------
*/
Route::prefix('v1/admin/auth')->group(function () {
    Route::post('/login', [AdminAuthController::class, 'login']);
    Route::post('/refresh', [AdminAuthController::class, 'refresh']);
});

/*
|--------------------------------------------------------------------------
| Admin API Routes (JWT auth)
|--------------------------------------------------------------------------
*/
Route::prefix('v1/admin')->middleware('jwt.auth')->group(function () {
    // Dashboard
    Route::get('/dashboard/summary', [AdminDashboardController::class, 'summary']);

    // Loans
    Route::get('/loans', [AdminLoanController::class, 'searchLoans']);
    Route::get('/loans/applications', [AdminLoanController::class, 'searchApplications']);
    Route::get('/loans/{id}', [AdminLoanController::class, 'getLoanDetails']);

    // Customers
    Route::get('/customers', [AdminCustomerController::class, 'search']);
    Route::get('/customers/{id}', [AdminCustomerController::class, 'show']);

    // Transactions
    Route::get('/transactions/loan', [AdminTransactionController::class, 'searchLoanTransactions']);
    Route::get('/transactions/electricity', [AdminTransactionController::class, 'searchElectricityTransactions']);
    Route::get('/transactions/failed', [AdminTransactionController::class, 'searchFailedTransactions']);

    // OTP Management
    Route::get('/otp/{applicationRef}', [AdminOtpController::class, 'getOtp']);
    Route::post('/otp/{applicationRef}/resend', [AdminOtpController::class, 'resendOtp']);

    // Users
    Route::get('/users', [AdminUserController::class, 'index']);
    Route::get('/users/{id}', [AdminUserController::class, 'show']);
    Route::post('/users', [AdminUserController::class, 'store']);
    Route::patch('/users/{id}/status', [AdminUserController::class, 'updateStatus']);

    // Configuration
    Route::get('/configurations/system', [AdminConfigController::class, 'listSystemConfigs']);
    Route::get('/configurations/system/category/{category}', [AdminConfigController::class, 'getByCategory']);
    Route::post('/configurations/system', [AdminConfigController::class, 'createSystemConfig']);
    Route::put('/configurations/system/{id}', [AdminConfigController::class, 'updateSystemConfig']);
    Route::delete('/configurations/system/{id}', [AdminConfigController::class, 'deleteSystemConfig']);
    Route::get('/configurations/loan-rules', [AdminConfigController::class, 'listLoanRules']);
    Route::post('/configurations/loan-rules', [AdminConfigController::class, 'createLoanRule']);
    Route::put('/configurations/loan-rules/{id}', [AdminConfigController::class, 'updateLoanRule']);
    Route::delete('/configurations/loan-rules/{id}', [AdminConfigController::class, 'deleteLoanRule']);

    // API Clients
    Route::get('/api-clients', [AdminApiClientController::class, 'index']);
    Route::post('/api-clients', [AdminApiClientController::class, 'store']);
    Route::put('/api-clients/{id}', [AdminApiClientController::class, 'update']);
    Route::post('/api-clients/{id}/regenerate-key', [AdminApiClientController::class, 'regenerateKey']);
    Route::delete('/api-clients/{id}', [AdminApiClientController::class, 'destroy']);

    // Audit Logs
    Route::get('/audit-logs', [AdminAuditController::class, 'search']);

    // Reconciliation
    Route::get('/reconciliation', [AdminReconciliationController::class, 'search']);
    Route::patch('/reconciliation/{id}/resolve', [AdminReconciliationController::class, 'resolve']);
});
