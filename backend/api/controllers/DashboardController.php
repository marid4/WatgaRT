<?php
declare(strict_types=1);

final class DashboardController
{
    public static function summary(): never
    {
        $user = AuthMiddleware::authenticate();
        $pdo = Database::getConnection();

        $rtFilter = '';
        $params = [];
        if ($user['role_code'] === 'ADMIN_RT' || $user['role_code'] === 'WARGA') {
            $rtFilter = ' AND rt_id = ?';
            $params[] = (int) $user['rt_id'];
        } elseif ($user['role_code'] === 'ADMIN_RW') {
            $rtFilter = ' AND rt_id IN (SELECT id FROM rt WHERE rw_id = ?)';
            $params[] = (int) $user['rw_id'];
        }

        $wargaStats = $pdo->prepare(
            "SELECT
                COUNT(*) AS total_warga,
                SUM(CASE WHEN jenis_kelamin = 'L' THEN 1 ELSE 0 END) AS total_laki,
                SUM(CASE WHEN jenis_kelamin = 'P' THEN 1 ELSE 0 END) AS total_perempuan,
                SUM(CASE WHEN status_domisili = 'AKTIF' THEN 1 ELSE 0 END) AS total_aktif
             FROM warga WHERE deleted_at IS NULL {$rtFilter}"
        );
        $wargaStats->execute($params);
        $wRow = $wargaStats->fetch() ?: [];

        $kkStmt = $pdo->prepare("SELECT COUNT(*) FROM keluarga WHERE deleted_at IS NULL {$rtFilter}");
        $kkStmt->execute($params);
        $totalKk = (int) $kkStmt->fetchColumn();

        $kasStmt = $pdo->prepare("SELECT COALESCE(SUM(saldo_sekarang), 0) FROM kas WHERE 1=1 {$rtFilter}");
        $kasStmt->execute($params);
        $totalKas = (float) $kasStmt->fetchColumn();

        $tagihanStmt = $pdo->prepare("SELECT COUNT(*), COALESCE(SUM(total_tagihan), 0) FROM tagihan WHERE status = 'BELUM_LUNAS' {$rtFilter}");
        $tagihanStmt->execute($params);
        $tagRow = $tagihanStmt->fetch(PDO::FETCH_NUM) ?: [0, 0];

        $chartStmt = $pdo->prepare(
            "SELECT DATE_FORMAT(tanggal_transaksi, '%Y-%m') AS bulan,
                    SUM(CASE WHEN jenis = 'PEMASUKAN' THEN nominal ELSE 0 END) AS pemasukan,
                    SUM(CASE WHEN jenis = 'PENGELUARAN' THEN nominal ELSE 0 END) AS pengeluaran
             FROM transaksi_kas
             WHERE deleted_at IS NULL {$rtFilter}
             GROUP BY DATE_FORMAT(tanggal_transaksi, '%Y-%m')
             ORDER BY bulan DESC LIMIT 6"
        );
        $chartStmt->execute($params);
        $chart = array_reverse($chartStmt->fetchAll());

        $notifStmt = $pdo->prepare(
            'SELECT id, tipe, judul, pesan, is_read, created_at FROM notifikasi WHERE user_id = ? ORDER BY created_at DESC LIMIT 5'
        );
        $notifStmt->execute([(int) $user['id']]);

        $annStmt = $pdo->prepare(
            "SELECT id, kategori, judul, published_at FROM pengumuman WHERE deleted_at IS NULL {$rtFilter} ORDER BY is_pinned DESC, published_at DESC LIMIT 5"
        );
        $annStmt->execute($params);

        json_success([
            'total_warga' => (int) ($wRow['total_warga'] ?? 0),
            'total_kk' => $totalKk,
            'total_laki' => (int) ($wRow['total_laki'] ?? 0),
            'total_perempuan' => (int) ($wRow['total_perempuan'] ?? 0),
            'total_aktif' => (int) ($wRow['total_aktif'] ?? 0),
            'total_kas' => $totalKas,
            'tagihan_belum_lunas_count' => (int) $tagRow[0],
            'tagihan_belum_lunas_nominal' => (float) $tagRow[1],
            'grafik_kas' => $chart,
            'notifikasi_penting' => $notifStmt->fetchAll(),
            'aktivitas_terbaru' => $annStmt->fetchAll(),
        ]);
    }
}
