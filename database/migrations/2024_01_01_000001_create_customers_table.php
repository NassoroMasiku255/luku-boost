<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('customers', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('customer_ref', 30)->unique();
            $table->string('phone_number', 20)->unique();
            $table->string('first_name', 100)->nullable();
            $table->string('last_name', 100)->nullable();
            $table->string('status', 20)->default('ACTIVE');
            $table->timestamps();
            $table->unsignedBigInteger('version')->default(0);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('customers');
    }
};
