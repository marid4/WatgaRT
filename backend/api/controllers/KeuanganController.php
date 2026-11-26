<?php
declare(strict_types=1);

require_once __DIR__ . '/../../services/AuditLogService.php';
require_once __DIR__ . '/../../services/NotificationService.php';
require_once __DIR__ . '/../../services/PdfExportService.php';

final class KeuanganController
{
    public static function kasSummary(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $rtId = (int) ($_GET['rt_id'] ?? $user['rt_id'] ?? 1);

        $stmt = $pdo->prepare('SELECT * FROM kas WHERE rt_id = ?');
        $stmt->execute([$rtId]);
        $kasList = $stmt->fetchAll();

        $sumStmt = $pdo->prepare(
            "SELECT
                SUM(CASE WHEN jenis = 'PEMASUKAN' THEN nominal ELSE 0 END) AS total_pemasukan,
                SUM(CASE WHEN jenis = 'PENGELUARAN' THEN nominal ELSE 0 END) AS total_pengeluaran
             FROM transaksi_kas WHERE rt_id = ? AND deleted_at IS NULL"
        );
        $sumStmt->execute([$rtId]);
        $totals = $sumStmt->fetch() ?: ['total_pemasukan' => 0, 'total_pengeluaran' => 0];

        json_success([
            'kas' => $kasList,
            'total_pemasukan' => (float) ($totals['total_pemasukan'] ?? 0),
            'total_pengeluaran' => (float) ($totals['total_pengeluaran'] ?? 0),
        ]);
    }

    public static function listTransaksiKas(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $rtId = (int) ($_GET['rt_id'] ?? $user['rt_id'] ?? 1);
        $jenis = trim((string) ($_GET['jenis'] ?? ''));

        $sql = 'SELECT * FROM transaksi_kas WHERE rt_id = ? AND deleted_at IS NULL';
        $params = [$rtId];
        if (in_array($jenis, ['PEMASUKAN', 'PENGELUARAN'], true)) {
            $sql .= ' AND jenis = ?';
            $params[] = $jenis;
        }
        $sql .= ' ORDER BY tanggal_transaksi DESC, id DESC LIMIT 100';
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        json_success($stmt->fetchAll());
    }

    public static function createTransaksiKas(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RT']);
        $input = get_json_input();

        $rtId = (int) ($input['rt_id'] ?? $user['rt_id'] ?? 1);
        $jenis = strtoupper(trim((string) ($input['jenis'] ?? '')));
        $nominal = (float) ($input['nominal'] ?? 0);
        $judul = trim((string) ($input['judul'] ?? ''));
        $kategori = trim((string) ($input['kategori'] ?? 'UMUM'));

        if (!in_array($jenis, ['PEMASUKAN', 'PENGELUARAN'], true) || $nominal <= 0 || $judul === '') {
            json_error('Jenis, judul, dan nominal (>0) wajib diisi.', 422);
        }

        $pdo = Database::getConnection();
        $pdo->beginTransaction();
        try {
            $kasStmt = $pdo->prepare('SELECT * FROM kas WHERE rt_id = ? ORDER BY id ASC LIMIT 1 FOR UPDATE');
            $kasStmt->execute([$rtId]);
            $kas = $kasStmt->fetch();
            if (!$kas) {
                throw new RuntimeException('Akun kas RT belum tersedia.');
            }
            $saldo = (float) $kas['saldo_sekarang'];
            $newSaldo = $jenis === 'PEMASUKAN' ? $saldo + $nominal : $saldo - $nominal;

            $buktiPath = handle_secure_upload('bukti', 'kas');
            $pdo->prepare('UPDATE kas SET saldo_sekarang = ? WHERE id = ?')->execute([$newSaldo, $kas['id']]);
            $pdo->prepare(
                'INSERT INTO transaksi_kas (kas_id, rt_id, jenis, kategori, judul, deskripsi, nominal, saldo_setelah, tanggal_transaksi, bukti_url, created_by)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
            )->execute([
                $kas['id'],
                $rtId,
                $jenis,
                $kategori,
                $judul,
                trim((string) ($input['deskripsi'] ?? '')),
                $nominal,
                $newSaldo,
                trim((string) ($input['tanggal_transaksi'] ?? date('Y-m-d'))),
                $buktiPath,
                (int) $user['id'],
            ]);
            $txId = (int) $pdo->lastInsertId();
            $pdo->commit();
            AuditLogService::log((int) $user['id'], $rtId, 'CREATE_KAS_' . $jenis, 'transaksi_kas', $txId);
            json_success(['id' => $txId, 'saldo_sekarang' => $newSaldo], 'Transaksi kas berhasil dicatat.', 201);
        } catch (Throwable $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            json_error($e->getMessage(), 400);
        }
    }

