<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('loan_transactions', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('transaction_ref', 40)->unique();
            $table->uuid('loan_id');
            $table->string('transaction_type', 30);
            $table->decimal('amount', 19, 4);
            $table->string('currency', 3)->default('TZS');
            $table->string('status', 20)->default('PENDING');
            $table->string('idempotency_key', 64)->unique()->nullable();
            $table->text('description')->nullable();
            $table->timestamps();
            $table->unsignedBigInteger('version')->default(0);

            $table->foreign('loan_id')->references('id')->on('loans');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('loan_transactions');
    }
};
