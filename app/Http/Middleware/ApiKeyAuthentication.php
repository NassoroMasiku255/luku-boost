<?php

namespace App\Http\Middleware;

use App\Models\ApiClient;
use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;

class ApiKeyAuthentication
{
    public function handle(Request $request, Closure $next)
    {
        $clientId = $request->header('X-Client-Id');
        $apiKey = $request->header('X-Api-Key');

        if (!$clientId || !$apiKey) {
            return response()->json([
                'success' => false,
                'error' => ['code' => 'UNAUTHORIZED', 'message' => 'Missing API credentials. Provide X-Client-Id and X-Api-Key headers.'],
                'data' => null,
            ], 401);
        }

        $client = ApiClient::where('client_id', $clientId)->first();

        if (!$client || !$client->enabled) {
            return response()->json([
                'success' => false,
                'error' => ['code' => 'UNAUTHORIZED', 'message' => 'Invalid or disabled API client.'],
                'data' => null,
            ], 401);
        }

        if (!Hash::check($apiKey, $client->api_key_hash)) {
            return response()->json([
                'success' => false,
                'error' => ['code' => 'UNAUTHORIZED', 'message' => 'Invalid API key.'],
                'data' => null,
            ], 401);
        }

        // IP whitelist check
        if ($client->allowed_ips) {
            $allowedIps = array_map('trim', explode(',', $client->allowed_ips));
            $clientIp = $request->ip();
            if (!empty($allowedIps[0]) && !in_array($clientIp, $allowedIps)) {
                return response()->json([
                    'success' => false,
                    'error' => ['code' => 'FORBIDDEN', 'message' => 'Your IP address is not whitelisted.'],
                    'data' => null,
                ], 403);
            }
        }

        // Rate limiting
        $cacheKey = "rate_limit:{$clientId}";
        $requests = (int) Cache::get($cacheKey, 0);
        if ($requests >= $client->rate_limit_per_minute) {
            return response()->json([
                'success' => false,
                'error' => ['code' => 'RATE_LIMITED', 'message' => 'Rate limit exceeded. Please try again later.'],
                'data' => null,
            ], 429);
        }
        Cache::put($cacheKey, $requests + 1, 60);

        $client->update(['last_used_at' => now()]);

        $request->attributes->set('api_client', $client);

        return $next($request);
    }
}
