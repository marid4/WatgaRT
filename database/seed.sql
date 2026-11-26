-- ============================================================================
-- wargaRT (Management Warga Premium) - Initial Seed Data
-- ============================================================================
SET NAMES utf8mb4;
USE `wargart_db`;

-- 1. Roles
INSERT INTO `roles` (`id`, `code`, `name`, `description`) VALUES
(1, 'SUPER_ADMIN', 'Super Admin', 'Pengelola seluruh sistem, wilayah, dan hak akses'),
(2, 'ADMIN_RW', 'Admin RW', 'Pengelola koordinasi antar-RT dan laporan wilayah RW'),
(3, 'ADMIN_RT', 'Admin RT', 'Pengelola administrasi warga, KK, iuran, kas, surat, dan pengaduan RT'),
(4, 'WARGA', 'Warga', 'Warga perumahan / lingkungan RT');

-- 2. Permissions
INSERT INTO `permissions` (`id`, `code`, `module`, `description`) VALUES
(1, 'system.manage', 'system', 'Kelola pengaturan sistem & wilayah'),
(2, 'users.manage', 'users', 'Kelola akun admin dan role'),
(3, 'warga.view', 'warga', 'Lihat data warga'),
(4, 'warga.manage', 'warga', 'Tambah/edit/hapus data warga & KK'),
(5, 'warga.view_sensitive', 'warga', 'Lihat NIK dan No KK lengkap'),
(6, 'iuran.manage', 'iuran', 'Kelola master iuran & tagihan'),
(7, 'pembayaran.verify', 'pembayaran', 'Verifikasi pembayaran manual'),
(8, 'kas.manage', 'kas', 'Kelola pemasukan dan pengeluaran kas'),
(9, 'surat.approve', 'surat', 'Setujui dan terbitkan nomor surat'),
(10, 'pengumuman.manage', 'pengumuman', 'Buat dan kelola pengumuman'),
(11, 'pengaduan.manage', 'pengaduan', 'Tanggapi dan ubah status pengaduan'),
(12, 'keamanan.manage', 'keamanan', 'Kelola buku tamu & laporan keamanan'),
(13, 'kegiatan.manage', 'kegiatan', 'Kelola kalender kegiatan warga'),
(14, 'laporan.export', 'laporan', 'Export laporan PDF, Excel, CSV');

-- 3. Wilayah, RW, RT
INSERT INTO `wilayah` (`id`, `kode_wilayah`, `nama_perumahan`, `kelurahan`, `kecamatan`, `kota_kabupaten`, `provinsi`, `kode_pos`, `alamat_sekretariat`) VALUES
(1, 'WLG-001', 'Perumahan Graha Harmoni Residence', 'Sukamaju', 'Cipedes', 'Kota Tasikmalaya', 'Jawa Barat', '46133', 'Jl. Harmoni Raya No. 1, Graha Harmoni Residence');

INSERT INTO `rw` (`id`, `wilayah_id`, `nomor_rw`, `nama_ketua`, `no_hp_ketua`, `alamat_sekretariat`) VALUES
(1, 1, '05', 'H. Ahmad Fauzi, S.Sos', '081234567801', 'Balai Warga RW 05, Jl. Harmoni Raya');

INSERT INTO `rt` (`id`, `rw_id`, `nomor_rt`, `nama_ketua`, `no_hp_ketua`, `no_hp_keamanan`, `alamat_sekretariat`, `rekening_bank`, `nomor_rekening`, `atas_nama_rekening`) VALUES
(1, 1, '01', 'Budi Santoso', '081234567811', '081234567899', 'Pos Sekretariat RT 01 Blok A', 'BCA', '1234567890', 'Kas RT 01 RW 05 Graha Harmoni'),
(2, 1, '02', 'Dedi Kurniawan', '081234567812', '081234567898', 'Pos Sekretariat RT 02 Blok B', 'BRI', '098765432101234', 'Kas RT 02 RW 05 Graha Harmoni');

