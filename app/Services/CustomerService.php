<?php

namespace App\Services;

use App\Exceptions\BusinessException;
use App\Helpers\ReferenceGenerator;
use App\Models\Customer;
use App\Models\Meter;
use Illuminate\Support\Facades\Log;

class CustomerService
{
    public function findOrCreateCustomer(string $phoneNumber): Customer
    {
        $customer = Customer::where('phone_number', $phoneNumber)->first();
        if ($customer) {
            return $customer;
        }

        $customer = Customer::create([
            'customer_ref' => ReferenceGenerator::generateCustomerReference(),
            'phone_number' => $phoneNumber,
            'status' => 'ACTIVE',
        ]);

        Log::info("Created new customer ref={$customer->customer_ref}");
        return $customer;
    }

    public function findOrCreateMeter(string $meterNumber, Customer $customer): Meter
    {
        $meter = Meter::where('meter_number', $meterNumber)->first();
        if ($meter) {
            return $meter;
        }

        $meter = Meter::create([
            'meter_number' => $meterNumber,
            'customer_id' => $customer->id,
            'status' => 'ACTIVE',
        ]);

        Log::info("Created new meter number={$meterNumber}");
        return $meter;
    }

    public function searchCustomers(?string $phone, ?string $status, ?string $name, int $page = 0, int $size = 20)
    {
        $query = Customer::query();

        if ($phone) $query->where('phone_number', 'like', "%{$phone}%");
        if ($status) $query->where('status', $status);
        if ($name) {
            $query->where(function ($q) use ($name) {
                $q->where('first_name', 'like', "%{$name}%")
                  ->orWhere('last_name', 'like', "%{$name}%");
            });
        }

        return $query->orderBy('created_at', 'desc')->paginate($size, ['*'], 'page', $page + 1);
    }

    public function getById(string $id): Customer
    {
        return Customer::findOrFail($id);
    }

    public function getByRef(string $ref): Customer
    {
        $customer = Customer::where('customer_ref', $ref)->first();
        if (!$customer) {
            throw new BusinessException('CUSTOMER_NOT_FOUND', "Customer not found: {$ref}", 404);
        }
        return $customer;
    }

    public function getByPhone(string $phoneNumber): Customer
    {
        $customer = Customer::where('phone_number', $phoneNumber)->first();
        if (!$customer) {
            throw new BusinessException('CUSTOMER_NOT_FOUND', "Customer not found: {$phoneNumber}", 404);
        }
        return $customer;
    }
}
