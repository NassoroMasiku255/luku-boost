<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('reconciliation_records', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('recon_ref', 40)->unique();
            $table->string('internal_ref', 40);
            $table->string('provider_ref', 100)->nullable();
            $table->string('internal_status', 20);
            $table->string('provider_status', 50)->nullable();
            $table->decimal('internal_amount', 19, 4)->nullable();
            $table->decimal('provider_amount', 19, 4)->nullable();
            $table->string('mismatch_type', 50);
            $table->string('resolution_status', 20)->default('PENDING');
            $table->text('resolution_notes')->nullable();
            $table->uuid('resolved_by')->nullable();
            $table->timestamp('resolved_at')->nullable();
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('reconciliation_records');
    }
};
