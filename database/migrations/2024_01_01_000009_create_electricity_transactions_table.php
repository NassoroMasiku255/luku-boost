<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('electricity_transactions', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('internal_ref', 40)->unique();
            $table->uuid('loan_id')->nullable();
            $table->uuid('application_id')->nullable();
            $table->string('meter_number', 30);
            $table->decimal('amount', 19, 4);
            $table->string('currency', 3)->default('TZS');
            $table->string('request_status', 20)->default('PENDING');
            $table->timestamp('request_timestamp')->nullable();
            $table->timestamp('response_timestamp')->nullable();
            $table->string('provider_ref', 100)->nullable();
            $table->string('provider_status', 50)->nullable();
            $table->string('token', 100)->nullable();
            $table->string('token_units', 50)->nullable();
            $table->text('failure_reason')->nullable();
            $table->string('idempotency_key', 64)->unique()->nullable();
            $table->string('correlation_id', 64)->nullable();
            $table->timestamps();
            $table->unsignedBigInteger('version')->default(0);

            $table->foreign('loan_id')->references('id')->on('loans');
            $table->foreign('application_id')->references('id')->on('loan_applications');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('electricity_transactions');
    }
};
