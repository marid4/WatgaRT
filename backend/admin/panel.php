<?php
declare(strict_types=1);

require_once __DIR__ . '/layout.php';
require_once __DIR__ . '/../services/PaymentGatewayService.php';
require_once __DIR__ . '/../services/NotificationService.php';

$page = (string) ($_GET['page'] ?? 'warga');
$pdo = Database::getConnection();
$flash = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    CsrfMiddleware::verifyOrFail();
    $action = (string) ($_POST['action'] ?? '');
    if ($action === 'verify_payment') {
        PaymentGatewayService::processSettlement((int) $_POST['pembayaran_id'], (int) $adminUser['id'], 'WEB_ADMIN');
        $flash = 'Pembayaran berhasil diverifikasi dan saldo kas bertambah.';
    } elseif ($action === 'approve_surat') {
        $id = (int) $_POST['surat_id'];
        $nomor = str_pad((string) $id, 3, '0', STR_PAD_LEFT) . '/SRT-RT/' . date('m/Y');
        $pdo->prepare('UPDATE pengajuan_surat SET status = "DISETUJUI", nomor_surat = ?, disetujui_oleh = ?, tanggal_persetujuan = NOW() WHERE id = ?')
            ->execute([$nomor, (int) $adminUser['id'], $id]);
        $flash = 'Surat disetujui dengan nomor ' . $nomor;
    } elseif ($action === 'save_setting') {
        $pdo->prepare('UPDATE app_settings SET setting_value = ? WHERE id = ?')->execute([trim((string) $_POST['setting_value']), (int) $_POST['setting_id']]);
        $flash = 'Pengaturan sistem diperbarui.';
    }
}

$titles = [
    'warga' => 'Manajemen Warga & KK',
    'rt_rw' => 'Manajemen Wilayah RT / RW',
    'iuran' => 'Manajemen Iuran & Tagihan',
    'pembayaran' => 'Verifikasi Pembayaran',
    'kas' => 'Keuangan & Kas RT',
    'surat' => 'Verifikasi Pengajuan Surat',
    'pengumuman' => 'Manajemen Pengumuman',
    'pengaduan' => 'Pengaduan Warga',
    'laporan' => 'Laporan & Export Data',
    'roles' => 'Manajemen Role & Permission',
    'settings' => 'Pengaturan Sistem',
];

render_admin_header($titles[$page] ?? 'Panel Admin', $page);
if ($flash !== ''): ?>
    <div style="background:#DCFCE7;color:#166534;padding:12px 16px;border-radius:10px;margin-bottom:16px;font-weight:600;"><?= htmlspecialchars($flash) ?></div>
<?php endif; ?>

<div class="card">
<?php if ($page === 'warga'):
    $rows = $pdo->query('SELECT id, nik_masked, nama_lengkap, jenis_kelamin, alamat_lengkap, status_domisili FROM warga WHERE deleted_at IS NULL ORDER BY id DESC LIMIT 50')->fetchAll(); ?>
    <table><thead><tr><th>NIK (Masked)</th><th>Nama Lengkap</th><th>L/P</th><th>Alamat</th><th>Status</th></tr></thead><tbody>
    <?php foreach ($rows as $r): ?>
        <tr><td><?= htmlspecialchars($r['nik_masked']) ?></td><td><?= htmlspecialchars($r['nama_lengkap']) ?></td><td><?= htmlspecialchars($r['jenis_kelamin']) ?></td><td><?= htmlspecialchars($r['alamat_lengkap']) ?></td><td><?= htmlspecialchars($r['status_domisili']) ?></td></tr>
    <?php endforeach; ?>
    </tbody></table>

<?php elseif ($page === 'pembayaran'):
    $rows = $pdo->query('SELECT p.id, p.order_id, p.metode_pembayaran, p.total_bayar, p.status, u.full_name FROM pembayaran p INNER JOIN users u ON u.id = p.user_id ORDER BY p.id DESC LIMIT 50')->fetchAll(); ?>
    <table><thead><tr><th>Order ID</th><th>Warga</th><th>Metode</th><th>Total</th><th>Status</th><th>Aksi</th></tr></thead><tbody>
    <?php foreach ($rows as $r): ?>
        <tr>
            <td><?= htmlspecialchars($r['order_id']) ?></td>
            <td><?= htmlspecialchars($r['full_name']) ?></td>
            <td><?= htmlspecialchars($r['metode_pembayaran']) ?></td>
            <td>Rp <?= number_format((float) $r['total_bayar'], 0, ',', '.') ?></td>
            <td><?= htmlspecialchars($r['status']) ?></td>
            <td>
                <?php if ($r['status'] !== 'BERHASIL'): ?>
                <form method="POST" style="display:inline;">
                    <input type="hidden" name="_csrf_token" value="<?= htmlspecialchars($csrfToken) ?>">
                    <input type="hidden" name="action" value="verify_payment">
                    <input type="hidden" name="pembayaran_id" value="<?= (int) $r['id'] ?>">
                    <button class="btn" type="submit">Verifikasi Lunas</button>
                </form>
                <?php endif; ?>
            </td>
        </tr>
    <?php endforeach; ?>
    </tbody></table>

