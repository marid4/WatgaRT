<?php
declare(strict_types=1);

require_once __DIR__ . '/../../services/JwtService.php';
require_once __DIR__ . '/../../services/AuditLogService.php';

final class AuthController
{
    public static function login(): never
    {
        $input = get_json_input();
        $identity = trim((string) ($input['username'] ?? $input['email'] ?? ''));
        $password = (string) ($input['password'] ?? '');

        if ($identity === '' || $password === '') {
            json_error('Username/email dan password wajib diisi.', 422);
        }

        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT u.*, r.code AS role_code, r.name AS role_name
             FROM users u
             INNER JOIN roles r ON r.id = u.role_id
             WHERE (u.username = ? OR u.email = ?) AND u.deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$identity, $identity]);
        $user = $stmt->fetch();

        if (!$user || !password_verify($password, $user['password_hash'])) {
            json_error('Username/email atau password salah.', 401);
        }
        if ((int) $user['is_active'] !== 1) {
            json_error('Akun Anda sedang dinonaktifkan oleh admin.', 403);
        }

        $pdo->prepare('UPDATE users SET last_login_at = NOW() WHERE id = ?')->execute([(int) $user['id']]);
        AuditLogService::log((int) $user['id'], $user['rt_id'] ? (int) $user['rt_id'] : null, 'LOGIN', 'users', (int) $user['id']);

        $token = JwtService::encode([
            'sub' => (int) $user['id'],
            'role' => $user['role_code'],
            'rt_id' => $user['rt_id'] ? (int) $user['rt_id'] : null,
            'rw_id' => $user['rw_id'] ? (int) $user['rw_id'] : null,
        ]);

        json_success([
            'token' => $token,
            'token_type' => 'Bearer',
            'expires_in' => JWT_TTL_SECONDS,
            'user' => [
                'id' => (int) $user['id'],
                'username' => $user['username'],
                'email' => $user['email'],
                'phone' => $user['phone'],
                'full_name' => $user['full_name'],
                'role_code' => $user['role_code'],
                'role_name' => $user['role_name'],
                'wilayah_id' => $user['wilayah_id'] ? (int) $user['wilayah_id'] : null,
                'rw_id' => $user['rw_id'] ? (int) $user['rw_id'] : null,
                'rt_id' => $user['rt_id'] ? (int) $user['rt_id'] : null,
                'avatar_url' => $user['avatar_url'],
            ],
        ], 'Login berhasil.');
    }

    public static function register(): never
    {
        $input = get_json_input();
        $username = trim((string) ($input['username'] ?? ''));
        $email = trim((string) ($input['email'] ?? ''));
        $fullName = trim((string) ($input['full_name'] ?? ''));
        $phone = trim((string) ($input['phone'] ?? ''));
        $password = (string) ($input['password'] ?? '');
        $rtId = (int) ($input['rt_id'] ?? 1);

        if ($username === '' || $email === '' || $fullName === '' || strlen($password) < 6) {
            json_error('Data registrasi tidak lengkap (password minimal 6 karakter).', 422);
        }
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            json_error('Format email tidak valid.', 422);
        }

        $pdo = Database::getConnection();
        $dup = $pdo->prepare('SELECT id FROM users WHERE username = ? OR email = ? LIMIT 1');
        $dup->execute([$username, $email]);
        if ($dup->fetchColumn()) {
            json_error('Username atau email sudah terdaftar.', 409);
        }

        $rtStmt = $pdo->prepare('SELECT r.id, r.rw_id, rw.wilayah_id FROM rt r INNER JOIN rw ON rw.id = r.rw_id WHERE r.id = ?');
        $rtStmt->execute([$rtId]);
        $rtRow = $rtStmt->fetch();
        if (!$rtRow) {
            json_error('Data RT tidak ditemukan.', 404);
        }

        $algo = defined('PASSWORD_ARGON2ID') ? PASSWORD_ARGON2ID : PASSWORD_BCRYPT;
        $hash = password_hash($password, $algo);

        $stmt = $pdo->prepare(
            'INSERT INTO users (role_id, wilayah_id, rw_id, rt_id, username, email, phone, full_name, password_hash, is_active)
             VALUES (4, ?, ?, ?, ?, ?, ?, ?, ?, 1)'
        );
        $stmt->execute([(int) $rtRow['wilayah_id'], (int) $rtRow['rw_id'], $rtId, $username, $email, $phone, $fullName, $hash]);
        $userId = (int) $pdo->lastInsertId();

        AuditLogService::log($userId, $rtId, 'REGISTER', 'users', $userId);
        json_success(['user_id' => $userId], 'Registrasi warga berhasil. Silakan login.', 201);
    }

    public static function me(): never
    {
        $user = AuthMiddleware::authenticate();
        json_success($user);
    }

    public static function updateFcmToken(): never
    {
        $user = AuthMiddleware::authenticate();
        $input = get_json_input();
        $token = trim((string) ($input['fcm_token'] ?? ''));
        if ($token === '') {
            json_error('fcm_token wajib diisi.', 422);
        }
        $pdo = Database::getConnection();
        $pdo->prepare('UPDATE users SET fcm_token = ? WHERE id = ?')->execute([$token, (int) $user['id']]);
        json_success(null, 'Token notifikasi diperbarui.');
    }
}
