<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../services/JwtService.php';

final class AuthMiddleware
{
    public static function authenticate(): array
    {
        $header = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
        if (!preg_match('/Bearer\s+(.+)/i', $header, $m)) {
            json_error('Token autentikasi tidak ditemukan.', 401);
        }
        $payload = JwtService::decode(trim($m[1]));
        if (!$payload || empty($payload['sub'])) {
            json_error('Sesi login tidak valid atau telah kedaluwarsa.', 401);
        }

        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT u.id, u.role_id, r.code AS role_code, r.name AS role_name,
                    u.wilayah_id, u.rw_id, u.rt_id, u.username, u.email, u.phone, u.full_name, u.avatar_url, u.is_active
             FROM users u
             INNER JOIN roles r ON r.id = u.role_id
             WHERE u.id = ? AND u.deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([(int) $payload['sub']]);
        $user = $stmt->fetch();
        if (!$user || (int) $user['is_active'] !== 1) {
            json_error('Akun tidak aktif atau tidak ditemukan.', 401);
        }
        return $user;
    }
}
