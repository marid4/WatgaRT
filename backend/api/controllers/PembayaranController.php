<?php
declare(strict_types=1);

require_once __DIR__ . '/../../services/PaymentGatewayService.php';
require_once __DIR__ . '/../../services/PdfExportService.php';

final class PembayaranController
{
    public static function listPembayaran(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $sql = 'SELECT p.*, t.nomor_tagihan, i.nama_iuran, u.full_name AS pembayar_nama
                FROM pembayaran p
                INNER JOIN tagihan t ON t.id = p.tagihan_id
                INNER JOIN iuran i ON i.id = t.iuran_id
                INNER JOIN users u ON u.id = p.user_id
                WHERE 1=1';
        $params = [];
        if ($user['role_code'] === 'WARGA') {
            $sql .= ' AND p.user_id = ?';
            $params[] = (int) $user['id'];
        } elseif ($user['role_code'] === 'ADMIN_RT') {
            $sql .= ' AND p.rt_id = ?';
            $params[] = (int) $user['rt_id'];
        }
        $sql .= ' ORDER BY p.created_at DESC LIMIT 100';
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        json_success($stmt->fetchAll());
    }

    public static function createPembayaran(): never
    {
        $user = AuthMiddleware::authenticate();
        $input = get_json_input();
        $tagihanId = (int) ($input['tagihan_id'] ?? 0);
        $metode = strtoupper(trim((string) ($input['metode_pembayaran'] ?? 'QRIS')));
        $channel = trim((string) ($input['channel_pembayaran'] ?? 'QRIS'));
        $idempotencyKey = trim((string) ($input['idempotency_key'] ?? ''));

        if ($tagihanId <= 0 || !in_array($metode, ['TRANSFER_MANUAL', 'QRIS', 'VIRTUAL_ACCOUNT', 'TUNAI'], true)) {
            json_error('Tagihan dan metode pembayaran tidak valid.', 422);
        }
        if ($idempotencyKey === '') {
            $idempotencyKey = hash('sha256', $user['id'] . '|' . $tagihanId . '|' . date('Y-m-d-H'));
        }

        $pdo = Database::getConnection();
        // Anti-duplication check
        $dupStmt = $pdo->prepare('SELECT * FROM pembayaran WHERE idempotency_key = ? LIMIT 1');
        $dupStmt->execute([$idempotencyKey]);
        if ($existing = $dupStmt->fetch()) {
            json_success($existing, 'Transaksi sudah dibuat sebelumnya (anti-duplikasi).');
        }

        $tStmt = $pdo->prepare('SELECT * FROM tagihan WHERE id = ? LIMIT 1');
        $tStmt->execute([$tagihanId]);
        $tagihan = $tStmt->fetch();
        if (!$tagihan) {
            json_error('Tagihan tidak ditemukan.', 404);
        }
        if ($tagihan['status'] === 'LUNAS') {
            json_error('Tagihan ini sudah lunas.', 409);
        }

        $orderId = 'WRT-' . date('YmdHis') . '-' . strtoupper(bin2hex(random_bytes(3)));
        $nominal = (float) $tagihan['total_tagihan'];
        $biayaAdmin = $metode === 'VIRTUAL_ACCOUNT' ? 2500.00 : 0.00;
        $total = $nominal + $biayaAdmin;

        $charge = PaymentGatewayService::createGatewayCharge([
            'order_id' => $orderId,
            'rt_id' => $tagihan['rt_id'],
            'tagihan_id' => $tagihanId,
            'total_bayar' => $total,
            'metode_pembayaran' => $metode,
            'channel_pembayaran' => $channel,
        ]);

        $status = $metode === 'TRANSFER_MANUAL' ? 'MENUNGGU_VERIFIKASI' : 'PENDING';
        $ins = $pdo->prepare(
            'INSERT INTO pembayaran (tagihan_id, user_id, rt_id, order_id, idempotency_key, metode_pembayaran, channel_pembayaran,
                                     nominal_bayar, biaya_admin, total_bayar, gateway_ref_id, va_number, qris_payload, status)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $ins->execute([
            $tagihanId,
            (int) $user['id'],
            (int) $tagihan['rt_id'],
            $orderId,
            $idempotencyKey,
            $metode,
            $channel,
            $nominal,
            $biayaAdmin,
            $total,
            $charge['gateway_ref_id'],
            $charge['va_number'],
            $charge['qris_payload'],
            $status,
        ]);
        $payId = (int) $pdo->lastInsertId();
        $rowStmt = $pdo->prepare('SELECT * FROM pembayaran WHERE id = ?');
        $rowStmt->execute([$payId]);
        json_success($rowStmt->fetch(), 'Instruksi pembayaran berhasil dibuat.', 201);
    }

