# Dokumentasi REST API wargaRT

Base URL: `https://domainanda.com/api/index.php`

Semua endpoint menggunakan query parameter `?route=<endpoint>` agar kompatibel penuh dengan cPanel Shared Hosting tanpa ketergantungan `mod_rewrite`.

## Autentikasi
Sertakan header pada setiap endpoint terproteksi:
`Authorization: Bearer <JWT_TOKEN>`

| Endpoint (`?route=...`) | Method | Role | Deskripsi |
| :--- | :--- | :--- | :--- |
| `health` | GET | Publik | Cek status layanan API |
| `auth/login` | POST | Publik | Login & mendapatkan JWT token |
| `auth/register` | POST | Publik | Registrasi akun warga baru |
| `auth/me` | GET | Semua | Profil user login aktif |
| `auth/fcm-token` | POST | Semua | Simpan token FCM push notification |
| `dashboard/summary` | GET | Semua | Statistik warga, KK, kas, grafik, aktivitas |
| `warga/list` | GET | Semua | Daftar warga (NIK otomatis dimasking) |
| `warga/detail&id=1` | GET | Semua | Detail warga (RBAC + RT isolation) |
| `warga/create` | POST | Admin RT / Super | Tambah data warga (NIK terenkripsi AES-256-GCM) |
| `warga/update` | POST | Admin RT / Super | Perbarui data warga |
| `kk/list` | GET | Semua | Daftar Kartu Keluarga |
| `kk/create` | POST | Admin RT / Super | Tambah Kartu Keluarga baru |
| `kk/add-member` | POST | Admin RT / Super | Tambah anggota ke KK |
| `kk/print&id=1` | GET | Semua | Unduh PDF data KK internal |
| `kas/summary` | GET | Semua | Ringkasan saldo kas RT |
| `kas/transactions` | GET | Semua | Riwayat pemasukan & pengeluaran kas |
| `kas/create-transaction` | POST | Admin RT / Super | Catat transaksi kas + bukti |
| `kas/export-csv` | GET | Semua | Export laporan kas format CSV/Excel |
| `kas/export-pdf` | GET | Semua | Export laporan kas format PDF |
| `iuran/list` | GET | Semua | Daftar master iuran |
| `iuran/create` | POST | Admin RT / Super | Buat jenis iuran baru |
| `iuran/generate-bills` | POST | Admin RT / Super | Terbitkan tagihan otomatis ke seluruh KK |
| `tagihan/list` | GET | Semua | Daftar tagihan iuran |
| `pembayaran/list` | GET | Semua | Riwayat transaksi pembayaran |
| `pembayaran/create` | POST | Semua | Buat pembayaran QRIS / VA / Transfer Manual (idempoten) |
| `pembayaran/upload-proof` | POST | Semua | Upload bukti transfer manual |
| `pembayaran/verify` | POST | Admin RT / Super | Verifikasi pembayaran manual |
| `pembayaran/webhook` | POST | Gateway | Callback/webhook payment gateway dengan verifikasi signature |
| `pembayaran/receipt-pdf&id=1` | GET | Semua | Unduh kwitansi pembayaran PDF |
| `surat/templates` | GET | Semua | Daftar template surat pengantar/keterangan |
| `surat/list` | GET | Semua | Daftar pengajuan surat |
| `surat/create` | POST | Semua | Ajukan surat baru |
| `surat/approve` | POST | Admin RT/RW/Super | Setujui & terbitkan nomor surat otomatis |
| `surat/pdf&id=1` | GET | Semua | Unduh PDF surat resmi |
| `pengumuman/list` | GET | Semua | Daftar pengumuman RT/RW |
| `pengumuman/create` | POST | Admin | Publikasi pengumuman & kirim notifikasi |
| `pengaduan/list` | GET | Semua | Daftar pengaduan warga |
| `pengaduan/create` | POST | Semua | Kirim pengaduan warga |
| `pengaduan/respond` | POST | Admin RT / Super | Tanggapi & ubah status pengaduan |
| `keamanan/sos` | POST | Semua | Kirim sinyal darurat SOS |
| `keamanan/contacts` | GET | Semua | Kontak Ketua RT, RW, dan Keamanan |
| `keamanan/reports` | GET | Semua | Riwayat laporan keamanan |
| `keamanan/tamu-list` | GET | Semua | Daftar buku tamu digital |
| `keamanan/tamu-checkin` | POST | Semua | Catat tamu masuk |
| `keamanan/tamu-checkout` | POST | Semua | Catat tamu keluar |
| `kegiatan/list` | GET | Semua | Daftar & kalender kegiatan warga |
| `kegiatan/create` | POST | Admin | Buat jadwal kegiatan baru |
| `kegiatan/register` | POST | Semua | Daftar sebagai peserta kegiatan |
| `notifikasi/list` | GET | Semua | Daftar notifikasi user |
| `notifikasi/read` | POST | Semua | Tandai notifikasi telah dibaca |
| `admin/backup-sql` | GET | Super Admin | Unduh struktur backup SQL |
