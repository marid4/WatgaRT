<?php
declare(strict_types=1);

require_once __DIR__ . '/../../services/EncryptionService.php';
require_once __DIR__ . '/../../services/AuditLogService.php';
require_once __DIR__ . '/../../services/PdfExportService.php';

final class WargaController
{
    public static function listWarga(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $search = trim((string) ($_GET['q'] ?? ''));
        $status = trim((string) ($_GET['status'] ?? ''));

        $sql = 'SELECT w.id, w.keluarga_id, w.rt_id, w.nik_masked, w.nama_lengkap, w.tempat_lahir, w.tanggal_lahir,
                       w.jenis_kelamin, w.alamat_lengkap, w.no_whatsapp, w.status_perkawinan, w.pekerjaan,
                       w.status_domisili, w.foto_profil, k.no_kk_masked
                FROM warga w
                LEFT JOIN keluarga k ON k.id = w.keluarga_id
                WHERE w.deleted_at IS NULL';
        $params = [];

        if ($user['role_code'] === 'WARGA') {
            $sql .= ' AND (w.user_id = ? OR w.keluarga_id IN (SELECT keluarga_id FROM warga WHERE user_id = ?))';
            $params[] = (int) $user['id'];
            $params[] = (int) $user['id'];
        } elseif ($user['role_code'] === 'ADMIN_RT') {
            $sql .= ' AND w.rt_id = ?';
            $params[] = (int) $user['rt_id'];
        } elseif ($user['role_code'] === 'ADMIN_RW') {
            $sql .= ' AND w.rt_id IN (SELECT id FROM rt WHERE rw_id = ?)';
            $params[] = (int) $user['rw_id'];
        }

        if ($search !== '') {
            $sql .= ' AND (w.nama_lengkap LIKE ? OR w.alamat_lengkap LIKE ?)';
            $params[] = "%{$search}%";
            $params[] = "%{$search}%";
        }
        if ($status !== '') {
            $sql .= ' AND w.status_domisili = ?';
            $params[] = $status;
        }
        $sql .= ' ORDER BY w.nama_lengkap ASC LIMIT 100';

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        json_success($stmt->fetchAll());
    }

    public static function detailWarga(): never
    {
        $user = AuthMiddleware::authenticate();
        $id = (int) ($_GET['id'] ?? 0);
        if ($id <= 0) {
            json_error('ID warga tidak valid.', 422);
        }
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM warga WHERE id = ? AND deleted_at IS NULL');
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        if (!$row) {
            json_error('Data warga tidak ditemukan.', 404);
        }
        if (!RbacMiddleware::canAccessRt($user, (int) $row['rt_id'])) {
            json_error('Akses wilayah RT ditolak.', 403);
        }

        $showFullNik = RbacMiddleware::hasPermission($user, 'warga.view_sensitive') || ((int) $row['user_id'] === (int) $user['id']);
        $row['nik_display'] = $showFullNik ? EncryptionService::decrypt($row['nik_encrypted']) : $row['nik_masked'];
        unset($row['nik_encrypted'], $row['nik_hash']);
        json_success($row);
    }

