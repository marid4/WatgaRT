<?php
declare(strict_types=1);

// Load .env file if present
$envPath = __DIR__ . '/.env';
if (file_exists($envPath)) {
    foreach (file($envPath, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) as $line) {
        $line = trim($line);
        if ($line === '' || str_starts_with($line, '#')) {
            continue;
        }
        if (str_contains($line, '=')) {
            [$k, $v] = explode('=', $line, 2);
            $k = trim($k);
            $v = trim($v, " \t\n\r\0\x0B\"'");
            if (!array_key_exists($k, $_ENV)) {
                $_ENV[$k] = $v;
            }
        }
    }
}

function env_val(string $key, string $default = ''): string
{
    return $_ENV[$key] ?? getenv($key) ?: $default;
}

define('APP_NAME', env_val('APP_NAME', 'wargaRT'));
define('APP_ENV', env_val('APP_ENV', 'production'));
define('APP_DEBUG', env_val('APP_DEBUG', 'false') === 'true');
define('APP_URL', rtrim(env_val('APP_URL', 'https://localhost'), '/'));

define('DB_HOST', env_val('DB_HOST', 'localhost'));
define('DB_PORT', env_val('DB_PORT', '3306'));
define('DB_NAME', env_val('DB_NAME', 'wargart_db'));
define('DB_USER', env_val('DB_USER', 'root'));
define('DB_PASS', env_val('DB_PASS', ''));

define('JWT_SECRET', env_val('JWT_SECRET', 'wargart_dev_secret_change_in_production_2026'));
define('JWT_TTL_SECONDS', (int) env_val('JWT_TTL_SECONDS', '86400'));
define('DATA_ENCRYPTION_KEY', env_val('DATA_ENCRYPTION_KEY', base64_encode(hash('sha256', 'wargart_default_aes_key', true))));

define('PAYMENT_GATEWAY', env_val('PAYMENT_GATEWAY', 'midtrans'));
define('PAYMENT_SERVER_KEY', env_val('PAYMENT_SERVER_KEY', ''));
define('PAYMENT_IS_PRODUCTION', env_val('PAYMENT_IS_PRODUCTION', 'false') === 'true');

define('FCM_SERVER_KEY', env_val('FCM_SERVER_KEY', ''));
define('RATE_LIMIT_MAX_REQUESTS', (int) env_val('RATE_LIMIT_MAX_REQUESTS', '120'));
define('RATE_LIMIT_WINDOW_SECONDS', (int) env_val('RATE_LIMIT_WINDOW_SECONDS', '60'));

define('UPLOAD_BASE_DIR', dirname(__DIR__) . '/uploads');
define('MAX_UPLOAD_BYTES', 5 * 1024 * 1024); // 5 MB