-- 4. Users (Password default untuk semua akun demo: "WargaRT@2026" - hash bcrypt)
INSERT INTO `users` (`id`, `role_id`, `wilayah_id`, `rw_id`, `rt_id`, `username`, `email`, `phone`, `full_name`, `password_hash`, `is_active`) VALUES
(1, 1, 1, NULL, NULL, 'superadmin', 'superadmin@wargart.id', '081100000001', 'Administrator Sistem', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1),
(2, 2, 1, 1, NULL, 'adminrw05', 'rw05@wargart.id', '081234567801', 'H. Ahmad Fauzi, S.Sos', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1),
(3, 3, 1, 1, 1, 'adminrt01', 'rt01@wargart.id', '081234567811', 'Budi Santoso', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1),
(4, 3, 1, 1, 2, 'adminrt02', 'rt02@wargart.id', '081234567812', 'Dedi Kurniawan', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1),
(5, 4, 1, 1, 1, 'warga01', 'andi.wijaya@wargart.id', '081299887766', 'Andi Wijaya', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1),
(6, 4, 1, 1, 1, 'warga02', 'siti.rahma@wargart.id', '081299887755', 'Siti Rahmawati', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1);

-- 5. Keluarga (SHA-256 hash & masked value; encrypted value diisi oleh backend saat create/update)
INSERT INTO `keluarga` (`id`, `rt_id`, `no_kk_encrypted`, `no_kk_hash`, `no_kk_masked`, `kepala_keluarga_nama`, `alamat`, `blok_rumah`, `status_hunian`) VALUES
(1, 1, 'DEMO_ENC_3278011001900001', SHA2('3278011001900001', 256), '327801******0001', 'Andi Wijaya', 'Jl. Harmoni Blok A No. 12', 'A-12', 'TETAP'),
(2, 1, 'DEMO_ENC_3278011001900002', SHA2('3278011001900002', 256), '327801******0002', 'Hendra Kusuma', 'Jl. Harmoni Blok A No. 15', 'A-15', 'TETAP');

-- 6. Warga
INSERT INTO `warga` (`id`, `user_id`, `keluarga_id`, `rt_id`, `nik_encrypted`, `nik_hash`, `nik_masked`, `nama_lengkap`, `tempat_lahir`, `tanggal_lahir`, `jenis_kelamin`, `alamat_lengkap`, `no_whatsapp`, `status_perkawinan`, `pekerjaan`, `status_domisili`, `tanggal_masuk`) VALUES
(1, 5, 1, 1, 'DEMO_ENC_3278011505880001', SHA2('3278011505880001', 256), '327801******0001', 'Andi Wijaya', 'Tasikmalaya', '1988-05-15', 'L', 'Jl. Harmoni Blok A No. 12, RT 01/RW 05', '081299887766', 'KAWIN', 'Karyawan Swasta', 'AKTIF', '2021-03-01'),
(2, 6, 1, 1, 'DEMO_ENC_3278015208900002', SHA2('3278015208900002', 256), '327801******0002', 'Siti Rahmawati', 'Bandung', '1990-08-12', 'P', 'Jl. Harmoni Blok A No. 12, RT 01/RW 05', '081299887755', 'KAWIN', 'Ibu Rumah Tangga', 'AKTIF', '2021-03-01'),
(3, NULL, 2, 1, 'DEMO_ENC_3278012003850003', SHA2('3278012003850003', 256), '327801******0003', 'Hendra Kusuma', 'Garut', '1985-03-20', 'L', 'Jl. Harmoni Blok A No. 15, RT 01/RW 05', '081299887744', 'KAWIN', 'Wiraswasta', 'AKTIF', '2020-07-10');

INSERT INTO `anggota_keluarga` (`keluarga_id`, `warga_id`, `hubungan_keluarga`, `tanggal_bergabung`, `is_active`) VALUES
(1, 1, 'KEPALA_KELUARGA', '2021-03-01', 1),
(1, 2, 'ISTRI', '2021-03-01', 1),
(2, 3, 'KEPALA_KELUARGA', '2020-07-10', 1);

