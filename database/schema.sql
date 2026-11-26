-- ============================================================================
-- wargaRT (Management Warga Premium) - MySQL / MariaDB Database Schema
-- Compatible with MySQL 8.0+ & MariaDB 10.4+ (cPanel Shared Hosting)
-- Charset: utf8mb4_unicode_ci
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `wargart_db`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `wargart_db`;

-- ----------------------------------------------------------------------------
-- 1. roles
-- ----------------------------------------------------------------------------
CREATE TABLE `roles` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(50) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. permissions
-- ----------------------------------------------------------------------------
CREATE TABLE `permissions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(100) NOT NULL,
  `module` VARCHAR(60) NOT NULL,
  `description` VARCHAR(255) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permissions_code` (`code`),
  KEY `idx_permissions_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. wilayah
-- ----------------------------------------------------------------------------
CREATE TABLE `wilayah` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `kode_wilayah` VARCHAR(30) NOT NULL,
  `nama_perumahan` VARCHAR(150) NOT NULL,
  `kelurahan` VARCHAR(100) NOT NULL,
  `kecamatan` VARCHAR(100) NOT NULL,
  `kota_kabupaten` VARCHAR(100) NOT NULL,
  `provinsi` VARCHAR(100) NOT NULL,
  `kode_pos` VARCHAR(10) NULL,
  `alamat_sekretariat` TEXT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wilayah_kode` (`kode_wilayah`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. rw
-- ----------------------------------------------------------------------------
CREATE TABLE `rw` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `wilayah_id` BIGINT UNSIGNED NOT NULL,
  `nomor_rw` VARCHAR(10) NOT NULL,
  `nama_ketua` VARCHAR(150) NULL,
  `no_hp_ketua` VARCHAR(25) NULL,
  `alamat_sekretariat` VARCHAR(255) NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rw_wilayah_nomor` (`wilayah_id`, `nomor_rw`),
  CONSTRAINT `fk_rw_wilayah` FOREIGN KEY (`wilayah_id`) REFERENCES `wilayah` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. rt
-- ----------------------------------------------------------------------------
CREATE TABLE `rt` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `rw_id` BIGINT UNSIGNED NOT NULL,
  `nomor_rt` VARCHAR(10) NOT NULL,
  `nama_ketua` VARCHAR(150) NULL,
  `no_hp_ketua` VARCHAR(25) NULL,
  `no_hp_keamanan` VARCHAR(25) NULL,
  `alamat_sekretariat` VARCHAR(255) NULL,
  `rekening_bank` VARCHAR(100) NULL,
  `nomor_rekening` VARCHAR(50) NULL,
  `atas_nama_rekening` VARCHAR(150) NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rt_rw_nomor` (`rw_id`, `nomor_rt`),
  CONSTRAINT `fk_rt_rw` FOREIGN KEY (`rw_id`) REFERENCES `rw` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. users
-- ----------------------------------------------------------------------------
CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT UNSIGNED NOT NULL,
  `wilayah_id` BIGINT UNSIGNED NULL,
  `rw_id` BIGINT UNSIGNED NULL,
  `rt_id` BIGINT UNSIGNED NULL,
  `username` VARCHAR(60) NOT NULL,
  `email` VARCHAR(150) NOT NULL,
  `phone` VARCHAR(25) NULL,
  `full_name` VARCHAR(150) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `avatar_url` VARCHAR(255) NULL,
  `fcm_token` VARCHAR(255) NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `last_login_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`),
  KEY `idx_users_role` (`role_id`),
  KEY `idx_users_rt_rw` (`rw_id`, `rt_id`),
  CONSTRAINT `fk_users_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_users_wilayah` FOREIGN KEY (`wilayah_id`) REFERENCES `wilayah` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_users_rw` FOREIGN KEY (`rw_id`) REFERENCES `rw` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_users_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. user_permissions
-- ----------------------------------------------------------------------------
CREATE TABLE `user_permissions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `permission_id` BIGINT UNSIGNED NOT NULL,
  `granted` TINYINT(1) NOT NULL DEFAULT 1,
  `granted_by` BIGINT UNSIGNED NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_perm` (`user_id`, `permission_id`),
  CONSTRAINT `fk_up_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_up_perm` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_up_granter` FOREIGN KEY (`granted_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. keluarga (Kartu Keluarga)
-- ----------------------------------------------------------------------------
CREATE TABLE `keluarga` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `no_kk_encrypted` TEXT NOT NULL,
  `no_kk_hash` CHAR(64) NOT NULL,
  `no_kk_masked` VARCHAR(32) NOT NULL,
  `kepala_keluarga_nama` VARCHAR(150) NOT NULL,
  `alamat` TEXT NOT NULL,
  `blok_rumah` VARCHAR(50) NULL,
  `status_hunian` ENUM('TETAP','KONTRAK','SEMENTARA') NOT NULL DEFAULT 'TETAP',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keluarga_kk_hash` (`no_kk_hash`),
  KEY `idx_keluarga_rt` (`rt_id`),
  CONSTRAINT `fk_keluarga_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 9. warga
-- ----------------------------------------------------------------------------
CREATE TABLE `warga` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NULL,
  `keluarga_id` BIGINT UNSIGNED NULL,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `nik_encrypted` TEXT NOT NULL,
  `nik_hash` CHAR(64) NOT NULL,
  `nik_masked` VARCHAR(32) NOT NULL,
  `nama_lengkap` VARCHAR(150) NOT NULL,
  `tempat_lahir` VARCHAR(100) NOT NULL,
  `tanggal_lahir` DATE NOT NULL,
  `jenis_kelamin` ENUM('L','P') NOT NULL,
  `alamat_lengkap` TEXT NOT NULL,
  `no_whatsapp` VARCHAR(25) NULL,
  `status_perkawinan` ENUM('BELUM_KAWIN','KAWIN','CERAI_HIDUP','CERAI_MATI') NOT NULL DEFAULT 'BELUM_KAWIN',
  `pekerjaan` VARCHAR(100) NULL,
  `agama` VARCHAR(30) NULL DEFAULT 'Islam',
  `status_domisili` ENUM('AKTIF','PINDAH','MENINGGAL','SEMENTARA') NOT NULL DEFAULT 'AKTIF',
  `foto_profil` VARCHAR(255) NULL,
  `tanggal_masuk` DATE NULL,
  `tanggal_keluar` DATE NULL,
  `catatan` TEXT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warga_nik_hash` (`nik_hash`),
  KEY `idx_warga_rt_status` (`rt_id`, `status_domisili`),
  KEY `idx_warga_keluarga` (`keluarga_id`),
  KEY `idx_warga_nama` (`nama_lengkap`),
  CONSTRAINT `fk_warga_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_warga_keluarga` FOREIGN KEY (`keluarga_id`) REFERENCES `keluarga` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_warga_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 10. anggota_keluarga
-- ----------------------------------------------------------------------------
CREATE TABLE `anggota_keluarga` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `keluarga_id` BIGINT UNSIGNED NOT NULL,
  `warga_id` BIGINT UNSIGNED NOT NULL,
  `hubungan_keluarga` ENUM('KEPALA_KELUARGA','ISTRI','ANAK','ORANG_TUA','FAMILI_LAIN') NOT NULL,
  `tanggal_bergabung` DATE NULL,
  `tanggal_keluar` DATE NULL,
  `keterangan` VARCHAR(255) NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ak_keluarga` (`keluarga_id`),
  KEY `idx_ak_warga` (`warga_id`),
  CONSTRAINT `fk_ak_keluarga` FOREIGN KEY (`keluarga_id`) REFERENCES `keluarga` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ak_warga` FOREIGN KEY (`warga_id`) REFERENCES `warga` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 11. iuran (Master Jenis Iuran)
-- ----------------------------------------------------------------------------
CREATE TABLE `iuran` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `kode_iuran` VARCHAR(30) NOT NULL,
  `nama_iuran` VARCHAR(120) NOT NULL,
  `kategori` ENUM('BULANAN','KEAMANAN','KEBERSIHAN','SOSIAL','KHUSUS') NOT NULL DEFAULT 'BULANAN',
  `nominal` DECIMAL(14,2) NOT NULL,
  `periode` ENUM('BULANAN','TAHUNAN','SEKALI') NOT NULL DEFAULT 'BULANAN',
  `jatuh_tempo_hari` TINYINT UNSIGNED NOT NULL DEFAULT 10,
  `deskripsi` TEXT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_by` BIGINT UNSIGNED NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iuran_rt_kode` (`rt_id`, `kode_iuran`),
  CONSTRAINT `fk_iuran_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_iuran_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 12. tagihan
-- ----------------------------------------------------------------------------
CREATE TABLE `tagihan` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `iuran_id` BIGINT UNSIGNED NOT NULL,
  `keluarga_id` BIGINT UNSIGNED NOT NULL,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `nomor_tagihan` VARCHAR(50) NOT NULL,
  `periode_bulan` TINYINT UNSIGNED NOT NULL,
  `periode_tahun` SMALLINT UNSIGNED NOT NULL,
  `nominal` DECIMAL(14,2) NOT NULL,
  `denda` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `total_tagihan` DECIMAL(14,2) NOT NULL,
  `tanggal_jatuh_tempo` DATE NOT NULL,
  `status` ENUM('BELUM_LUNAS','MENUNGGU_VERIFIKASI','LUNAS','KADALUARSA') NOT NULL DEFAULT 'BELUM_LUNAS',
  `catatan` VARCHAR(255) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tagihan_nomor` (`nomor_tagihan`),
  UNIQUE KEY `uk_tagihan_unique_period` (`iuran_id`, `keluarga_id`, `periode_bulan`, `periode_tahun`),
  KEY `idx_tagihan_rt_status` (`rt_id`, `status`),
  KEY `idx_tagihan_keluarga` (`keluarga_id`),
  CONSTRAINT `fk_tagihan_iuran` FOREIGN KEY (`iuran_id`) REFERENCES `iuran` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tagihan_keluarga` FOREIGN KEY (`keluarga_id`) REFERENCES `keluarga` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tagihan_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 13. pembayaran
-- ----------------------------------------------------------------------------
CREATE TABLE `pembayaran` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tagihan_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `order_id` VARCHAR(80) NOT NULL,
  `idempotency_key` VARCHAR(80) NOT NULL,
  `metode_pembayaran` ENUM('TRANSFER_MANUAL','QRIS','VIRTUAL_ACCOUNT','TUNAI') NOT NULL,
  `channel_pembayaran` VARCHAR(60) NULL,
  `nominal_bayar` DECIMAL(14,2) NOT NULL,
  `biaya_admin` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `total_bayar` DECIMAL(14,2) NOT NULL,
  `bukti_transfer_url` VARCHAR(255) NULL,
  `gateway_ref_id` VARCHAR(120) NULL,
  `va_number` VARCHAR(60) NULL,
  `qris_payload` TEXT NULL,
  `status` ENUM('PENDING','MENUNGGU_VERIFIKASI','BERHASIL','GAGAL','DITOLAK','EXPIRED') NOT NULL DEFAULT 'PENDING',
  `verified_by` BIGINT UNSIGNED NULL,
  `verified_at` TIMESTAMP NULL DEFAULT NULL,
  `paid_at` TIMESTAMP NULL DEFAULT NULL,
  `catatan_admin` VARCHAR(255) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pembayaran_order_id` (`order_id`),
  UNIQUE KEY `uk_pembayaran_idempotency` (`idempotency_key`),
  KEY `idx_pembayaran_tagihan` (`tagihan_id`),
  KEY `idx_pembayaran_rt_status` (`rt_id`, `status`),
  CONSTRAINT `fk_pembayaran_tagihan` FOREIGN KEY (`tagihan_id`) REFERENCES `tagihan` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_pembayaran_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_pembayaran_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_pembayaran_verifier` FOREIGN KEY (`verified_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 14. payment_callbacks
-- ----------------------------------------------------------------------------
CREATE TABLE `payment_callbacks` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pembayaran_id` BIGINT UNSIGNED NULL,
  `order_id` VARCHAR(80) NOT NULL,
  `gateway_name` VARCHAR(50) NOT NULL,
  `transaction_status` VARCHAR(50) NOT NULL,
  `signature_valid` TINYINT(1) NOT NULL DEFAULT 0,
  `raw_payload` LONGTEXT NOT NULL,
  `processed` TINYINT(1) NOT NULL DEFAULT 0,
  `processed_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pc_order_id` (`order_id`),
  KEY `idx_pc_pembayaran` (`pembayaran_id`),
  CONSTRAINT `fk_pc_pembayaran` FOREIGN KEY (`pembayaran_id`) REFERENCES `pembayaran` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 15. kas
-- ----------------------------------------------------------------------------
CREATE TABLE `kas` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `nama_kas` VARCHAR(120) NOT NULL,
  `saldo_awal` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `saldo_sekarang` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `is_public_to_warga` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kas_rt` (`rt_id`),
  CONSTRAINT `fk_kas_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 16. transaksi_kas
-- ----------------------------------------------------------------------------
CREATE TABLE `transaksi_kas` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `kas_id` BIGINT UNSIGNED NOT NULL,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `pembayaran_id` BIGINT UNSIGNED NULL,
  `jenis` ENUM('PEMASUKAN','PENGELUARAN') NOT NULL,
  `kategori` VARCHAR(80) NOT NULL,
  `judul` VARCHAR(150) NOT NULL,
  `deskripsi` TEXT NULL,
  `nominal` DECIMAL(15,2) NOT NULL,
  `saldo_setelah` DECIMAL(15,2) NOT NULL,
  `tanggal_transaksi` DATE NOT NULL,
  `bukti_url` VARCHAR(255) NULL,
  `created_by` BIGINT UNSIGNED NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tk_kas_tanggal` (`kas_id`, `tanggal_transaksi`),
  KEY `idx_tk_rt_jenis` (`rt_id`, `jenis`),
  CONSTRAINT `fk_tk_kas` FOREIGN KEY (`kas_id`) REFERENCES `kas` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tk_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tk_pembayaran` FOREIGN KEY (`pembayaran_id`) REFERENCES `pembayaran` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_tk_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 17. template_surat
-- ----------------------------------------------------------------------------
CREATE TABLE `template_surat` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `kode_jenis` VARCHAR(50) NOT NULL,
  `nama_surat` VARCHAR(150) NOT NULL,
  `format_nomor` VARCHAR(100) NOT NULL DEFAULT '{NO}/RT{RT}/RW{RW}/{BULAN}/{TAHUN}',
  `isi_template` LONGTEXT NOT NULL,
  `persyaratan` TEXT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_kode` (`kode_jenis`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 18. pengajuan_surat
-- ----------------------------------------------------------------------------
CREATE TABLE `pengajuan_surat` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT UNSIGNED NOT NULL,
  `warga_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `nomor_surat` VARCHAR(80) NULL,
  `jenis_surat` VARCHAR(100) NOT NULL,
  `keperluan` TEXT NOT NULL,
  `data_tambahan_json` JSON NULL,
  `status` ENUM('DIAJUKAN','DIPROSES','DISETUJUI','DITOLAK','SELESAI') NOT NULL DEFAULT 'DIAJUKAN',
  `catatan_admin` TEXT NULL,
  `disetujui_oleh` BIGINT UNSIGNED NULL,
  `tanggal_persetujuan` TIMESTAMP NULL DEFAULT NULL,
  `pejabat_penandatangan` VARCHAR(150) NULL,
  `jabatan_penandatangan` VARCHAR(100) NULL,
  `file_pdf_url` VARCHAR(255) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ps_nomor` (`nomor_surat`),
  KEY `idx_ps_rt_status` (`rt_id`, `status`),
  KEY `idx_ps_user` (`user_id`),
  CONSTRAINT `fk_ps_template` FOREIGN KEY (`template_id`) REFERENCES `template_surat` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ps_warga` FOREIGN KEY (`warga_id`) REFERENCES `warga` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ps_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ps_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ps_approver` FOREIGN KEY (`disetujui_oleh`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 19. pengumuman
-- ----------------------------------------------------------------------------
CREATE TABLE `pengumuman` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `wilayah_id` BIGINT UNSIGNED NULL,
  `rw_id` BIGINT UNSIGNED NULL,
  `rt_id` BIGINT UNSIGNED NULL,
  `scope` ENUM('RT','RW','WILAYAH') NOT NULL DEFAULT 'RT',
  `kategori` ENUM('UMUM','KEGIATAN','KERJA_BAKTI','DARURAT') NOT NULL DEFAULT 'UMUM',
  `judul` VARCHAR(200) NOT NULL,
  `isi` LONGTEXT NOT NULL,
  `gambar_url` VARCHAR(255) NULL,
  `dokumen_url` VARCHAR(255) NULL,
  `is_pinned` TINYINT(1) NOT NULL DEFAULT 0,
  `published_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT UNSIGNED NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pengumuman_scope_rt` (`scope`, `rt_id`, `published_at`),
  CONSTRAINT `fk_pengumuman_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_pengumuman_rw` FOREIGN KEY (`rw_id`) REFERENCES `rw` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_pengumuman_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 20. pengaduan
-- ----------------------------------------------------------------------------
CREATE TABLE `pengaduan` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `kode_tiket` VARCHAR(40) NOT NULL,
  `kategori` ENUM('FASILITAS','KEAMANAN','KEBERSIHAN','SOSIAL','LAINNYA') NOT NULL,
  `judul` VARCHAR(200) NOT NULL,
  `deskripsi` TEXT NOT NULL,
  `lokasi_kejadian` VARCHAR(255) NULL,
  `foto_url` VARCHAR(255) NULL,
  `prioritas` ENUM('RENDAH','SEDANG','TINGGI','DARURAT') NOT NULL DEFAULT 'SEDANG',
  `status` ENUM('BARU','DIPROSES','SELESAI','DITOLAK') NOT NULL DEFAULT 'BARU',
  `tanggapan_admin` TEXT NULL,
  `ditanggapi_oleh` BIGINT UNSIGNED NULL,
  `ditanggapi_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pengaduan_tiket` (`kode_tiket`),
  KEY `idx_pengaduan_rt_status` (`rt_id`, `status`),
  CONSTRAINT `fk_pengaduan_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_pengaduan_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_pengaduan_responder` FOREIGN KEY (`ditanggapi_oleh`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 21. kegiatan
-- ----------------------------------------------------------------------------
CREATE TABLE `kegiatan` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `rt_id` BIGINT UNSIGNED NULL,
  `rw_id` BIGINT UNSIGNED NULL,
  `judul` VARCHAR(200) NOT NULL,
  `kategori` ENUM('RAPAT','KERJA_BAKTI','SOSIAL','KEAMANAN','LAINNYA') NOT NULL DEFAULT 'SOSIAL',
  `deskripsi` TEXT NULL,
  `lokasi` VARCHAR(255) NOT NULL,
  `tanggal_mulai` DATETIME NOT NULL,
  `tanggal_selesai` DATETIME NULL,
  `kuota_peserta` INT UNSIGNED NULL,
  `wajib_daftar` TINYINT(1) NOT NULL DEFAULT 1,
  `status` ENUM('TERJADWAL','BERLANGSUNG','SELESAI','DIBATALKAN') NOT NULL DEFAULT 'TERJADWAL',
  `created_by` BIGINT UNSIGNED NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kegiatan_rt_tanggal` (`rt_id`, `tanggal_mulai`),
  CONSTRAINT `fk_kegiatan_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_kegiatan_rw` FOREIGN KEY (`rw_id`) REFERENCES `rw` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_kegiatan_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 22. peserta_kegiatan
-- ----------------------------------------------------------------------------
CREATE TABLE `peserta_kegiatan` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `kegiatan_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `status_kehadiran` ENUM('TERDAFTAR','HADIR','TIDAK_HADIR','BATAL') NOT NULL DEFAULT 'TERDAFTAR',
  `catatan` VARCHAR(255) NULL,
  `registered_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pk_kegiatan_user` (`kegiatan_id`, `user_id`),
  CONSTRAINT `fk_pk_kegiatan` FOREIGN KEY (`kegiatan_id`) REFERENCES `kegiatan` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_pk_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 23. tamu (Buku Tamu Digital)
-- ----------------------------------------------------------------------------
CREATE TABLE `tamu` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `nama_tamu` VARCHAR(150) NOT NULL,
  `no_hp` VARCHAR(25) NULL,
  `asal_instansi` VARCHAR(150) NULL,
  `tujuan_warga_id` BIGINT UNSIGNED NULL,
  `tujuan_alamat` VARCHAR(255) NOT NULL,
  `keperluan` VARCHAR(255) NOT NULL,
  `nomor_kendaraan` VARCHAR(30) NULL,
  `waktu_masuk` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `waktu_keluar` DATETIME NULL DEFAULT NULL,
  `status` ENUM('MASUK','KELUAR') NOT NULL DEFAULT 'MASUK',
  `dicatat_oleh` BIGINT UNSIGNED NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tamu_rt_waktu` (`rt_id`, `waktu_masuk`),
  CONSTRAINT `fk_tamu_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tamu_warga` FOREIGN KEY (`tujuan_warga_id`) REFERENCES `warga` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_tamu_user` FOREIGN KEY (`dicatat_oleh`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 24. laporan_keamanan (SOS & Insiden Keamanan)
-- ----------------------------------------------------------------------------
CREATE TABLE `laporan_keamanan` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `rt_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `jenis` ENUM('SOS_DARURAT','KEJADIAN_KEAMANAN','PATROLI') NOT NULL DEFAULT 'KEJADIAN_KEAMANAN',
  `judul` VARCHAR(200) NOT NULL,
  `deskripsi` TEXT NULL,
  `lokasi` VARCHAR(255) NULL,
  `latitude` DECIMAL(10,7) NULL,
  `longitude` DECIMAL(10,7) NULL,
  `foto_url` VARCHAR(255) NULL,
  `status` ENUM('AKTIF','DITANGANI','SELESAI') NOT NULL DEFAULT 'AKTIF',
  `ditangani_oleh` BIGINT UNSIGNED NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lk_rt_status` (`rt_id`, `status`),
  CONSTRAINT `fk_lk_rt` FOREIGN KEY (`rt_id`) REFERENCES `rt` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_lk_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_lk_handler` FOREIGN KEY (`ditangani_oleh`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 25. notifikasi
-- ----------------------------------------------------------------------------
CREATE TABLE `notifikasi` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `rt_id` BIGINT UNSIGNED NULL,
  `tipe` ENUM('TAGIHAN','JATUH_TEMPO','PEMBAYARAN','SURAT','PENGUMUMAN','PENGADUAN','DARURAT') NOT NULL,
  `judul` VARCHAR(200) NOT NULL,
  `pesan` TEXT NOT NULL,
  `reference_type` VARCHAR(50) NULL,
  `reference_id` BIGINT UNSIGNED NULL,
  `is_read` TINYINT(1) NOT NULL DEFAULT 0,
  `read_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notif_user_read` (`user_id`, `is_read`, `created_at`),
  CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 26. audit_logs
-- ----------------------------------------------------------------------------
CREATE TABLE `audit_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NULL,
  `rt_id` BIGINT UNSIGNED NULL,
  `action` VARCHAR(80) NOT NULL,
  `entity_type` VARCHAR(80) NOT NULL,
  `entity_id` BIGINT UNSIGNED NULL,
  `old_values` JSON NULL,
  `new_values` JSON NULL,
  `ip_address` VARCHAR(45) NULL,
  `user_agent` VARCHAR(255) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user` (`user_id`),
  KEY `idx_audit_entity` (`entity_type`, `entity_id`),
  KEY `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 27. app_settings
-- ----------------------------------------------------------------------------
CREATE TABLE `app_settings` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `setting_key` VARCHAR(100) NOT NULL,
  `setting_value` TEXT NULL,
  `setting_group` VARCHAR(50) NOT NULL DEFAULT 'general',
  `is_public` TINYINT(1) NOT NULL DEFAULT 0,
  `description` VARCHAR(255) NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_settings_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
