<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/config.php';

final class JwtService
{
    public static function encode(array $payload, ?int $ttl = null): string
    {
        $now = time();
        $ttl = $ttl ?? JWT_TTL_SECONDS;
        $header = ['alg' => 'HS256', 'typ' => 'JWT'];
        $payload['iat'] = $now;
        $payload['exp'] = $now + $ttl;

        $segments = [
            self::base64UrlEncode(json_encode($header, JSON_THROW_ON_ERROR)),
            self::base64UrlEncode(json_encode($payload, JSON_THROW_ON_ERROR)),
        ];
        $signingInput = implode('.', $segments);
        $signature = hash_hmac('sha256', $signingInput, JWT_SECRET, true);
        $segments[] = self::base64UrlEncode($signature);

        return implode('.', $segments);
    }

    public static function decode(string $token): ?array
    {
        $parts = explode('.', $token);
        if (count($parts) !== 3) {
            return null;
        }
        [$headerB64, $payloadB64, $sigB64] = $parts;
        $signingInput = $headerB64 . '.' . $payloadB64;
        $expectedSig = self::base64UrlEncode(hash_hmac('sha256', $signingInput, JWT_SECRET, true));
        if (!hash_equals($expectedSig, $sigB64)) {
            return null;
        }
        $payloadJson = self::base64UrlDecode($payloadB64);
        $payload = json_decode($payloadJson, true);
        if (!is_array($payload)) {
            return null;
        }
        if (isset($payload['exp']) && (int) $payload['exp'] < time()) {
            return null;
        }
        return $payload;
    }

    private static function base64UrlEncode(string $data): string
    {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    private static function base64UrlDecode(string $data): string
    {
        $remainder = strlen($data) % 4;
        if ($remainder > 0) {
            $data .= str_repeat('=', 4 - $remainder);
        }
        return base64_decode(strtr($data, '-_', '+/')) ?: '';
    }
}
