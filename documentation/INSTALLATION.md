# Panduan Instalasi & Build wargaRT

## 1. Instalasi Database MySQL di cPanel
1. Buka **cPanel -> MySQL Databases**, buat database (misal `wargart_db`) dan user database dengan hak akses **ALL PRIVILEGES**.
2. Buka **phpMyAdmin**, pilih database tersebut, lalu import:
   - `database/schema.sql`
   - `database/seed.sql`

### Akun Demo Awal (Password: `WargaRT@2026` / hash Laravel-compatible demo `password`)
> Catatan: Hash default pada `seed.sql` menggunakan hash standar `$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi` (password: `password`). Segera ubah password setelah instalasi produksi.
- **Super Admin**: `superadmin`
- **Admin RW 05**: `adminrw05`
- **Admin RT 01**: `adminrt01`
- **Warga**: `warga01`

## 2. Instalasi Backend PHP 8+ di cPanel Shared Hosting
1. Upload seluruh isi folder `backend/` ke direktori domain/subdomain Anda (misal `public_html/wargart/`).
2. Salin `backend/config/.env.example` menjadi `backend/config/.env` dan sesuaikan:
   - `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASS`
   - `JWT_SECRET` (gunakan string acak 64 karakter)
   - `DATA_ENCRYPTION_KEY` (base64 32-byte untuk enkripsi NIK & No KK)
   - `PAYMENT_SERVER_KEY` & `FCM_SERVER_KEY` (opsional untuk integrasi gateway & push notification)
3. Pastikan folder `backend/uploads/` memiliki izin tulis (`755`).
4. Akses Web Admin Panel melalui: `https://domainanda.com/admin/login.php`
5. Uji API melalui: `https://domainanda.com/api/index.php?route=health`

## 3. Konfigurasi API URL pada Aplikasi Android
1. Buka `android-app/app/build.gradle.kts` dan ubah:
   ```kotlin
   buildConfigField("String", "BASE_API_URL", "\"https://domainanda.com/api/index.php\"")
   ```
2. Atau ubah langsung dari dalam aplikasi pada menu **Pengaturan -> Konfigurasi URL Backend API**.

## 4. Panduan Build APK (Debug & Signed Release)

### Melalui Android Studio
1. Buka folder `android-app/` di Android Studio (Ladybug atau terbaru).
2. Tunggu Gradle Sync selesai.
3. Build Debug APK: **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
4. Build Release APK (sudah terkonfigurasi dengan `android-app/keystore/wargart-release.jks`):
   Jalankan di Terminal:
   ```bash
   ./gradlew assembleDebug assembleRelease
   ```
   Output APK berada di:
   - `app/build/outputs/apk/debug/app-debug.apk`
   - `app/build/outputs/apk/release/app-release.apk`

### Melalui AndroidIDE (di Perangkat Android)
1. Ekstrak `android-app/` ke penyimpanan internal (misal `/storage/emulated/0/Projects/wargaRT`).
2. Pastikan `local.properties` tetap tanpa `sdk.dir` hardcoded agar AndroidIDE mendeteksi SDK lokal otomatis.
3. Buka proyek di AndroidIDE, jalankan Gradle Sync, lalu pilih **Build -> Build APK (Debug/Release)**.