    public static function exportKasCsv(): never
    {
        $user = AuthMiddleware::authenticate();
        $rtId = (int) ($_GET['rt_id'] ?? $user['rt_id'] ?? 1);
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT tanggal_transaksi, jenis, kategori, judul, nominal, saldo_setelah FROM transaksi_kas WHERE rt_id = ? AND deleted_at IS NULL ORDER BY tanggal_transaksi DESC');
        $stmt->execute([$rtId]);

        header('Content-Type: text/csv; charset=utf-8');
        header('Content-Disposition: attachment; filename="Laporan_Kas_RT_' . $rtId . '.csv"');
        $out = fopen('php://output', 'w');
        fputcsv($out, ['Tanggal', 'Jenis', 'Kategori', 'Judul', 'Nominal', 'Saldo Setelah']);
        foreach ($stmt->fetchAll() as $row) {
            fputcsv($out, [$row['tanggal_transaksi'], $row['jenis'], $row['kategori'], $row['judul'], $row['nominal'], $row['saldo_setelah']]);
        }
        fclose($out);
        exit;
    }

    public static function exportKasPdf(): never
    {
        $user = AuthMiddleware::authenticate();
        $rtId = (int) ($_GET['rt_id'] ?? $user['rt_id'] ?? 1);
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT tanggal_transaksi, jenis, kategori, judul, nominal, saldo_setelah FROM transaksi_kas WHERE rt_id = ? AND deleted_at IS NULL ORDER BY tanggal_transaksi DESC LIMIT 40');
        $stmt->execute([$rtId]);

        $lines = ['Laporan Transaksi Kas RT #' . $rtId, ''];
        foreach ($stmt->fetchAll() as $r) {
            $lines[] = sprintf(
                '%s | %s | %s | Rp %s | Saldo: Rp %s',
                $r['tanggal_transaksi'],
                $r['jenis'],
                $r['judul'],
                number_format((float) $r['nominal'], 0, ',', '.'),
                number_format((float) $r['saldo_setelah'], 0, ',', '.')
            );
        }
        header('Content-Type: application/pdf');
        header('Content-Disposition: inline; filename="Laporan_Kas_RT_' . $rtId . '.pdf"');
        echo PdfExportService::generateSimplePdf('LAPORAN KAS RT - wargaRT', $lines);
        exit;
    }

    public static function listIuran(): never
    {
        $user = AuthMiddleware::authenticate();
        $rtId = (int) ($_GET['rt_id'] ?? $user['rt_id'] ?? 1);
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM iuran WHERE rt_id = ? AND deleted_at IS NULL ORDER BY id DESC');
        $stmt->execute([$rtId]);
        json_success($stmt->fetchAll());
    }

