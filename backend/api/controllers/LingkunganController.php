<?php
declare(strict_types=1);

require_once __DIR__ . '/../../services/NotificationService.php';

final class LingkunganController
{
    public static function triggerSos(): never
    {
        $user = AuthMiddleware::authenticate();
        $input = get_json_input();
        $rtId = (int) ($user['rt_id'] ?? 1);
        $pdo = Database::getConnection();
        $pdo->prepare(
            'INSERT INTO laporan_keamanan (rt_id, user_id, jenis, judul, deskripsi, lokasi, latitude, longitude, status)
             VALUES (?, ?, "SOS_DARURAT", ?, ?, ?, ?, ?, "AKTIF")'
        )->execute([
            $rtId,
            (int) $user['id'],
            'SINYAL DARURAT SOS - ' . $user['full_name'],
            trim((string) ($input['deskripsi'] ?? 'Warga menekan tombol darurat SOS!')),
            trim((string) ($input['lokasi'] ?? 'Lokasi lingkungan RT')),
            $input['latitude'] ?? null,
            $input['longitude'] ?? null,
        ]);
        $sosId = (int) $pdo->lastInsertId();
        NotificationService::broadcastToRt($rtId, 'DARURAT', 'DARURAT SOS!', $user['full_name'] . ' membutuhkan bantuan darurat segera!', 'keamanan', $sosId);
        json_success(['id' => $sosId], 'Sinyal darurat SOS dikirim ke pengurus & keamanan.', 201);
    }

