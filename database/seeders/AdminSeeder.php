<?php

namespace Database\Seeders;

use App\Models\AdminUser;
use App\Models\Permission;
use App\Models\Role;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

class AdminSeeder extends Seeder
{
    public function run(): void
    {
        // Create all permissions
        $permissions = [
            // Loan
            ['name' => 'LOAN_VIEW', 'description' => 'View loans', 'module' => 'LOAN'],
            ['name' => 'LOAN_MANAGE', 'description' => 'Manage loans', 'module' => 'LOAN'],
            // Customer
            ['name' => 'CUSTOMER_VIEW', 'description' => 'View customers', 'module' => 'CUSTOMER'],
            ['name' => 'CUSTOMER_MANAGE', 'description' => 'Manage customers', 'module' => 'CUSTOMER'],
            // Transaction
            ['name' => 'TRANSACTION_VIEW', 'description' => 'View transactions', 'module' => 'TRANSACTION'],
            // Config
            ['name' => 'CONFIG_VIEW', 'description' => 'View configuration', 'module' => 'CONFIG'],
            ['name' => 'CONFIG_UPDATE', 'description' => 'Update configuration', 'module' => 'CONFIG'],
            ['name' => 'CONFIG_MANAGE', 'description' => 'Full configuration management', 'module' => 'CONFIG'],
            // User
            ['name' => 'USER_VIEW', 'description' => 'View admin users', 'module' => 'USER'],
            ['name' => 'USER_MANAGE', 'description' => 'Manage admin users', 'module' => 'USER'],
            // Audit
            ['name' => 'AUDIT_VIEW', 'description' => 'View audit logs', 'module' => 'AUDIT'],
            // Reconciliation
            ['name' => 'RECONCILIATION_VIEW', 'description' => 'View reconciliation records', 'module' => 'RECONCILIATION'],
            ['name' => 'RECONCILIATION_MANAGE', 'description' => 'Manage reconciliation records', 'module' => 'RECONCILIATION'],
            // Dashboard
            ['name' => 'DASHBOARD_VIEW', 'description' => 'View dashboard', 'module' => 'DASHBOARD'],
        ];

        $permissionModels = [];
        foreach ($permissions as $p) {
            $permissionModels[] = Permission::firstOrCreate(
                ['name' => $p['name']],
                array_merge($p, ['id' => (string) Str::uuid(), 'created_at' => now()])
            );
        }

        // Create SUPER_ADMIN role with all permissions
        $superAdminRole = Role::firstOrCreate(
            ['name' => 'SUPER_ADMIN'],
            ['id' => (string) Str::uuid(), 'description' => 'Super Administrator with all permissions', 'status' => 'ACTIVE']
        );
        $superAdminRole->permissions()->sync(collect($permissionModels)->pluck('id'));

        // Create ADMIN role with most permissions
        $adminRole = Role::firstOrCreate(
            ['name' => 'ADMIN'],
            ['id' => (string) Str::uuid(), 'description' => 'Administrator', 'status' => 'ACTIVE']
        );
        $adminPermissions = collect($permissionModels)->filter(fn($p) => !str_contains($p->name, 'MANAGE'));
        $adminRole->permissions()->sync($adminPermissions->pluck('id'));

        // Create VIEWER role
        $viewerRole = Role::firstOrCreate(
            ['name' => 'VIEWER'],
            ['id' => (string) Str::uuid(), 'description' => 'Read-only viewer', 'status' => 'ACTIVE']
        );
        $viewerPermissions = collect($permissionModels)->filter(fn($p) => str_contains($p->name, 'VIEW'));
        $viewerRole->permissions()->sync($viewerPermissions->pluck('id'));

        // Create superadmin user
        $superadmin = AdminUser::firstOrCreate(
            ['username' => 'superadmin'],
            [
                'id' => (string) Str::uuid(),
                'email' => 'superadmin@lukuboost.com',
                'password_hash' => Hash::make('Admin@2026!'),
                'full_name' => 'Super Administrator',
                'status' => 'ACTIVE',
            ]
        );
        $superadmin->roles()->sync([$superAdminRole->id]);

        $this->command->info('Admin seeder completed: superadmin/Admin@2026!');
    }
}
