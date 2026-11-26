<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/config.php';

final class RateLimitMiddleware
{
    public static function check(string $keyPrefix = 'api'): void
    {
        $ip = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
        $hash = sha1($keyPrefix . '|' . $ip);
        $dir = sys_get_temp_dir() . '/wargart_ratelimit';
        if (!is_dir($dir)) {
            @mkdir($dir, 0750, true);
        }
        $file = $dir . '/' . $hash . '.json';
        $now = time();
        $windowStart = $now - RATE_LIMIT_WINDOW_SECONDS;

        $timestamps = [];
        if (file_exists($file)) {
            $raw = @file_get_contents($file);
            $decoded = $raw ? json_decode($raw, true) : [];
            if (is_array($decoded)) {
                $timestamps = array_values(array_filter($decoded, static fn($t) => (int) $t >= $windowStart));
            }
        }

        if (count($timestamps) >= RATE_LIMIT_MAX_REQUESTS) {
            http_response_code(429);
            header('Content-Type: application/json; charset=utf-8');
            echo json_encode(['success' => false, 'message' => 'Terlalu banyak permintaan. Coba lagi sebentar.']);
            exit;
        }

        $timestamps[] = $now;
        @file_put_contents($file, json_encode($timestamps), LOCK_EX);
    }
}
