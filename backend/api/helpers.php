<?php
declare(strict_types=1);

function json_success(mixed $data = null, string $message = 'OK', int $code = 200): never
{
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'success' => true,
        'message' => $message,
        'data' => $data,
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

function json_error(string $message, int $code = 400, mixed $errors = null): never
{
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'success' => false,
        'message' => $message,
        'errors' => $errors,
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

function get_json_input(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || trim($raw) === '') {
        return $_POST;
    }
    $decoded = json_decode($raw, true);
    return is_array($decoded) ? $decoded : [];
}

function clean_str(?string $value): string
{
    return htmlspecialchars(trim((string) $value), ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

function handle_secure_upload(string $fieldName, string $subFolder, array $allowedExts = ['jpg', 'jpeg', 'png', 'pdf']): ?string
{
    if (!isset($_FILES[$fieldName]) || $_FILES[$fieldName]['error'] === UPLOAD_ERR_NO_FILE) {
        return null;
    }
    $file = $_FILES[$fieldName];
    if ($file['error'] !== UPLOAD_ERR_OK) {
        json_error('Gagal mengunggah berkas.', 400);
    }
    if ($file['size'] > MAX_UPLOAD_BYTES) {
        json_error('Ukuran berkas maksimal 5 MB.', 400);
    }
    $ext = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
    if (!in_array($ext, $allowedExts, true)) {
        json_error('Format berkas tidak diizinkan.', 400);
    }

    $targetDir = UPLOAD_BASE_DIR . '/' . $subFolder;
    if (!is_dir($targetDir)) {
        mkdir($targetDir, 0755, true);
    }
    $newName = date('Ymd_His_') . bin2hex(random_bytes(6)) . '.' . $ext;
    $dest = $targetDir . '/' . $newName;
    if (!move_uploaded_file($file['tmp_name'], $dest)) {
        json_error('Gagal menyimpan berkas unggahan.', 500);
    }
    return 'uploads/' . $subFolder . '/' . $newName;
}
