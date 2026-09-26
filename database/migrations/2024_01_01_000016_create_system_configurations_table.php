<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('system_configurations', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('config_key', 100)->unique();
            $table->text('config_value');
            $table->string('category', 50);
            $table->text('description')->nullable();
            $table->string('value_type', 20)->default('STRING');
            $table->boolean('is_sensitive')->default(false);
            $table->uuid('updated_by')->nullable();
            $table->timestamps();
            $table->unsignedBigInteger('version')->default(0);
        });

        Schema::create('loan_rule_configurations', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('rule_name', 100)->unique();
            $table->string('rule_type', 50);
            $table->text('rule_value');
            $table->integer('priority')->default(0);
            $table->boolean('enabled')->default(true);
            $table->text('description')->nullable();
            $table->timestamp('effective_from')->nullable();
            $table->timestamp('effective_to')->nullable();
            $table->uuid('updated_by')->nullable();
            $table->timestamps();
            $table->unsignedBigInteger('version')->default(0);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('loan_rule_configurations');
        Schema::dropIfExists('system_configurations');
    }
};
