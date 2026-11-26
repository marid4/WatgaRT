<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';

final class RbacMiddleware
{
    public static function requireRole(array $user, array $allowedRoles): void
    {
        if (!in_array($user['role_code'], $allowedRoles, true)) {
            json_error('Akses ditolak: kewenangan peran tidak mencukupi.', 403);
        }
    }

    public static function hasPermission(array $user, string $permissionCode): bool
    {
        if ($user['role_code'] === 'SUPER_ADMIN') {
            return true;
        }
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT up.granted
             FROM user_permissions up
             INNER JOIN permissions p ON p.id = up.permission_id
             WHERE up.user_id = ? AND p.code = ? LIMIT 1'
        );
        $stmt->execute([(int) $user['id'], $permissionCode]);
        $override = $stmt->fetchColumn();
        if ($override !== false) {
            return (int) $override === 1;
        }

        $defaults = [
            'ADMIN_RW' => ['warga.view', 'laporan.export', 'pengumuman.manage'],
            'ADMIN_RT' => [
                'warga.view', 'warga.manage', 'warga.view_sensitive',
                'iuran.manage', 'pembayaran.verify', 'kas.manage',
                'surat.approve', 'pengumuman.manage', 'pengaduan.manage',
                'keamanan.manage', 'kegiatan.manage', 'laporan.export'
            ],
            'WARGA' => [],
        ];
        return in_array($permissionCode, $defaults[$user['role_code']] ?? [], true);
    }

    public static function canAccessRt(array $user, int $targetRtId): bool
    {
        if ($user['role_code'] === 'SUPER_ADMIN') {
            return true;
        }
        if ($user['role_code'] === 'ADMIN_RW') {
            $pdo = Database::getConnection();
            $stmt = $pdo->prepare('SELECT id FROM rt WHERE id = ? AND rw_id = ?');
            $stmt->execute([$targetRtId, (int) $user['rw_id']]);
            return (bool) $stmt->fetchColumn();
        }
        return (int) $user['rt_id'] === $targetRtId;
    }
}
