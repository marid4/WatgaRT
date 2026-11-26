<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../middleware/CsrfMiddleware.php';

CsrfMiddleware::ensureSession();
if (!empty($_SESSION['admin_user'])) {
    header('Location: index.php');
    exit;
}

$error = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    CsrfMiddleware::verifyOrFail();
    $identity = trim((string) ($_POST['username'] ?? ''));
    $password = (string) ($_POST['password'] ?? '');

    $pdo = Database::getConnection();
    $stmt = $pdo->prepare(
        'SELECT u.*, r.code AS role_code, r.name AS role_name
         FROM users u INNER JOIN roles r ON r.id = u.role_id
         WHERE (u.username = ? OR u.email = ?) AND u.deleted_at IS NULL LIMIT 1'
    );
    $stmt->execute([$identity, $identity]);
    $user = $stmt->fetch();

    if ($user && password_verify($password, $user['password_hash']) && in_array($user['role_code'], ['SUPER_ADMIN', 'ADMIN_RW', 'ADMIN_RT'], true)) {
        session_regenerate_id(true);
        $_SESSION['admin_user'] = [
            'id' => (int) $user['id'],
            'full_name' => $user['full_name'],
            'role_code' => $user['role_code'],
            'role_name' => $user['role_name'],
            'rt_id' => $user['rt_id'] ? (int) $user['rt_id'] : null,
            'rw_id' => $user['rw_id'] ? (int) $user['rw_id'] : null,
        ];
        header('Location: index.php');
        exit;
    }
    $error = 'Kredensial tidak valid atau akun bukan pengelola RT/RW.';
}
$csrf = CsrfMiddleware::token();
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login Admin - wargaRT</title>
    <style>
        :root { --navy: #0B1F3A; --royal: #1D4ED8; --gold: #D4AF37; --bg: #F3F5F9; }
        body { margin: 0; font-family: 'Segoe UI', system-ui, sans-serif; background: linear-gradient(135deg, var(--navy), #132F57); min-height: 100vh; display: grid; place-items: center; }
        .card { background: #fff; width: 100%; max-width: 400px; border-radius: 18px; padding: 34px; box-shadow: 0 24px 60px rgba(0,0,0,.35); border-top: 4px solid var(--gold); }
        h1 { margin: 0 0 6px; color: var(--navy); font-size: 24px; }
        p { margin: 0 0 22px; color: #64748B; font-size: 14px; }
        label { display: block; font-size: 13px; font-weight: 600; color: var(--navy); margin-bottom: 6px; }
        input { width: 100%; box-sizing: border-box; padding: 12px 14px; border: 1px solid #CBD5E1; border-radius: 10px; margin-bottom: 16px; font-size: 14px; }
        button { width: 100%; padding: 13px; border: none; border-radius: 10px; background: var(--royal); color: #fff; font-weight: 700; font-size: 15px; cursor: pointer; }
        .err { background: #FEE2E2; color: #991B1B; padding: 10px 12px; border-radius: 8px; font-size: 13px; margin-bottom: 14px; }
    </style>
</head>
<body>
    <div class="card">
        <h1>wargaRT Admin</h1>
        <p>Panel Administrasi Warga &amp; Keuangan RT/RW</p>
        <?php if ($error !== ''): ?><div class="err"><?= htmlspecialchars($error) ?></div><?php endif; ?>
        <form method="POST">
            <input type="hidden" name="_csrf_token" value="<?= htmlspecialchars($csrf) ?>">
            <label>Username atau Email</label>
            <input type="text" name="username" required placeholder="adminrt01">
            <label>Password</label>
            <input type="password" name="password" required placeholder="••••••••">
            <button type="submit">Masuk ke Dashboard</button>
        </form>
    </div>
</body>
</html>
