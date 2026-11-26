<?php
declare(strict_types=1);

require_once __DIR__ . '/../../services/NotificationService.php';
require_once __DIR__ . '/../../services/PdfExportService.php';
require_once __DIR__ . '/../../services/EncryptionService.php';

final class LayananController
{
    public static function listTemplates(): never
    {
        AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        json_success($pdo->query('SELECT * FROM template_surat WHERE is_active = 1')->fetchAll());
    }

    public static function listSurat(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $sql = 'SELECT ps.*, w.nama_lengkap, ts.nama_surat
                FROM pengajuan_surat ps
                INNER JOIN warga w ON w.id = ps.warga_id
                INNER JOIN template_surat ts ON ts.id = ps.template_id
                WHERE 1=1';
        $params = [];
        if ($user['role_code'] === 'WARGA') {
            $sql .= ' AND ps.user_id = ?';
            $params[] = (int) $user['id'];
        } elseif ($user['role_code'] === 'ADMIN_RT') {
            $sql .= ' AND ps.rt_id = ?';
            $params[] = (int) $user['rt_id'];
        }
        $sql .= ' ORDER BY ps.created_at DESC';
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        json_success($stmt->fetchAll());
    }

    public static function createSurat(): never
    {
        $user = AuthMiddleware::authenticate();
        $input = get_json_input();
        $templateId = (int) ($input['template_id'] ?? 1);
        $keperluan = trim((string) ($input['keperluan'] ?? ''));
        if ($keperluan === '') {
            json_error('Keperluan surat wajib diisi.', 422);
        }

        $pdo = Database::getConnection();
        $wStmt = $pdo->prepare('SELECT id, rt_id FROM warga WHERE user_id = ? AND deleted_at IS NULL LIMIT 1');
        $wStmt->execute([(int) $user['id']]);
        $warga = $wStmt->fetch();
        $wargaId = $warga ? (int) $warga['id'] : (int) ($input['warga_id'] ?? 1);
        $rtId = $warga ? (int) $warga['rt_id'] : (int) ($user['rt_id'] ?? 1);

        $tStmt = $pdo->prepare('SELECT nama_surat FROM template_surat WHERE id = ?');
        $tStmt->execute([$templateId]);
        $namaSurat = $tStmt->fetchColumn() ?: 'Surat Pengantar RT/RW';

        $ins = $pdo->prepare(
            'INSERT INTO pengajuan_surat (template_id, warga_id, user_id, rt_id, jenis_surat, keperluan, status)
             VALUES (?, ?, ?, ?, ?, ?, "DIAJUKAN")'
        );
        $ins->execute([$templateId, $wargaId, (int) $user['id'], $rtId, $namaSurat, $keperluan]);
        json_success(['id' => (int) $pdo->lastInsertId()], 'Pengajuan surat berhasil dikirim.', 201);
    }

    public static function approveSurat(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RT', 'ADMIN_RW']);
        $input = get_json_input();
        $id = (int) ($input['id'] ?? 0);
        $approve = (bool) ($input['approve'] ?? true);

        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT ps.*, rt.nomor_rt, rw.nomor_rw FROM pengajuan_surat ps INNER JOIN rt ON rt.id = ps.rt_id INNER JOIN rw ON rw.id = rt.rw_id WHERE ps.id = ?');
        $stmt->execute([$id]);
        $surat = $stmt->fetch();
        if (!$surat) {
            json_error('Pengajuan surat tidak ditemukan.', 404);
        }

        if ($approve) {
            $seq = str_pad((string) $id, 3, '0', STR_PAD_LEFT);
            $nomor = sprintf('%s/SRT-RT%s/RW%s/%s/%s', $seq, $surat['nomor_rt'], $surat['nomor_rw'], date('m'), date('Y'));
            $pdo->prepare(
                'UPDATE pengajuan_surat SET status = "DISETUJUI", nomor_surat = ?, disetujui_oleh = ?, tanggal_persetujuan = NOW(),
                 pejabat_penandatangan = ?, jabatan_penandatangan = ? WHERE id = ?'
            )->execute([$nomor, (int) $user['id'], $user['full_name'], $user['role_name'], $id]);
            NotificationService::sendToUser((int) $surat['user_id'], (int) $surat['rt_id'], 'SURAT', 'Surat Disetujui', "Surat Anda ({$nomor}) telah disetujui dan siap diunduh.", 'surat', $id);
            json_success(['nomor_surat' => $nomor], 'Surat disetujui.');
        } else {
            $pdo->prepare('UPDATE pengajuan_surat SET status = "DITOLAK", catatan_admin = ? WHERE id = ?')
                ->execute([trim((string) ($input['catatan'] ?? 'Ditolak')), $id]);
            json_success(null, 'Pengajuan surat ditolak.');
        }
    }

    public static function downloadSuratPdf(): never
    {
        $user = AuthMiddleware::authenticate();
        $id = (int) ($_GET['id'] ?? 0);
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT ps.*, w.nama_lengkap, w.nik_masked, w.tempat_lahir, w.tanggal_lahir, w.alamat_lengkap, rt.nomor_rt, rw.nomor_rw
             FROM pengajuan_surat ps
             INNER JOIN warga w ON w.id = ps.warga_id
             INNER JOIN rt ON rt.id = ps.rt_id
             INNER JOIN rw ON rw.id = rt.rw_id
             WHERE ps.id = ?'
        );
        $stmt->execute([$id]);
        $s = $stmt->fetch();
        if (!$s) {
            json_error('Surat tidak ditemukan.', 404);
        }