-- 7. Iuran & Tagihan
INSERT INTO `iuran` (`id`, `rt_id`, `kode_iuran`, `nama_iuran`, `kategori`, `nominal`, `periode`, `jatuh_tempo_hari`, `deskripsi`, `created_by`) VALUES
(1, 1, 'IUR-KEAMANAN', 'Iuran Keamanan & Satpam', 'KEAMANAN', 75000.00, 'BULANAN', 10, 'Iuran keamanan 24 jam lingkungan RT 01', 3),
(2, 1, 'IUR-KEBERSIHAN', 'Iuran Kebersihan & Sampah', 'KEBERSIHAN', 35000.00, 'BULANAN', 10, 'Iuran pengangkutan sampah rutin 3x seminggu', 3),
(3, 1, 'IUR-SOSIAL', 'Iuran Dana Sosial RT', 'SOSIAL', 15000.00, 'BULANAN', 10, 'Dana santunan duka dan sosial warga', 3);

INSERT INTO `tagihan` (`id`, `iuran_id`, `keluarga_id`, `rt_id`, `nomor_tagihan`, `periode_bulan`, `periode_tahun`, `nominal`, `denda`, `total_tagihan`, `tanggal_jatuh_tempo`, `status`) VALUES
(1, 1, 1, 1, 'INV-RT01-202611-0001', 11, 2026, 75000.00, 0.00, 75000.00, '2026-11-10', 'BELUM_LUNAS'),
(2, 2, 1, 1, 'INV-RT01-202611-0002', 11, 2026, 35000.00, 0.00, 35000.00, '2026-11-10', 'LUNAS'),
(3, 1, 2, 1, 'INV-RT01-202611-0003', 11, 2026, 75000.00, 0.00, 75000.00, '2026-11-10', 'LUNAS');

-- 8. Kas & Transaksi Kas
INSERT INTO `kas` (`id`, `rt_id`, `nama_kas`, `saldo_awal`, `saldo_sekarang`, `is_public_to_warga`) VALUES
(1, 1, 'Kas Utama RT 01 / RW 05', 5000000.00, 5450000.00, 1),
(2, 2, 'Kas Utama RT 02 / RW 05', 3200000.00, 3200000.00, 1);

INSERT INTO `transaksi_kas` (`kas_id`, `rt_id`, `jenis`, `kategori`, `judul`, `deskripsi`, `nominal`, `saldo_setelah`, `tanggal_transaksi`, `created_by`) VALUES
(1, 1, 'PEMASUKAN', 'IURAN_WARGA', 'Rekap Iuran Kebersihan & Keamanan', 'Penerimaan iuran warga minggu ke-1 November', 650000.00, 5650000.00, '2026-11-05', 3),
(1, 1, 'PENGELUARAN', 'KEBERSIHAN', 'Pembelian Peralatan Kerja Bakti', 'Sapu lidi, kantong sampah, dan sarung tangan', 200000.00, 5450000.00, '2026-11-08', 3);

