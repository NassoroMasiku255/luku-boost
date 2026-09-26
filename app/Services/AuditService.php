<?php

namespace App\Services;

use App\Models\AuditLog;
use Illuminate\Support\Facades\Log;

class AuditService
{
    public function logAction(
        string $action,
        string $entityType,
        ?string $entityId,
        ?string $oldValue,
        ?string $newValue,
        ?string $description
    ): void {
        $userId = null;
        $username = null;
        $userType = 'SYSTEM';

        $admin = request()->attributes->get('admin_user');
        if ($admin) {
            $userId = $admin->id;
            $username = $admin->username;
            $userType = 'ADMIN';
        }

        AuditLog::create([
            'user_id' => $userId,
            'user_type' => $userType,
            'username' => $username,
            'action' => $action,
            'entity_type' => $entityType,
            'entity_id' => $entityId,
            'old_value' => $oldValue,
            'new_value' => $newValue,
            'description' => $description,
            'ip_address' => request()->ip(),
            'user_agent' => request()->userAgent(),
            'correlation_id' => request()->header('X-Correlation-Id'),
            'created_at' => now(),
        ]);

        Log::debug("Audit logged: action={$action}, entity={$entityType}/{$entityId}");
    }

    public function logSystemEvent(string $action, string $entityType, ?string $entityId, ?string $description): void
    {
        AuditLog::create([
            'user_id' => null,
            'user_type' => 'SYSTEM',
            'username' => null,
            'action' => $action,
            'entity_type' => $entityType,
            'entity_id' => $entityId,
            'description' => $description,
            'correlation_id' => request()->header('X-Correlation-Id'),
            'created_at' => now(),
        ]);
    }

    public function searchAuditLogs(
        ?string $userId,
        ?string $action,
        ?string $entityType,
        ?string $entityId,
        ?string $from,
        ?string $to,
        int $page = 0,
        int $size = 20
    ) {
        $query = AuditLog::query()->orderBy('created_at', 'desc');

        if ($userId) $query->where('user_id', $userId);
        if ($action) $query->where('action', $action);
        if ($entityType) $query->where('entity_type', $entityType);
        if ($entityId) $query->where('entity_id', $entityId);
        if ($from) $query->where('created_at', '>=', $from);
        if ($to) $query->where('created_at', '<=', $to);

        return $query->paginate($size, ['*'], 'page', $page + 1);
    }
}