        $lines = [
            'Nomor Surat: ' . ($s['nomor_surat'] ?? 'DRAFT-' . $s['id']),
            'Jenis: ' . $s['jenis_surat'],
            'Wilayah: RT ' . $s['nomor_rt'] . ' / RW ' . $s['nomor_rw'],
            '',
            'Yang bertanda tangan di bawah ini menerangkan bahwa:',
            'Nama Lengkap: ' . $s['nama_lengkap'],
            'NIK: ' . $s['nik_masked'],
            'Tempat/Tgl Lahir: ' . $s['tempat_lahir'] . ', ' . $s['tanggal_lahir'],
            'Alamat: ' . $s['alamat_lengkap'],
            'Keperluan: ' . $s['keperluan'],
            '',
            'Status: ' . $s['status'],
            'Pejabat Berwenang: ' . ($s['pejabat_penandatangan'] ?? 'Menunggu Persetujuan'),
        ];
        header('Content-Type: application/pdf');
        header('Content-Disposition: inline; filename="Surat_' . $id . '.pdf"');
        echo PdfExportService::generateSimplePdf(strtoupper($s['jenis_surat']), $lines);
        exit;
    }

    public static function listPengumuman(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT p.*, u.full_name AS author_name FROM pengumuman p LEFT JOIN users u ON u.id = p.created_by
             WHERE p.deleted_at IS NULL AND (p.rt_id IS NULL OR p.rt_id = ?) ORDER BY p.is_pinned DESC, p.published_at DESC LIMIT 50'
        );
        $stmt->execute([(int) ($user['rt_id'] ?? 1)]);
        json_success($stmt->fetchAll());
    }

    public static function createPengumuman(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RW', 'ADMIN_RT']);
        $input = get_json_input();
        $judul = trim((string) ($input['judul'] ?? ''));
        $isi = trim((string) ($input['isi'] ?? ''));
        if ($judul === '' || $isi === '') {
            json_error('Judul dan isi pengumuman wajib diisi.', 422);
        }
        $rtId = (int) ($input['rt_id'] ?? $user['rt_id'] ?? 1);
        $pdo = Database::getConnection();
        $pdo->prepare(
            'INSERT INTO pengumuman (wilayah_id, rw_id, rt_id, scope, kategori, judul, isi, is_pinned, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
        )->execute([
            $user['wilayah_id'] ?? 1,
            $user['rw_id'] ?? 1,
            $rtId,
            trim((string) ($input['scope'] ?? 'RT')),
            trim((string) ($input['kategori'] ?? 'UMUM')),
            $judul,
            $isi,
            !empty($input['is_pinned']) ? 1 : 0,
            (int) $user['id'],
        ]);
        $pid = (int) $pdo->lastInsertId();
        NotificationService::broadcastToRt($rtId, 'PENGUMUMAN', $judul, mb_substr($isi, 0, 120), 'pengumuman', $pid);
        json_success(['id' => $pid], 'Pengumuman dipublikasikan.', 201);
    }

    public static function listPengaduan(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $sql = 'SELECT p.*, u.full_name AS pelapor_nama FROM pengaduan p INNER JOIN users u ON u.id = p.user_id WHERE 1=1';
        $params = [];
        if ($user['role_code'] === 'WARGA') {
            $sql .= ' AND p.user_id = ?';
            $params[] = (int) $user['id'];
        } elseif ($user['role_code'] === 'ADMIN_RT') {
            $sql .= ' AND p.rt_id = ?';
            $params[] = (int) $user['rt_id'];
        }
        $sql .= ' ORDER BY p.created_at DESC';
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        json_success($stmt->fetchAll());
    }

    public static function createPengaduan(): never
    {
        $user = AuthMiddleware::authenticate();
        $input = get_json_input();
        $judul = trim((string) ($input['judul'] ?? ''));
        $deskripsi = trim((string) ($input['deskripsi'] ?? ''));
        if ($judul === '' || $deskripsi === '') {
            json_error('Judul dan deskripsi pengaduan wajib diisi.', 422);
        }
        $fotoPath = handle_secure_upload('foto', 'pengaduan');
        $tiket = 'ADU-' . date('Ymd') . '-' . strtoupper(bin2hex(random_bytes(2)));
        $pdo = Database::getConnection();
        $pdo->prepare(
            'INSERT INTO pengaduan (user_id, rt_id, kode_tiket, kategori, judul, deskripsi, lokasi_kejadian, foto_url, prioritas, status)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, "BARU")'
        )->execute([
            (int) $user['id'],
            (int) ($user['rt_id'] ?? 1),
            $tiket,
            trim((string) ($input['kategori'] ?? 'FASILITAS')),
            $judul,
            $deskripsi,
            trim((string) ($input['lokasi_kejadian'] ?? '')),
            $fotoPath,
            trim((string) ($input['prioritas'] ?? 'SEDANG')),
        ]);
        json_success(['kode_tiket' => $tiket], 'Pengaduan berhasil dikirim.', 201);
    }

    public static function respondPengaduan(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RT']);
        $input = get_json_input();
        $id = (int) ($input['id'] ?? 0);
        $status = trim((string) ($input['status'] ?? 'DIPROSES'));
        $tanggapan = trim((string) ($input['tanggapan_admin'] ?? ''));
        $pdo = Database::getConnection();
        $pdo->prepare('UPDATE pengaduan SET status = ?, tanggapan_admin = ?, ditanggapi_oleh = ?, ditanggapi_at = NOW() WHERE id = ?')
            ->execute([$status, $tanggapan, (int) $user['id'], $id]);
        json_success(null, 'Tanggapan pengaduan disimpan.');
    }
}
