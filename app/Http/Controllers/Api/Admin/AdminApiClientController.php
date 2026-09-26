<?php

namespace App\Http\Controllers\Api\Admin;

use App\Exceptions\BusinessException;
use App\Http\Controllers\Controller;
use App\Models\ApiClient;
use App\Services\AuditService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class AdminApiClientController extends Controller
{
    protected AuditService $auditService;

    public function __construct(AuditService $auditService)
    {
        $this->auditService = $auditService;
    }

    public function index(): JsonResponse
    {
        $clients = ApiClient::all()->map(fn($c) => $this->toSafeMap($c));
        return response()->json(['success' => true, 'error' => null, 'data' => $clients]);
    }

    public function store(Request $request): JsonResponse
    {
        $clientName = $request->input('clientName');
        if (!$clientName) {
            throw new BusinessException('INVALID_CLIENT_NAME', 'Client name is required.', 400);
        }

        $clientId = $this->generateClientId();
        $rawApiKey = $this->generateApiKey();

        $admin = $request->attributes->get('admin_user');

        $client = ApiClient::create([
            'client_name' => $clientName,
            'client_id' => $clientId,
            'api_key_hash' => Hash::make($rawApiKey),
            'description' => $request->input('description'),
            'allowed_ips' => $request->input('allowedIps'),
            'rate_limit_per_minute' => (int) $request->input('rateLimitPerMinute', 60),
            'enabled' => true,
            'created_by' => $admin?->id,
        ]);

        $this->auditService->logAction('API_CLIENT_CREATED', 'API_CLIENT', $client->id,
            null, $clientName, "Created API client: {$clientName}");

        return response()->json([
            'success' => true, 'error' => null,
            'data' => [
                'id' => $client->id,
                'clientName' => $client->client_name,
                'clientId' => $clientId,
                'apiKey' => $rawApiKey,
                'description' => $client->description,
                'allowedIps' => $client->allowed_ips,
                'rateLimitPerMinute' => $client->rate_limit_per_minute,
                'enabled' => $client->enabled,
                'message' => 'Save these credentials securely. The API key will NOT be shown again.',
            ],
        ], 201);
    }

    public function update(Request $request, string $id): JsonResponse
    {
        $client = ApiClient::findOrFail($id);

        if ($request->has('clientName')) $client->client_name = $request->input('clientName');
        if ($request->has('description')) $client->description = $request->input('description');
        if ($request->has('allowedIps')) $client->allowed_ips = $request->input('allowedIps');
        if ($request->has('enabled')) $client->enabled = (bool) $request->input('enabled');
        if ($request->has('rateLimitPerMinute')) $client->rate_limit_per_minute = (int) $request->input('rateLimitPerMinute');

        $client->save();

        $this->auditService->logAction('API_CLIENT_UPDATED', 'API_CLIENT', $id,
            null, null, "Updated API client: {$client->client_name}");

        return response()->json(['success' => true, 'error' => null, 'data' => $this->toSafeMap($client)]);
    }

    public function regenerateKey(string $id): JsonResponse
    {
        $client = ApiClient::findOrFail($id);
        $newApiKey = $this->generateApiKey();
        $client->update(['api_key_hash' => Hash::make($newApiKey)]);

        $this->auditService->logAction('API_KEY_REGENERATED', 'API_CLIENT', $id,
            null, null, "Regenerated API key for: {$client->client_name}");

        return response()->json([
            'success' => true, 'error' => null,
            'data' => [
                'clientId' => $client->client_id,
                'apiKey' => $newApiKey,
                'message' => 'Save this key securely. It will NOT be shown again.',
            ],
        ]);
    }

    public function destroy(string $id): JsonResponse
    {
        $client = ApiClient::findOrFail($id);
        $client->delete();

        $this->auditService->logAction('API_CLIENT_DELETED', 'API_CLIENT', $id,
            $client->client_name, null, "Deleted API client: {$client->client_name}");

        return response()->json(['success' => true, 'error' => null, 'data' => null]);
    }

    protected function toSafeMap(ApiClient $client): array
    {
        return [
            'id' => $client->id,
            'clientName' => $client->client_name,
            'clientId' => $client->client_id,
            'description' => $client->description,
            'allowedIps' => $client->allowed_ips,
            'rateLimitPerMinute' => $client->rate_limit_per_minute,
            'enabled' => $client->enabled,
            'lastUsedAt' => $client->last_used_at?->toISOString(),
            'createdAt' => $client->created_at?->toISOString(),
        ];
    }

    protected function generateClientId(): string
    {
        return 'lkb_' . bin2hex(random_bytes(16));
    }

    protected function generateApiKey(): string
    {
        return 'lkb_key_' . bin2hex(random_bytes(32));
    }
}