    public static function uploadBukti(): never
    {
        $user = AuthMiddleware::authenticate();
        $pembayaranId = (int) ($_POST['pembayaran_id'] ?? 0);
        if ($pembayaranId <= 0) {
            json_error('pembayaran_id wajib diisi.', 422);
        }
        $path = handle_secure_upload('bukti', 'bukti_bayar');
        if (!$path) {
            json_error('File bukti transfer wajib diunggah.', 422);
        }
        $pdo = Database::getConnection();
        $pdo->prepare('UPDATE pembayaran SET bukti_transfer_url = ?, status = "MENUNGGU_VERIFIKASI" WHERE id = ? AND user_id = ?')
            ->execute([$path, $pembayaranId, (int) $user['id']]);
        json_success(['bukti_transfer_url' => $path], 'Bukti transfer diunggah, menunggu verifikasi admin.');
    }

    public static function verifyManual(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RT']);
        $input = get_json_input();
        $pembayaranId = (int) ($input['pembayaran_id'] ?? 0);
        $approve = (bool) ($input['approve'] ?? true);

        if ($pembayaranId <= 0) {
            json_error('pembayaran_id wajib diisi.', 422);
        }
        if ($approve) {
            PaymentGatewayService::processSettlement($pembayaranId, (int) $user['id'], 'ADMIN_MANUAL');
            json_success(null, 'Pembayaran diverifikasi & kas RT otomatis bertambah.');
        } else {
            $pdo = Database::getConnection();
            $pdo->prepare('UPDATE pembayaran SET status = "DITOLAK", verified_by = ?, verified_at = NOW(), catatan_admin = ? WHERE id = ?')
                ->execute([(int) $user['id'], trim((string) ($input['catatan'] ?? 'Bukti tidak valid')), $pembayaranId]);
            json_success(null, 'Pembayaran ditolak.');
        }
    }

    public static function webhook(): never
    {
        $raw = file_get_contents('php://input') ?: '{}';
        $payload = json_decode($raw, true);
        if (!is_array($payload) || empty($payload['order_id'])) {
            json_error('Payload webhook tidak valid.', 400);
        }

        $isValid = PaymentGatewayService::verifyWebhookSignature($payload, $_SERVER['HTTP_X_SIGNATURE'] ?? '');
        $pdo = Database::getConnection();
        $pStmt = $pdo->prepare('SELECT id FROM pembayaran WHERE order_id = ? LIMIT 1');
        $pStmt->execute([$payload['order_id']]);
        $pembayaranId = $pStmt->fetchColumn();

        $pdo->prepare(
            'INSERT INTO payment_callbacks (pembayaran_id, order_id, gateway_name, transaction_status, signature_valid, raw_payload, processed)
             VALUES (?, ?, ?, ?, ?, ?, 0)'
        )->execute([
            $pembayaranId ?: null,
            $payload['order_id'],
            PAYMENT_GATEWAY,
            (string) ($payload['transaction_status'] ?? 'unknown'),
            $isValid ? 1 : 0,
            $raw,
        ]);
        $cbId = (int) $pdo->lastInsertId();

        if (!$isValid) {
            json_error('Signature webhook tidak valid.', 403);
        }
        if ($pembayaranId && in_array($payload['transaction_status'], ['settlement', 'capture', 'PAID', 'SUCCESS'], true)) {
            PaymentGatewayService::processSettlement((int) $pembayaranId, null, 'PAYMENT_GATEWAY');
            $pdo->prepare('UPDATE payment_callbacks SET processed = 1, processed_at = NOW() WHERE id = ?')->execute([$cbId]);
        }
        json_success(['received' => true]);
    }

    public static function receiptPdf(): never
    {
        $user = AuthMiddleware::authenticate();
        $id = (int) ($_GET['id'] ?? 0);
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT p.*, t.nomor_tagihan, i.nama_iuran, u.full_name
             FROM pembayaran p
             INNER JOIN tagihan t ON t.id = p.tagihan_id
             INNER JOIN iuran i ON i.id = t.iuran_id
             INNER JOIN users u ON u.id = p.user_id
             WHERE p.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        if (!$row) {
            json_error('Bukti pembayaran tidak ditemukan.', 404);
        }
        $lines = [
            'No. Order: ' . $row['order_id'],
            'No. Tagihan: ' . $row['nomor_tagihan'],
            'Nama Warga: ' . $row['full_name'],
            'Jenis Iuran: ' . $row['nama_iuran'],
            'Metode: ' . $row['metode_pembayaran'] . ' (' . ($row['channel_pembayaran'] ?? '-') . ')',
            'Total Dibayar: Rp ' . number_format((float) $row['total_bayar'], 0, ',', '.'),
            'Status: ' . $row['status'],
            'Tanggal Bayar: ' . ($row['paid_at'] ?? $row['created_at']),
        ];
        header('Content-Type: application/pdf');
        header('Content-Disposition: inline; filename="Kwitansi_' . $row['order_id'] . '.pdf"');
        echo PdfExportService::generateSimplePdf('BUKTI PEMBAYARAN IURAN - wargaRT', $lines);
        exit;
    }
}
