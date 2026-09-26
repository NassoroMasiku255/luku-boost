<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('loans', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('loan_ref', 40)->unique();
            $table->uuid('customer_id');
            $table->uuid('meter_id');
            $table->uuid('application_id')->unique();
            $table->decimal('principal_amount', 19, 4);
            $table->decimal('fee_amount', 19, 4);
            $table->decimal('total_due', 19, 4);
            $table->decimal('outstanding_amount', 19, 4);
            $table->string('currency', 3)->default('TZS');
            $table->string('status', 20)->default('ACTIVE');
            $table->timestamp('issued_at')->nullable();
            $table->timestamp('due_at')->nullable();
            $table->timestamps();
            $table->unsignedBigInteger('version')->default(0);

            $table->foreign('customer_id')->references('id')->on('customers');
            $table->foreign('meter_id')->references('id')->on('meters');
            $table->foreign('application_id')->references('id')->on('loan_applications');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('loans');
    }
};
