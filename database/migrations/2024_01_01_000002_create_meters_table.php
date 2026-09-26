<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('meters', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('meter_number', 30)->unique();
            $table->uuid('customer_id')->nullable();
            $table->string('meter_type', 30)->nullable();
            $table->string('provider', 50)->nullable();
            $table->string('status', 20)->default('ACTIVE');
            $table->decimal('last_token_amount', 19, 4)->nullable();
            $table->timestamp('last_token_date')->nullable();
            $table->timestamps();
            $table->unsignedBigInteger('version')->default(0);

            $table->foreign('customer_id')->references('id')->on('customers');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('meters');
    }
};
