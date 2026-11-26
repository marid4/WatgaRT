<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../middleware/CsrfMiddleware.php';

CsrfMiddleware::ensureSession();
if (empty($_SESSION['admin_user'])) {
    header('Location: login.php');
    exit;
}

$adminUser = $_SESSION['admin_user'];
$csrfToken = CsrfMiddleware::token();

function render_admin_header(string $title, string $activeMenu = 'dashboard'): void
{
    global $adminUser;
    $menus = [
        'dashboard' => ['Dashboard', 'index.php'],
        'warga' => ['Data Warga & KK', 'panel.php?page=warga'],
        'rt_rw' => ['Wilayah RT/RW', 'panel.php?page=rt_rw'],
        'iuran' => ['Iuran & Tagihan', 'panel.php?page=iuran'],
        'pembayaran' => ['Verifikasi Bayar', 'panel.php?page=pembayaran'],
        'kas' => ['Keuangan & Kas', 'panel.php?page=kas'],
        'surat' => ['Verifikasi Surat', 'panel.php?page=surat'],
        'pengumuman' => ['Pengumuman', 'panel.php?page=pengumuman'],
        'pengaduan' => ['Pengaduan Warga', 'panel.php?page=pengaduan'],
        'laporan' => ['Laporan & Export', 'panel.php?page=laporan'],
        'roles' => ['Role & Permission', 'panel.php?page=roles'],
        'settings' => ['Pengaturan Sistem', 'panel.php?page=settings'],
    ];
    ?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= htmlspecialchars($title) ?> - wargaRT Admin</title>
    <style>
        :root { --navy: #0B1F3A; --royal: #1D4ED8; --gold: #D4AF37; --bg: #F3F5F9; --text: #0F172A; --muted: #64748B; }
        * { box-sizing: border-box; }
        body { margin: 0; font-family: 'Segoe UI', system-ui, sans-serif; background: var(--bg); color: var(--text); display: flex; min-height: 100vh; }
        aside { width: 250px; background: var(--navy); color: #fff; padding: 22px 14px; flex-shrink: 0; display: flex; flex-direction: column; }
        .brand { font-size: 20px; font-weight: 800; padding: 0 12px 18px; border-bottom: 1px solid rgba(255,255,255,.12); margin-bottom: 14px; }
        .brand span { color: var(--gold); }
        nav a { display: block; padding: 10px 12px; border-radius: 10px; color: #CBD5E1; text-decoration: none; font-size: 14px; margin-bottom: 4px; }
        nav a.active, nav a:hover { background: rgba(29, 78, 216, .45); color: #fff; font-weight: 600; }
        main { flex: 1; padding: 26px 32px; overflow-x: auto; }
        .topbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
        .badge { background: #E0E7FF; color: var(--royal); padding: 6px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 16px; margin-bottom: 24px; }
        .stat { background: #fff; padding: 18px 20px; border-radius: 14px; box-shadow: 0 4px 16px rgba(11,31,58,.05); border-left: 4px solid var(--royal); }
        .stat small { color: var(--muted); font-size: 12px; font-weight: 600; text-transform: uppercase; }
        .stat h2 { margin: 8px 0 0; font-size: 24px; color: var(--navy); }
        .card { background: #fff; border-radius: 14px; padding: 20px; box-shadow: 0 4px 16px rgba(11,31,58,.05); margin-bottom: 20px; }
        table { width: 100%; border-collapse: collapse; font-size: 14px; }
        th, td { padding: 11px 12px; text-align: left; border-bottom: 1px solid #E2E8F0; }
        th { color: var(--navy); font-size: 12px; text-transform: uppercase; background: #F8FAFC; }
        .btn { display: inline-block; padding: 8px 14px; border-radius: 8px; border: none; background: var(--royal); color: #fff; font-size: 13px; font-weight: 600; text-decoration: none; cursor: pointer; }
        .btn-gold { background: var(--gold); color: var(--navy); }
        .btn-danger { background: #DC2626; }
        input, select, textarea { padding: 9px 11px; border: 1px solid #CBD5E1; border-radius: 8px; font-size: 14px; }
    </style>
</head>
<body>
<aside>
    <div class="brand">warga<span>RT</span></div>
    <nav>
        <?php foreach ($menus as $key => [$label, $href]): ?>
            <a href="<?= $href ?>" class="<?= $activeMenu === $key ? 'active' : '' ?>"><?= htmlspecialchars($label) ?></a>
        <?php endforeach; ?>
        <a href="logout.php" style="margin-top:18px;color:#FCA5A5;">Keluar</a>
    </nav>
</aside>
<main>
    <div class="topbar">
        <h1 style="margin:0;font-size:22px;color:#0B1F3A;"><?= htmlspecialchars($title) ?></h1>
        <div>
            <span class="badge"><?= htmlspecialchars($adminUser['full_name']) ?> (<?= htmlspecialchars($adminUser['role_name']) ?>)</span>
        </div>
    </div>
    <?php
}

function render_admin_footer(): void
{
    echo '</main></body></html>';
}