    public static function emergencyContacts(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT rt.nomor_rt, rt.nama_ketua AS ketua_rt, rt.no_hp_ketua AS hp_ketua_rt, rt.no_hp_keamanan AS hp_keamanan,
                    rw.nomor_rw, rw.nama_ketua AS ketua_rw, rw.no_hp_ketua AS hp_ketua_rw
             FROM rt INNER JOIN rw ON rw.id = rt.rw_id WHERE rt.id = ?'
        );
        $stmt->execute([(int) ($user['rt_id'] ?? 1)]);
        json_success($stmt->fetch());
    }

    public static function listKeamanan(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT lk.*, u.full_name AS pelapor FROM laporan_keamanan lk INNER JOIN users u ON u.id = lk.user_id
             WHERE lk.rt_id = ? ORDER BY lk.created_at DESC LIMIT 50'
        );
        $stmt->execute([(int) ($user['rt_id'] ?? 1)]);
        json_success($stmt->fetchAll());
    }

    public static function listTamu(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM tamu WHERE rt_id = ? ORDER BY waktu_masuk DESC LIMIT 50');
        $stmt->execute([(int) ($user['rt_id'] ?? 1)]);
        json_success($stmt->fetchAll());
    }

    public static function checkinTamu(): never
    {
        $user = AuthMiddleware::authenticate();
        $input = get_json_input();
        $nama = trim((string) ($input['nama_tamu'] ?? ''));
        $tujuan = trim((string) ($input['tujuan_alamat'] ?? ''));
        if ($nama === '' || $tujuan === '') {
            json_error('Nama tamu dan alamat tujuan wajib diisi.', 422);
        }
        $pdo = Database::getConnection();
        $pdo->prepare(
            'INSERT INTO tamu (rt_id, nama_tamu, no_hp, asal_instansi, tujuan_alamat, keperluan, nomor_kendaraan, status, dicatat_oleh)
             VALUES (?, ?, ?, ?, ?, ?, ?, "MASUK", ?)'
        )->execute([
            (int) ($user['rt_id'] ?? 1),
            $nama,
            trim((string) ($input['no_hp'] ?? '')),
            trim((string) ($input['asal_instansi'] ?? '')),
            $tujuan,
            trim((string) ($input['keperluan'] ?? 'Bertamu')),
            trim((string) ($input['nomor_kendaraan'] ?? '')),
            (int) $user['id'],
        ]);
        json_success(['id' => (int) $pdo->lastInsertId()], 'Tamu berhasil dicatat masuk.', 201);
    }

    public static function checkoutTamu(): never
    {
        $user = AuthMiddleware::authenticate();
        $input = get_json_input();
        $id = (int) ($input['id'] ?? 0);
        $pdo = Database::getConnection();
        $pdo->prepare('UPDATE tamu SET status = "KELUAR", waktu_keluar = NOW() WHERE id = ?')->execute([$id]);
        json_success(null, 'Tamu tercatat keluar.');
    }

    public static function listKegiatan(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT k.*, (SELECT COUNT(*) FROM peserta_kegiatan pk WHERE pk.kegiatan_id = k.id) AS total_peserta,
                    EXISTS(SELECT 1 FROM peserta_kegiatan pk2 WHERE pk2.kegiatan_id = k.id AND pk2.user_id = ?) AS sudah_daftar
             FROM kegiatan k WHERE k.rt_id IS NULL OR k.rt_id = ? ORDER BY k.tanggal_mulai ASC'
        );
        $stmt->execute([(int) $user['id'], (int) ($user['rt_id'] ?? 1)]);
        json_success($stmt->fetchAll());
    }

    public static function createKegiatan(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RW', 'ADMIN_RT']);
        $input = get_json_input();
        $judul = trim((string) ($input['judul'] ?? ''));
        if ($judul === '') {
            json_error('Judul kegiatan wajib diisi.', 422);
        }
        $pdo = Database::getConnection();
        $pdo->prepare(
            'INSERT INTO kegiatan (rt_id, rw_id, judul, kategori, deskripsi, lokasi, tanggal_mulai, tanggal_selesai, kuota_peserta, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        )->execute([
            (int) ($user['rt_id'] ?? 1),
            (int) ($user['rw_id'] ?? 1),
            $judul,
            trim((string) ($input['kategori'] ?? 'SOSIAL')),
            trim((string) ($input['deskripsi'] ?? '')),
            trim((string) ($input['lokasi'] ?? 'Balai Warga')),
            trim((string) ($input['tanggal_mulai'] ?? date('Y-m-d H:i:s'))),
            $input['tanggal_selesai'] ?? null,
            (int) ($input['kuota_peserta'] ?? 50),
            (int) $user['id'],
        ]);
        json_success(['id' => (int) $pdo->lastInsertId()], 'Kegiatan berhasil dijadwalkan.', 201);
    }

    public static function registerKegiatan(): never
    {
        $user = AuthMiddleware::authenticate();
        $input = get_json_input();
        $kegiatanId = (int) ($input['kegiatan_id'] ?? 0);
        $pdo = Database::getConnection();
        $pdo->prepare('INSERT IGNORE INTO peserta_kegiatan (kegiatan_id, user_id, status_kehadiran) VALUES (?, ?, "TERDAFTAR")')
            ->execute([$kegiatanId, (int) $user['id']]);
        json_success(null, 'Pendaftaran kegiatan berhasil.');
    }

    public static function listNotifikasi(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM notifikasi WHERE user_id = ? ORDER BY created_at DESC LIMIT 50');
        $stmt->execute([(int) $user['id']]);
        json_success($stmt->fetchAll());
    }

    public static function markNotifikasiRead(): never
    {
        $user = AuthMiddleware::authenticate();
        $input = get_json_input();
        $id = (int) ($input['id'] ?? 0);
        $pdo = Database::getConnection();
        if ($id > 0) {
            $pdo->prepare('UPDATE notifikasi SET is_read = 1, read_at = NOW() WHERE id = ? AND user_id = ?')->execute([$id, (int) $user['id']]);
        } else {
            $pdo->prepare('UPDATE notifikasi SET is_read = 1, read_at = NOW() WHERE user_id = ?')->execute([(int) $user['id']]);
        }
        json_success(null, 'Notifikasi ditandai sudah dibaca.');
    }

    public static function listWilayah(): never
    {
        AuthMiddleware::authenticate();
        $pdo = Database::getConnection();
        $wilayah = $pdo->query('SELECT * FROM wilayah WHERE deleted_at IS NULL')->fetchAll();
        $rw = $pdo->query('SELECT * FROM rw WHERE deleted_at IS NULL')->fetchAll();
        $rt = $pdo->query('SELECT * FROM rt WHERE deleted_at IS NULL')->fetchAll();
        json_success(['wilayah' => $wilayah, 'rw' => $rw, 'rt' => $rt]);
    }

    public static function manageUsers(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN', 'ADMIN_RW']);
        $pdo = Database::getConnection();
        $stmt = $pdo->query(
            'SELECT u.id, u.username, u.email, u.full_name, u.phone, u.is_active, r.code AS role_code, r.name AS role_name, u.rt_id, u.rw_id
             FROM users u INNER JOIN roles r ON r.id = u.role_id WHERE u.deleted_at IS NULL ORDER BY u.id DESC'
        );
        json_success($stmt->fetchAll());
    }

    public static function backupDatabase(): never
    {
        $user = AuthMiddleware::authenticate();
        RbacMiddleware::requireRole($user, ['SUPER_ADMIN']);
        $pdo = Database::getConnection();
        $tables = $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
        $dump = "-- wargaRT SQL Backup " . date('c') . "\nSET FOREIGN_KEY_CHECKS=0;\n";
        foreach ($tables as $t) {
            $create = $pdo->query("SHOW CREATE TABLE `{$t}`")->fetch(PDO::FETCH_NUM);
            $dump .= "\n" . ($create[1] ?? '') . ";\n";
        }
        header('Content-Type: application/sql');
        header('Content-Disposition: attachment; filename="wargart_backup_' . date('Ymd_His') . '.sql"');
        echo $dump;
        exit;
    }
}