<?php elseif ($page === 'surat'):
    $rows = $pdo->query('SELECT ps.id, ps.jenis_surat, ps.keperluan, ps.status, ps.nomor_surat, w.nama_lengkap FROM pengajuan_surat ps INNER JOIN warga w ON w.id = ps.warga_id ORDER BY ps.id DESC LIMIT 50')->fetchAll(); ?>
    <table><thead><tr><th>Pemohon</th><th>Jenis Surat</th><th>Keperluan</th><th>Nomor Surat</th><th>Status</th><th>Aksi</th></tr></thead><tbody>
    <?php foreach ($rows as $r): ?>
        <tr>
            <td><?= htmlspecialchars($r['nama_lengkap']) ?></td>
            <td><?= htmlspecialchars($r['jenis_surat']) ?></td>
            <td><?= htmlspecialchars($r['keperluan']) ?></td>
            <td><?= htmlspecialchars($r['nomor_surat'] ?? '-') ?></td>
            <td><?= htmlspecialchars($r['status']) ?></td>
            <td>
                <?php if ($r['status'] === 'DIAJUKAN'): ?>
                <form method="POST" style="display:inline;">
                    <input type="hidden" name="_csrf_token" value="<?= htmlspecialchars($csrfToken) ?>">
                    <input type="hidden" name="action" value="approve_surat">
                    <input type="hidden" name="surat_id" value="<?= (int) $r['id'] ?>">
                    <button class="btn" type="submit">Setujui &amp; Terbitkan</button>
                </form>
                <?php endif; ?>
            </td>
        </tr>
    <?php endforeach; ?>
    </tbody></table>

<?php elseif ($page === 'settings'):
    $rows = $pdo->query('SELECT * FROM app_settings ORDER BY setting_group, setting_key')->fetchAll(); ?>
    <table><thead><tr><th>Key</th><th>Group</th><th>Nilai</th><th>Aksi</th></tr></thead><tbody>
    <?php foreach ($rows as $r): ?>
        <tr>
            <td><strong><?= htmlspecialchars($r['setting_key']) ?></strong><br><small><?= htmlspecialchars($r['description'] ?? '') ?></small></td>
            <td><?= htmlspecialchars($r['setting_group']) ?></td>
            <td colspan="2">
                <form method="POST" style="display:flex;gap:8px;">
                    <input type="hidden" name="_csrf_token" value="<?= htmlspecialchars($csrfToken) ?>">
                    <input type="hidden" name="action" value="save_setting">
                    <input type="hidden" name="setting_id" value="<?= (int) $r['id'] ?>">
                    <input type="text" name="setting_value" value="<?= htmlspecialchars((string) $r['setting_value']) ?>" style="flex:1;">
                    <button class="btn" type="submit">Simpan</button>
                </form>
            </td>
        </tr>
    <?php endforeach; ?>
    </tbody></table>

<?php elseif ($page === 'laporan'): ?>
    <p>Unduh laporan resmi langsung dalam format CSV (Excel) dan PDF:</p>
    <a class="btn" href="../api/index.php?route=kas/export-csv" target="_blank">Export Kas CSV / Excel</a>
    <a class="btn btn-gold" href="../api/index.php?route=kas/export-pdf" target="_blank">Export Kas PDF</a>

<?php else:
    $tableMap = [
        'rt_rw' => 'SELECT r.id, rw.nomor_rw, r.nomor_rt, r.nama_ketua, r.no_hp_ketua, r.rekening_bank FROM rt r INNER JOIN rw ON rw.id = r.rw_id',
        'iuran' => 'SELECT id, kode_iuran, nama_iuran, kategori, nominal, periode FROM iuran WHERE deleted_at IS NULL',
        'kas' => 'SELECT id, tanggal_transaksi, jenis, kategori, judul, nominal, saldo_setelah FROM transaksi_kas WHERE deleted_at IS NULL ORDER BY id DESC LIMIT 40',
        'pengumuman' => 'SELECT id, scope, kategori, judul, published_at FROM pengumuman WHERE deleted_at IS NULL ORDER BY id DESC',
        'pengaduan' => 'SELECT id, kode_tiket, kategori, judul, prioritas, status FROM pengaduan ORDER BY id DESC',
        'roles' => 'SELECT id, code, name, description FROM roles',
    ];
    $rows = $pdo->query($tableMap[$page] ?? 'SELECT id, nama_lengkap FROM warga LIMIT 10')->fetchAll();
    if ($rows): ?>
    <table><thead><tr><?php foreach (array_keys($rows[0]) as $col): ?><th><?= htmlspecialchars($col) ?></th><?php endforeach; ?></tr></thead>
    <tbody><?php foreach ($rows as $r): ?><tr><?php foreach ($r as $v): ?><td><?= htmlspecialchars((string) $v) ?></td><?php endforeach; ?></tr><?php endforeach; ?></tbody></table>
    <?php else: ?><p>Belum ada data.</p><?php endif;
endif; ?>
</div>
<?php render_admin_footer(); ?>
