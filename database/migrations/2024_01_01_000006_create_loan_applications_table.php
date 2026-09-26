<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('loan_applications', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('application_ref', 40)->unique();
            $table->uuid('customer_id');
            $table->uuid('meter_id');
            $table->string('phone_number', 20);
            $table->string('meter_number', 30);
            $table->decimal('requested_amount', 19, 4)->nullable();
            $table->decimal('eligible_amount', 19, 4)->nullable();
            $table->decimal('approved_amount', 19, 4)->nullable();
            $table->string('currency', 3)->default('TZS');
            $table->string('status', 30)->default('INITIATED');
            $table->string('eligibility_ref', 40)->nullable();
            $table->string('idempotency_key', 64)->unique()->nullable();
            $table->string('correlation_id', 64)->nullable();
            $table->text('failure_reason')->nullable();
            $table->timestamps();
            $table->unsignedBigInteger('version')->default(0);

            $table->foreign('customer_id')->references('id')->on('customers');
            $table->foreign('meter_id')->references('id')->on('meters');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('loan_applications');
    }
};