    public static function createWarga(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RT']);
        $input = get_json_input();

        $nik = preg_replace('/\D+/', '', (string) ($input['nik'] ?? '')) ?? '';
        $nama = trim((string) ($input['nama_lengkap'] ?? ''));
        $rtId = (int) ($input['rt_id'] ?? $user['rt_id'] ?? 1);

        if (strlen($nik) !== 16 || $nama === '') {
            json_error('NIK (16 digit) dan nama lengkap wajib diisi.', 422);
        }
        if (!RbacMiddleware::canAccessRt($user, $rtId)) {
            json_error('Akses RT ditolak.', 403);
        }

        $pdo = Database::getConnection();
        $nikHash = EncryptionService::blindHash($nik);
        $dup = $pdo->prepare('SELECT id FROM warga WHERE nik_hash = ? AND deleted_at IS NULL');
        $dup->execute([$nikHash]);
        if ($dup->fetchColumn()) {
            json_error('NIK sudah terdaftar di sistem.', 409);
        }

        $stmt = $pdo->prepare(
            'INSERT INTO warga (keluarga_id, rt_id, nik_encrypted, nik_hash, nik_masked, nama_lengkap, tempat_lahir, tanggal_lahir,
                                jenis_kelamin, alamat_lengkap, no_whatsapp, status_perkawinan, pekerjaan, status_domisili, tanggal_masuk)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            !empty($input['keluarga_id']) ? (int) $input['keluarga_id'] : null,
            $rtId,
            EncryptionService::encrypt($nik),
            $nikHash,
            EncryptionService::maskIdNumber($nik),
            $nama,
            trim((string) ($input['tempat_lahir'] ?? '-')),
            trim((string) ($input['tanggal_lahir'] ?? '1990-01-01')),
            in_array($input['jenis_kelamin'] ?? 'L', ['L', 'P'], true) ? $input['jenis_kelamin'] : 'L',
            trim((string) ($input['alamat_lengkap'] ?? '-')),
            trim((string) ($input['no_whatsapp'] ?? '')),
            trim((string) ($input['status_perkawinan'] ?? 'BELUM_KAWIN')),
            trim((string) ($input['pekerjaan'] ?? '')),
            trim((string) ($input['status_domisili'] ?? 'AKTIF')),
            trim((string) ($input['tanggal_masuk'] ?? date('Y-m-d'))),
        ]);
        $newId = (int) $pdo->lastInsertId();
        AuditLogService::log((int) $user['id'], $rtId, 'CREATE_WARGA', 'warga', $newId);
        json_success(['id' => $newId], 'Data warga berhasil ditambahkan.', 201);
    }

    public static function updateWarga(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RT']);
        $input = get_json_input();
        $id = (int) ($input['id'] ?? 0);
        if ($id <= 0) {
            json_error('ID warga wajib diisi.', 422);
        }

        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM warga WHERE id = ? AND deleted_at IS NULL');
        $stmt->execute([$id]);
        $existing = $stmt->fetch();
        if (!$existing || !RbacMiddleware::canAccessRt($user, (int) $existing['rt_id'])) {
            json_error('Data warga tidak ditemukan atau akses ditolak.', 404);
        }

        $pdo->prepare(
            'UPDATE warga SET nama_lengkap = ?, alamat_lengkap = ?, no_whatsapp = ?, pekerjaan = ?, status_perkawinan = ?, status_domisili = ? WHERE id = ?'
        )->execute([
            trim((string) ($input['nama_lengkap'] ?? $existing['nama_lengkap'])),
            trim((string) ($input['alamat_lengkap'] ?? $existing['alamat_lengkap'])),
            trim((string) ($input['no_whatsapp'] ?? $existing['no_whatsapp'])),
            trim((string) ($input['pekerjaan'] ?? $existing['pekerjaan'])),
            trim((string) ($input['status_perkawinan'] ?? $existing['status_perkawinan'])),
            trim((string) ($input['status_domisili'] ?? $existing['status_domisili'])),
            $id,
        ]);
        AuditLogService::log((int) $user['id'], (int) $existing['rt_id'], 'UPDATE_WARGA', 'warga', $id);
        json_success(null, 'Data warga berhasil diperbarui.');
    }

    public static function listKeluarga(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $sql = 'SELECT k.id, k.rt_id, k.no_kk_masked, k.kepala_keluarga_nama, k.alamat, k.blok_rumah, k.status_hunian,
                       (SELECT COUNT(*) FROM anggota_keluarga ak WHERE ak.keluarga_id = k.id AND ak.is_active = 1) AS jumlah_anggota
                FROM keluarga k WHERE k.deleted_at IS NULL';
        $params = [];
        if ($user['role_code'] === 'ADMIN_RT') {
            $sql .= ' AND k.rt_id = ?';
            $params[] = (int) $user['rt_id'];
        } elseif ($user['role_code'] === 'WARGA') {
            $sql .= ' AND k.id IN (SELECT keluarga_id FROM warga WHERE user_id = ?)';
            $params[] = (int) $user['id'];
        }
        $sql .= ' ORDER BY k.kepala_keluarga_nama ASC';
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        json_success($stmt->fetchAll());
    }

    public static function createKeluarga(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RT']);
        $input = get_json_input();

        $noKk = preg_replace('/\D+/', '', (string) ($input['no_kk'] ?? '')) ?? '';
        $kepala = trim((string) ($input['kepala_keluarga_nama'] ?? ''));
        $rtId = (int) ($input['rt_id'] ?? $user['rt_id'] ?? 1);
        if (strlen($noKk) !== 16 || $kepala === '') {
            json_error('Nomor KK (16 digit) dan nama kepala keluarga wajib diisi.', 422);
        }

        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'INSERT INTO keluarga (rt_id, no_kk_encrypted, no_kk_hash, no_kk_masked, kepala_keluarga_nama, alamat, blok_rumah, status_hunian)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $rtId,
            EncryptionService::encrypt($noKk),
            EncryptionService::blindHash($noKk),
            EncryptionService::maskIdNumber($noKk),
            $kepala,
            trim((string) ($input['alamat'] ?? '-')),
            trim((string) ($input['blok_rumah'] ?? '')),
            trim((string) ($input['status_hunian'] ?? 'TETAP')),
        ]);
        $kkId = (int) $pdo->lastInsertId();
        AuditLogService::log((int) $user['id'], $rtId, 'CREATE_KK', 'keluarga', $kkId);
        json_success(['id' => $kkId], 'Kartu Keluarga berhasil ditambahkan.', 201);
    }

    public static function addAnggotaKeluarga(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RT']);
        $input = get_json_input();
        $kkId = (int) ($input['keluarga_id'] ?? 0);
        $wargaId = (int) ($input['warga_id'] ?? 0);
        $hubungan = trim((string) ($input['hubungan_keluarga'] ?? 'ANAK'));
        if ($kkId <= 0 || $wargaId <= 0) {
            json_error('keluarga_id dan warga_id wajib diisi.', 422);
        }

        $pdo = Database::getConnection();
        $pdo->prepare('UPDATE warga SET keluarga_id = ? WHERE id = ?')->execute([$kkId, $wargaId]);
        $pdo->prepare(
            'INSERT INTO anggota_keluarga (keluarga_id, warga_id, hubungan_keluarga, tanggal_bergabung, is_active)
             VALUES (?, ?, ?, CURDATE(), 1)'
        )->execute([$kkId, $wargaId, $hubungan]);
        json_success(null, 'Anggota keluarga berhasil ditambahkan.');
    }

    public static function printKeluargaPdf(): never
    {
        $user = AuthMiddleware::authenticate();
        $kkId = (int) ($_GET['id'] ?? 0);
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM keluarga WHERE id = ? AND deleted_at IS NULL');
        $stmt->execute([$kkId]);
        $kk = $stmt->fetch();
        if (!$kk) {
            json_error('Kartu keluarga tidak ditemukan.', 404);
        }

        $mStmt = $pdo->prepare(
            'SELECT w.nama_lengkap, w.nik_masked, w.jenis_kelamin, w.tempat_lahir, w.tanggal_lahir, ak.hubungan_keluarga
             FROM anggota_keluarga ak INNER JOIN warga w ON w.id = ak.warga_id
             WHERE ak.keluarga_id = ? AND ak.is_active = 1'
        );
        $mStmt->execute([$kkId]);
        $lines = [
            'No. KK (Terlindung): ' . $kk['no_kk_masked'],
            'Kepala Keluarga: ' . $kk['kepala_keluarga_nama'],
            'Alamat: ' . $kk['alamat'] . ' (Blok ' . ($kk['blok_rumah'] ?? '-') . ')',
            'Status Hunian: ' . $kk['status_hunian'],
            '',
            'Daftar Anggota Keluarga:',
        ];
        foreach ($mStmt->fetchAll() as $idx => $m) {
            $lines[] = sprintf(
                '%d. %s | %s | %s | %s, %s | %s',
                $idx + 1,
                $m['nama_lengkap'],
                $m['nik_masked'],
                $m['jenis_kelamin'],
                $m['tempat_lahir'],
                $m['tanggal_lahir'],
                $m['hubungan_keluarga']
            );
        }

        $pdf = PdfExportService::generateSimplePdf('DATA KARTU KELUARGA INTERNAL - wargaRT', $lines);
        header('Content-Type: application/pdf');
        header('Content-Disposition: inline; filename="KK_' . $kkId . '.pdf"');
        echo $pdf;
        exit;
    }
}
