<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';

final class NotificationService
{
    public static function sendToUser(
        int $userId,
        ?int $rtId,
        string $tipe,
        string $judul,
        string $pesan,
        ?string $refType = null,
        ?int $refId = null
    ): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'INSERT INTO notifikasi (user_id, rt_id, tipe, judul, pesan, reference_type, reference_id)
             VALUES (?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([$userId, $rtId, $tipe, $judul, $pesan, $refType, $refId]);

        $uStmt = $pdo->prepare('SELECT fcm_token FROM users WHERE id = ? AND is_active = 1');
        $uStmt->execute([$userId]);
        $token = $uStmt->fetchColumn();
        if (is_string($token) && $token !== '' && FCM_SERVER_KEY !== '') {
            self::dispatchFcm($token, $judul, $pesan, ['type' => $tipe, 'ref_type' => $refType, 'ref_id' => $refId]);
        }
    }

    public static function broadcastToRt(
        int $rtId,
        string $tipe,
        string $judul,
        string $pesan,
        ?string $refType = null,
        ?int $refId = null
    ): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT id FROM users WHERE rt_id = ? AND is_active = 1 AND deleted_at IS NULL');
        $stmt->execute([$rtId]);
        foreach ($stmt->fetchAll(PDO::FETCH_COLUMN) as $uid) {
            self::sendToUser((int) $uid, $rtId, $tipe, $judul, $pesan, $refType, $refId);
        }
    }

    private static function dispatchFcm(string $fcmToken, string $title, string $body, array $data = []): void
    {
        $payload = [
            'to' => $fcmToken,
            'notification' => ['title' => $title, 'body' => $body, 'sound' => 'default'],
            'data' => $data,
        ];
        $ch = curl_init('https://fcm.googleapis.com/fcm/send');
        if ($ch === false) {
            return;
        }
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 5,
            CURLOPT_HTTPHEADER => [
                'Authorization: key=' . FCM_SERVER_KEY,
                'Content-Type: application/json',
            ],
            CURLOPT_POSTFIELDS => json_encode($payload),
        ]);
        curl_exec($ch);
        curl_close($ch);
    }
}