    public static function createIuran(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RT']);
        $input = get_json_input();
        $rtId = (int) ($input['rt_id'] ?? $user['rt_id'] ?? 1);
        $nama = trim((string) ($input['nama_iuran'] ?? ''));
        $nominal = (float) ($input['nominal'] ?? 0);
        if ($nama === '' || $nominal <= 0) {
            json_error('Nama iuran dan nominal (>0) wajib diisi.', 422);
        }
        $kode = trim((string) ($input['kode_iuran'] ?? 'IUR-' . strtoupper(bin2hex(random_bytes(3)))));
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'INSERT INTO iuran (rt_id, kode_iuran, nama_iuran, kategori, nominal, periode, jatuh_tempo_hari, deskripsi, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $rtId,
            $kode,
            $nama,
            trim((string) ($input['kategori'] ?? 'BULANAN')),
            $nominal,
            trim((string) ($input['periode'] ?? 'BULANAN')),
            (int) ($input['jatuh_tempo_hari'] ?? 10),
            trim((string) ($input['deskripsi'] ?? '')),
            (int) $user['id'],
        ]);
        json_success(['id' => (int) $pdo->lastInsertId()], 'Jenis iuran berhasil dibuat.', 201);
    }

    public static function generateTagihan(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RT']);
        $input = get_json_input();
        $iuranId = (int) ($input['iuran_id'] ?? 0);
        $bulan = (int) ($input['bulan'] ?? date('n'));
        $tahun = (int) ($input['tahun'] ?? date('Y'));
        if ($iuranId <= 0) {
            json_error('iuran_id wajib diisi.', 422);
        }

        $pdo = Database::getConnection();
        $iStmt = $pdo->prepare('SELECT * FROM iuran WHERE id = ? AND is_active = 1');
        $iStmt->execute([$iuranId]);
        $iuran = $iStmt->fetch();
        if (!$iuran) {
            json_error('Master iuran tidak ditemukan.', 404);
        }

        $kkStmt = $pdo->prepare('SELECT id FROM keluarga WHERE rt_id = ? AND is_active = 1 AND deleted_at IS NULL');
        $kkStmt->execute([$iuran['rt_id']]);
        $created = 0;
        $dueDate = sprintf('%04d-%02d-%02d', $tahun, $bulan, min(28, max(1, (int) $iuran['jatuh_tempo_hari'])));

        foreach ($kkStmt->fetchAll(PDO::FETCH_COLUMN) as $kkId) {
            $nomor = sprintf('INV-RT%02d-%04d%02d-%04d', $iuran['rt_id'], $tahun, $bulan, $kkId * 10 + $iuranId);
            $ins = $pdo->prepare(
                'INSERT IGNORE INTO tagihan (iuran_id, keluarga_id, rt_id, nomor_tagihan, periode_bulan, periode_tahun, nominal, denda, total_tagihan, tanggal_jatuh_tempo, status)
                 VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, "BELUM_LUNAS")'
            );
            $ins->execute([$iuranId, $kkId, $iuran['rt_id'], $nomor, $bulan, $tahun, $iuran['nominal'], $iuran['nominal'], $dueDate]);
            if ($ins->rowCount() > 0) {
                $created++;
            }
        }
        NotificationService::broadcastToRt((int) $iuran['rt_id'], 'TAGIHAN', 'Tagihan Baru: ' . $iuran['nama_iuran'], "Tagihan periode {$bulan}/{$tahun} telah diterbitkan.", 'iuran', $iuranId);
        json_success(['generated_count' => $created], "Berhasil menerbitkan {$created} tagihan baru.");
    }

    public static function listTagihan(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $sql = 'SELECT t.*, i.nama_iuran, i.kategori, k.kepala_keluarga_nama, k.blok_rumah
                FROM tagihan t
                INNER JOIN iuran i ON i.id = t.iuran_id
                INNER JOIN keluarga k ON k.id = t.keluarga_id
                WHERE 1=1';
        $params = [];
        if ($user['role_code'] === 'WARGA') {
            $sql .= ' AND t.keluarga_id IN (SELECT keluarga_id FROM warga WHERE user_id = ?)';
            $params[] = (int) $user['id'];
        } elseif ($user['role_code'] === 'ADMIN_RT') {
            $sql .= ' AND t.rt_id = ?';
            $params[] = (int) $user['rt_id'];
        }
        $sql .= ' ORDER BY t.periode_tahun DESC, t.periode_bulan DESC, t.id DESC';
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        json_success($stmt->fetchAll());
    }
}
