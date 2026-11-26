<?php
declare(strict_types=1);

require_once __DIR__ . '/layout.php';

$pdo = Database::getConnection();
$wargaCount = (int) $pdo->query('SELECT COUNT(*) FROM warga WHERE deleted_at IS NULL')->fetchColumn();
$kkCount = (int) $pdo->query('SELECT COUNT(*) FROM keluarga WHERE deleted_at IS NULL')->fetchColumn();
$saldoKas = (float) $pdo->query('SELECT COALESCE(SUM(saldo_sekarang), 0) FROM kas')->fetchColumn();
$pendingBayar = (int) $pdo->query('SELECT COUNT(*) FROM pembayaran WHERE status = "MENUNGGU_VERIFIKASI"')->fetchColumn();
$pendingSurat = (int) $pdo->query('SELECT COUNT(*) FROM pengajuan_surat WHERE status = "DIAJUKAN"')->fetchColumn();

$recentLogs = $pdo->query(
    'SELECT a.action, a.entity_type, a.created_at, u.full_name FROM audit_logs a LEFT JOIN users u ON u.id = a.user_id ORDER BY a.id DESC LIMIT 8'
)->fetchAll();

render_admin_header('Dashboard Statistik', 'dashboard');
?>
<div class="grid">
    <div class="stat"><small>Total Warga</small><h2><?= number_format($wargaCount) ?></h2></div>
    <div class="stat"><small>Kartu Keluarga</small><h2><?= number_format($kkCount) ?></h2></div>
    <div class="stat" style="border-left-color:#D4AF37;"><small>Total Saldo Kas</small><h2>Rp <?= number_format($saldoKas, 0, ',', '.') ?></h2></div>
    <div class="stat" style="border-left-color:#F59E0B;"><small>Verifikasi Bayar / Surat</small><h2><?= $pendingBayar ?> / <?= $pendingSurat ?></h2></div>
</div>

<div class="card">
    <h3 style="margin-top:0;">Aktivitas Sistem Terbaru (Audit Log)</h3>
    <table>
        <thead><tr><th>Waktu</th><th>Pengguna</th><th>Aksi</th><th>Entitas</th></tr></thead>
        <tbody>
        <?php foreach ($recentLogs as $log): ?>
            <tr>
                <td><?= htmlspecialchars($log['created_at']) ?></td>
                <td><?= htmlspecialchars($log['full_name'] ?? 'Sistem') ?></td>
                <td><strong><?= htmlspecialchars($log['action']) ?></strong></td>
                <td><?= htmlspecialchars($log['entity_type']) ?></td>
            </tr>
        <?php endforeach; ?>
        </tbody>
    </table>
</div>
<?php render_admin_footer(); ?>
