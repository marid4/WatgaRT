<?php
declare(strict_types=1);

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-Signature');

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/../middleware/RateLimitMiddleware.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';
require_once __DIR__ . '/../middleware/RbacMiddleware.php';

RateLimitMiddleware::check('api');

$route = trim((string) ($_GET['route'] ?? $_SERVER['PATH_INFO'] ?? ''), '/');
$method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

require_once __DIR__ . '/controllers/AuthController.php';
require_once __DIR__ . '/controllers/DashboardController.php';
require_once __DIR__ . '/controllers/WargaController.php';
require_once __DIR__ . '/controllers/KeuanganController.php';
require_once __DIR__ . '/controllers/PembayaranController.php';
require_once __DIR__ . '/controllers/LayananController.php';
require_once __DIR__ . '/controllers/LingkunganController.php';

try {
    match ($route) {
        '', 'health' => json_success(['service' => 'wargaRT API', 'version' => '1.0.0', 'time' => date('c')]),

        // Auth
        'auth/login' => AuthController::login(),
        'auth/register' => AuthController::register(),
        'auth/me' => AuthController::me(),
        'auth/fcm-token' => AuthController::updateFcmToken(),

        // Dashboard
        'dashboard/summary' => DashboardController::summary(),

        // Warga & KK
        'warga/list' => WargaController::listWarga(),
        'warga/detail' => WargaController::detailWarga(),
        'warga/create' => WargaController::createWarga(),
        'warga/update' => WargaController::updateWarga(),
        'kk/list' => WargaController::listKeluarga(),
        'kk/create' => WargaController::createKeluarga(),
        'kk/add-member' => WargaController::addAnggotaKeluarga(),
        'kk/print' => WargaController::printKeluargaPdf(),

        // Keuangan, Kas & Iuran
        'kas/summary' => KeuanganController::kasSummary(),
        'kas/transactions' => KeuanganController::listTransaksiKas(),
        'kas/create-transaction' => KeuanganController::createTransaksiKas(),
        'kas/export-csv' => KeuanganController::exportKasCsv(),
        'kas/export-pdf' => KeuanganController::exportKasPdf(),
        'iuran/list' => KeuanganController::listIuran(),
        'iuran/create' => KeuanganController::createIuran(),
        'iuran/generate-bills' => KeuanganController::generateTagihan(),
        'tagihan/list' => KeuanganController::listTagihan(),

        // Pembayaran & Webhook
        'pembayaran/list' => PembayaranController::listPembayaran(),
        'pembayaran/create' => PembayaranController::createPembayaran(),
        'pembayaran/upload-proof' => PembayaranController::uploadBukti(),
        'pembayaran/verify' => PembayaranController::verifyManual(),
        'pembayaran/webhook' => PembayaranController::webhook(),
        'pembayaran/receipt-pdf' => PembayaranController::receiptPdf(),

        // Surat, Pengumuman, Pengaduan
        'surat/templates' => LayananController::listTemplates(),
        'surat/list' => LayananController::listSurat(),
        'surat/create' => LayananController::createSurat(),
        'surat/approve' => LayananController::approveSurat(),
        'surat/pdf' => LayananController::downloadSuratPdf(),
        'pengumuman/list' => LayananController::listPengumuman(),
        'pengumuman/create' => LayananController::createPengumuman(),
        'pengaduan/list' => LayananController::listPengaduan(),
        'pengaduan/create' => LayananController::createPengaduan(),
        'pengaduan/respond' => LayananController::respondPengaduan(),

        // Keamanan, Tamu, Kegiatan, Notifikasi, Admin
        'keamanan/sos' => LingkunganController::triggerSos(),
        'keamanan/contacts' => LingkunganController::emergencyContacts(),
        'keamanan/reports' => LingkunganController::listKeamanan(),
        'keamanan/tamu-list' => LingkunganController::listTamu(),
        'keamanan/tamu-checkin' => LingkunganController::checkinTamu(),
        'keamanan/tamu-checkout' => LingkunganController::checkoutTamu(),
        'kegiatan/list' => LingkunganController::listKegiatan(),
        'kegiatan/create' => LingkunganController::createKegiatan(),
        'kegiatan/register' => LingkunganController::registerKegiatan(),
        'notifikasi/list' => LingkunganController::listNotifikasi(),
        'notifikasi/read' => LingkunganController::markNotifikasiRead(),
        'admin/wilayah' => LingkunganController::listWilayah(),
        'admin/users' => LingkunganController::manageUsers(),
        'admin/backup-sql' => LingkunganController::backupDatabase(),

        default => json_error('Endpoint tidak ditemukan: ' . $route, 404),
    };
} catch (Throwable $e) {
    $msg = APP_DEBUG ? $e->getMessage() : 'Terjadi kesalahan internal server.';
    json_error($msg, 500);
}
