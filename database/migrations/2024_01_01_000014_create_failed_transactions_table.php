<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('failed_transactions', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('original_ref', 40);
            $table->string('transaction_type', 30);
            $table->string('entity_type', 30)->nullable();
            $table->uuid('entity_id')->nullable();
            $table->text('failure_reason');
            $table->integer('retry_count')->default(0);
            $table->integer('max_retries')->default(3);
            $table->timestamp('last_retry_at')->nullable();
            $table->timestamp('next_retry_at')->nullable();
            $table->string('status', 20)->default('PENDING');
            $table->text('resolution_notes')->nullable();
            $table->uuid('resolved_by')->nullable();
            $table->timestamp('resolved_at')->nullable();
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('failed_transactions');
    }
};
