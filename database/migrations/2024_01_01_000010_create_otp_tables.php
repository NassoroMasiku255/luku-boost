<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('otp_requests', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('phone_number', 20);
            $table->string('otp_hash', 128);
            $table->string('salt', 64);
            $table->string('otp_plain', 10)->nullable();
            $table->string('purpose', 30)->default('LOAN_APPLICATION');
            $table->string('status', 20)->default('PENDING');
            $table->integer('attempts_remaining')->default(5);
            $table->string('application_ref', 40)->nullable();
            $table->string('ip_address', 45)->nullable();
            $table->timestamp('expires_at');
            $table->timestamp('verified_at')->nullable();
            $table->timestamps();

            $table->index('application_ref');
            $table->index('phone_number');
        });

        Schema::create('otp_verifications', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->uuid('otp_request_id');
            $table->boolean('success')->default(false);
            $table->string('ip_address', 45)->nullable();
            $table->string('user_agent')->nullable();
            $table->timestamp('created_at')->useCurrent();

            $table->foreign('otp_request_id')->references('id')->on('otp_requests');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('otp_verifications');
        Schema::dropIfExists('otp_requests');
    }
};
