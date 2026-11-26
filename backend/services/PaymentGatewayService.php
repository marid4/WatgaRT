<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/NotificationService.php';
require_once __DIR__ . '/AuditLogService.php';

final class PaymentGatewayService
{
    public static function createGatewayCharge(array $pembayaran): array
    {
        $orderId = $pembayaran['order_id'];
        $amount = (int) round((float) $pembayaran['total_bayar']);
        $method = $pembayaran['metode_pembayaran'];

        if ($method === 'QRIS') {
            $qrisPayload = sprintf(
                '00020101021226580014ID.CO.WARGART0115%s0210%s520483995303360540%d5802ID5907WARGART6011TASIKMALAYA6304ABCD',
                str_pad((string) $pembayaran['rt_id'], 6, '0', STR_PAD_LEFT),
                substr($orderId, -10),
                $amount
            );
            return [
                'gateway_ref_id' => 'GW-QRIS-' . strtoupper(bin2hex(random_bytes(5))),
                'qris_payload' => $qrisPayload,
                'va_number' => null,
            ];
        }

        if ($method === 'VIRTUAL_ACCOUNT') {
            $bankPrefix = match (strtoupper((string) ($pembayaran['channel_pembayaran'] ?? 'BCA'))) {
                'BRI' => '26215',
                'MANDIRI' => '88019',
                'BNI' => '80081',
                default => '39010',
            };
            $vaNumber = $bankPrefix . str_pad((string) $pembayaran['tagihan_id'], 6, '0', STR_PAD_LEFT) . random_int(1000, 9999);
            return [
                'gateway_ref_id' => 'GW-VA-' . strtoupper(bin2hex(random_bytes(5))),
                'qris_payload' => null,
                'va_number' => $vaNumber,
            ];
        }

        return [
            'gateway_ref_id' => null,
            'qris_payload' => null,
            'va_number' => null,
        ];
    }

    public static function verifyWebhookSignature(array $payload, string $signatureHeader = ''): bool
    {
        if (PAYMENT_SERVER_KEY === '') {
            // Sandbox / dev fallback when server key is not configured
            return isset($payload['order_id'], $payload['transaction_status']);
        }
        $orderId = (string) ($payload['order_id'] ?? '');
        $statusCode = (string) ($payload['status_code'] ?? '200');
        $grossAmount = (string) ($payload['gross_amount'] ?? '0');
        $expected = hash('sha512', $orderId . $statusCode . $grossAmount . PAYMENT_SERVER_KEY);
        $provided = (string) ($payload['signature_key'] ?? $signatureHeader);
        return $provided !== '' && hash_equals($expected, $provided);
    }

    public static function processSettlement(int $pembayaranId, ?int $verifierId = null, string $source = 'GATEWAY'): void
    {
        $pdo = Database::getConnection();
        $pdo->beginTransaction();
        try {
            $stmt = $pdo->prepare('SELECT * FROM pembayaran WHERE id = ? FOR UPDATE');
            $stmt->execute([$pembayaranId]);
            $pay = $stmt->fetch();
            if (!$pay) {
                throw new RuntimeException('Transaksi pembayaran tidak ditemukan.');
            }
            if ($pay['status'] === 'BERHASIL') {
                $pdo->commit();
                return; // Idempotent: prevent double processing
            }

            $uPay = $pdo->prepare(
                'UPDATE pembayaran SET status = "BERHASIL", verified_by = ?, verified_at = NOW(), paid_at = NOW() WHERE id = ?'
            );
            $uPay->execute([$verifierId, $pembayaranId]);

            $uTag = $pdo->prepare('UPDATE tagihan SET status = "LUNAS" WHERE id = ?');
            $uTag->execute([$pay['tagihan_id']]);

            // Auto-credit RT cash balance
            $kasStmt = $pdo->prepare('SELECT * FROM kas WHERE rt_id = ? ORDER BY id ASC LIMIT 1 FOR UPDATE');
            $kasStmt->execute([$pay['rt_id']]);
            $kas = $kasStmt->fetch();
            if ($kas) {
                $newSaldo = (float) $kas['saldo_sekarang'] + (float) $pay['nominal_bayar'];
                $pdo->prepare('UPDATE kas SET saldo_sekarang = ? WHERE id = ?')->execute([$newSaldo, $kas['id']]);
                $pdo->prepare(
                    'INSERT INTO transaksi_kas (kas_id, rt_id, pembayaran_id, jenis, kategori, judul, deskripsi, nominal, saldo_setelah, tanggal_transaksi, created_by)
                     VALUES (?, ?, ?, "PEMASUKAN", "IURAN_WARGA", ?, ?, ?, ?, CURDATE(), ?)'
                )->execute([
                    $kas['id'],
                    $pay['rt_id'],
                    $pembayaranId,
                    'Pembayaran Iuran #' . $pay['order_id'],
                    'Pembayaran terverifikasi via ' . $source . ' (' . $pay['metode_pembayaran'] . ')',
                    $pay['nominal_bayar'],
                    $newSaldo,
                    $verifierId ?? $pay['user_id'],
                ]);
            }

            $pdo->commit();

            NotificationService::sendToUser(
                (int) $pay['user_id'],
                (int) $pay['rt_id'],
                'PEMBAYARAN',
                'Pembayaran Berhasil',
                'Pembayaran tagihan #' . $pay['order_id'] . ' sebesar Rp ' . number_format((float) $pay['total_bayar'], 0, ',', '.') . ' telah lunas.',
                'pembayaran',
                $pembayaranId
            );
            AuditLogService::log($verifierId ?? (int) $pay['user_id'], (int) $pay['rt_id'], 'PAYMENT_SETTLED', 'pembayaran', $pembayaranId);
        } catch (Throwable $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            throw $e;
        }
    }
}
