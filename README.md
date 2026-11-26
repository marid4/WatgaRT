# wargaRT — Management Warga Premium

Aplikasi manajemen administrasi warga RT, RW, dan perumahan berbasis online (Android Native Kotlin + Jetpack Compose + PHP 8+ REST API + MySQL/MariaDB + Web Admin Panel).

## Fitur Utama
- **Multi-Role RBAC**: Super Admin, Admin RW, Admin RT, dan Warga dengan isolasi akses antar-RT/RW.
- **Dashboard Premium**: Statistik warga, KK, saldo kas RT, tagihan belum lunas, grafik pemasukan/pengeluaran, dan notifikasi penting.
- **Data Warga & Kartu Keluarga**: Enkripsi AES-256-GCM untuk NIK & No. KK, masking otomatis untuk non-berwenang, dan cetak PDF KK internal.
- **Keuangan, Kas RT & Iuran**: Kas otomatis, tagihan bulanan massal, pembayaran QRIS, Virtual Account, Transfer Manual, webhook anti-duplikasi, kwitansi PDF, dan export CSV/PDF.
- **Layanan Warga**: Pengajuan & persetujuan surat menyurat PDF bernomor otomatis, pengumuman RT/RW, dan pengaduan warga.
- **Keamanan & Kegiatan**: Tombol darurat SOS, kontak darurat wilayah, buku tamu digital, dan kalender kegiatan warga.
- **Mode Offline & Sinkronisasi**: Cache lokal Room Database (warga, pengumuman, riwayat bayar) dan antrean pengajuan offline yang otomatis tersinkron saat internet kembali tersedia.

## Struktur Direktori
- `android-app/` — Proyek Android multi-modul (`app`, `core`, `domain`, `data`, `feature`) + keystore release (`keystore/wargart-release.jks`).
- `backend/` — REST API PHP 8+ (`api/`), Web Admin Panel (`admin/`), middleware keamanan, dan layanan PDF/Enkripsi/Payment.
- `database/` — Skema MySQL 27 tabel (`schema.sql`) dan data awal (`seed.sql`).
- `documentation/` — Dokumentasi endpoint (`API.md`) dan panduan instalasi/build (`INSTALLATION.md`).
