<?php

namespace App\Services;

use Illuminate\Support\Facades\Log;

class JwtService
{
    protected string $secret;
    protected int $accessTokenExpiryMinutes;
    protected int $refreshTokenExpiryDays;
    protected string $issuer;

    public function __construct()
    {
        $this->secret = config('app.jwt.secret', 'lukuboost-jwt-secret-key-minimum-32-characters-long-for-hs256');
        $this->accessTokenExpiryMinutes = (int) config('app.jwt.access_token_expiry_minutes', 60);
        $this->refreshTokenExpiryDays = (int) config('app.jwt.refresh_token_expiry_days', 7);
        $this->issuer = config('app.jwt.issuer', 'lukuboost');
    }

    public function generateAccessToken(string $userId, string $username, array $permissions): string
    {
        $payload = [
            'sub' => $userId,
            'iss' => $this->issuer,
            'username' => $username,
            'permissions' => $permissions,
            'type' => 'ACCESS',
            'iat' => time(),
            'exp' => time() + ($this->accessTokenExpiryMinutes * 60),
        ];

        return $this->encode($payload);
    }

    public function generateRefreshToken(string $userId): string
    {
        $payload = [
            'sub' => $userId,
            'iss' => $this->issuer,
            'type' => 'REFRESH',
            'iat' => time(),
            'exp' => time() + ($this->refreshTokenExpiryDays * 86400),
        ];

        return $this->encode($payload);
    }

    public function parseToken(string $token): ?object
    {
        try {
            return $this->decode($token);
        } catch (\Exception $e) {
            Log::debug("Invalid JWT token: {$e->getMessage()}");
            return null;
        }
    }

    public function validateToken(string $token): bool
    {
        return $this->parseToken($token) !== null;
    }

    public function getUserId(string $token): ?string
    {
        $claims = $this->parseToken($token);
        return $claims?->sub ?? null;
    }

    public function getUsername(string $token): ?string
    {
        $claims = $this->parseToken($token);
        return $claims?->username ?? null;
    }

    public function getPermissions(string $token): array
    {
        $claims = $this->parseToken($token);
        return (array) ($claims?->permissions ?? []);
    }

    public function getTokenType(string $token): ?string
    {
        $claims = $this->parseToken($token);
        return $claims?->type ?? null;
    }

    // Pure PHP JWT implementation (HS256)
    protected function encode(array $payload): string
    {
        $header = $this->base64UrlEncode(json_encode(['typ' => 'JWT', 'alg' => 'HS256']));
        $payload = $this->base64UrlEncode(json_encode($payload));
        $signature = $this->base64UrlEncode(
            hash_hmac('sha256', "{$header}.{$payload}", $this->secret, true)
        );

        return "{$header}.{$payload}.{$signature}";
    }

    protected function decode(string $token): object
    {
        $parts = explode('.', $token);
        if (count($parts) !== 3) {
            throw new \InvalidArgumentException('Invalid JWT token format.');
        }

        [$header, $payload, $signature] = $parts;

        $expectedSignature = $this->base64UrlEncode(
            hash_hmac('sha256', "{$header}.{$payload}", $this->secret, true)
        );

        if (!hash_equals($expectedSignature, $signature)) {
            throw new \InvalidArgumentException('Invalid JWT signature.');
        }

        $claims = json_decode($this->base64UrlDecode($payload));
        if (!$claims) {
            throw new \InvalidArgumentException('Invalid JWT payload.');
        }

        if (isset($claims->exp) && $claims->exp < time()) {
            throw new \InvalidArgumentException('JWT token has expired.');
        }

        return $claims;
    }

    protected function base64UrlEncode(string $data): string
    {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    protected function base64UrlDecode(string $data): string
    {
        return base64_decode(strtr($data, '-_', '+/'));
    }
}
