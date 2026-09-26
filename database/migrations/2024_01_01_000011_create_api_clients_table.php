<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('api_clients', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('client_name', 100);
            $table->string('client_id', 64)->unique();
            $table->string('api_key_hash', 255);
            $table->string('description')->nullable();
            $table->boolean('enabled')->default(true);
            $table->string('allowed_ips')->nullable();
            $table->integer('rate_limit_per_minute')->default(60);
            $table->timestamp('last_used_at')->nullable();
            $table->uuid('created_by')->nullable();
            $table->timestamps();
            $table->unsignedBigInteger('version')->default(0);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('api_clients');
    }
};
