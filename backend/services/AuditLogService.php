<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';

final class AuditLogService
{
    public static function log(
        ?int $userId,
        ?int $rtId,
        string $action,
        string $entityType,
        ?int $entityId = null,
        ?array $oldValues = null,
        ?array $newValues = null
    ): void {
        try {
            $pdo = Database::getConnection();
            $stmt = $pdo->prepare(
                'INSERT INTO audit_logs (user_id, rt_id, action, entity_type, entity_id, old_values, new_values, ip_address, user_agent)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
            );
            $stmt->execute([
                $userId,
                $rtId,
                $action,
                $entityType,
                $entityId,
                $oldValues !== null ? json_encode($oldValues, JSON_UNESCAPED_UNICODE) : null,
                $newValues !== null ? json_encode($newValues, JSON_UNESCAPED_UNICODE) : null,
                $_SERVER['REMOTE_ADDR'] ?? null,
                substr($_SERVER['HTTP_USER_AGENT'] ?? '', 0, 250),
            ]);
        } catch (Throwable $e) {
            // Audit log failures should not crash core requests
        }
    }
}