-- 9. Template Surat
INSERT INTO `template_surat` (`id`, `kode_jenis`, `nama_surat`, `format_nomor`, `isi_template`, `persyaratan`) VALUES
(1, 'PENGANTAR_RT_RW', 'Surat Pengantar RT/RW', '{NO}/SRT-RT{RT}/RW{RW}/{BULAN}/{TAHUN}', 'Yang bertanda tangan di bawah ini Ketua RT {RT} RW {RW} {PERUMAHAN}, menerangkan bahwa:\nNama: {NAMA}\nNIK: {NIK}\nTempat/Tgl Lahir: {TTL}\nAlamat: {ALAMAT}\n\nAdalah benar warga kami yang berdomisili di wilayah tersebut di atas. Surat pengantar ini diberikan untuk keperluan: {KEPERLUAN}.', 'Fotokopi KTP dan Kartu Keluarga'),
(2, 'DOMISILI', 'Surat Keterangan Domisili', '{NO}/SKD-RT{RT}/RW{RW}/{BULAN}/{TAHUN}', 'Dengan ini Ketua RT {RT} RW {RW} menerangkan bahwa {NAMA} (NIK: {NIK}) benar berdomisili di {ALAMAT} sejak {TANGGAL_MASUK}.', 'KTP asli dan KK'),
(3, 'USAHA', 'Surat Keterangan Usaha (SKU)', '{NO}/SKU-RT{RT}/RW{RW}/{BULAN}/{TAHUN}', 'Menerangkan bahwa {NAMA} (NIK: {NIK}) benar memiliki/menjalankan usaha di lingkungan RT {RT} RW {RW} untuk keperluan: {KEPERLUAN}.', 'KTP, KK, dan foto tempat usaha'),
(4, 'TIDAK_MAMPU', 'Surat Keterangan Tidak Mampu (SKTM)', '{NO}/SKTM-RT{RT}/RW{RW}/{BULAN}/{TAHUN}', 'Menerangkan bahwa {NAMA} (NIK: {NIK}) benar warga RT {RT} RW {RW} yang tergolong keluarga kurang mampu. Surat ini dipergunakan untuk: {KEPERLUAN}.', 'KTP dan KK'),
(5, 'KEMATIAN', 'Surat Keterangan Kematian', '{NO}/SKK-RT{RT}/RW{RW}/{BULAN}/{TAHUN}', 'Menerangkan bahwa warga kami {NAMA} (NIK: {NIK}) telah meninggal dunia. Keterangan ini dibuat untuk pengurusan administrasi kependudukan.', 'KTP almarhum dan pelapor');

-- 10. Pengumuman & Kegiatan
INSERT INTO `pengumuman` (`wilayah_id`, `rw_id`, `rt_id`, `scope`, `kategori`, `judul`, `isi`, `is_pinned`, `created_by`) VALUES
(1, 1, 1, 'RT', 'KERJA_BAKTI', 'Kerja Bakti Pembersihan Saluran Air', 'Dihimbau kepada seluruh warga RT 01 untuk mengikuti kerja bakti hari Minggu pukul 07.00 WIB di area Blok A.', 1, 3),
(1, 1, NULL, 'RW', 'UMUM', 'Jadwal Fogging Nyamuk DBD RW 05', 'Pelaksanaan fogging serentak akan dilakukan oleh tim Puskesmas pada hari Sabtu mulai pukul 08.00 WIB.', 0, 2);

INSERT INTO `kegiatan` (`rt_id`, `rw_id`, `judul`, `kategori`, `deskripsi`, `lokasi`, `tanggal_mulai`, `tanggal_selesai`, `kuota_peserta`, `created_by`) VALUES
(1, 1, 'Rapat Koordinasi Bulanan RT 01', 'RAPAT', 'Evaluasi kas RT, jadwal ronda, dan persiapan HUT lingkungan.', 'Balai Warga RT 01', '2026-11-28 19:30:00', '2026-11-28 21:30:00', 50, 3),
(1, 1, 'Kerja Bakti Lingkungan Blok A', 'KERJA_BAKTI', 'Pembersihan drainase dan perapihan taman bermain.', 'Taman Blok A', '2026-11-29 07:00:00', '2026-11-29 10:00:00', 100, 3);

-- 11. App Settings
INSERT INTO `app_settings` (`setting_key`, `setting_value`, `setting_group`, `is_public`, `description`) VALUES
('app_name', 'wargaRT', 'general', 1, 'Nama resmi aplikasi'),
('payment_gateway_provider', 'midtrans', 'payment', 0, 'Provider payment gateway aktif (midtrans/xendit/tripay)'),
('payment_qris_enabled', '1', 'payment', 1, 'Aktifkan pembayaran QRIS'),
('payment_va_enabled', '1', 'payment', 1, 'Aktifkan Virtual Account'),
('fcm_enabled', '1', 'notification', 0, 'Aktifkan push notification Firebase Cloud Messaging'),
('privacy_mask_nik_for_non_admin', '1', 'security', 0, 'Masking NIK dan No KK untuk role non-berwenang');
