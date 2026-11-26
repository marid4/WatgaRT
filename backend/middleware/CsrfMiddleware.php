<?php
declare(strict_types=1);

final class CsrfMiddleware
{
    public static function ensureSession(): void
    {
        if (session_status() === PHP_SESSION_NONE) {
            session_set_cookie_params([
                'httponly' => true,
                'samesite' => 'Lax',
                'secure' => (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off'),
            ]);
            session_start();
        }
    }

    public static function token(): string
    {
        self::ensureSession();
        if (empty($_SESSION['_csrf_token'])) {
            $_SESSION['_csrf_token'] = bin2hex(random_bytes(32));
        }
        return $_SESSION['_csrf_token'];
    }

    public static function verifyOrFail(): void
    {
        self::ensureSession();
        $provided = $_POST['_csrf_token'] ?? $_SERVER['HTTP_X_CSRF_TOKEN'] ?? '';
        $expected = $_SESSION['_csrf_token'] ?? '';
        if ($expected === '' || !hash_equals($expected, (string) $provided)) {
            http_response_code(403);
            exit('CSRF validation failed.');
        }
    }
}
