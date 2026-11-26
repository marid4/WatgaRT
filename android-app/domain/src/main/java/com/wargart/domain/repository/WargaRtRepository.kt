package com.wargart.domain.repository

import com.wargart.domain.model.*
import kotlinx.coroutines.flow.Flow

interface WargaRtRepository {
    val sessionFlow: Flow<UserSession?>
    val darkModeFlow: Flow<Boolean>
    val apiBaseUrlFlow: Flow<String>
    val pendingSyncCountFlow: Flow<Int>

    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setApiBaseUrl(url: String)
    suspend fun login(username: String, password: String): Result<UserSession>
    suspend fun register(username: String, email: String, fullName: String, phone: String, password: String): Result<String>
    suspend fun logout()

    suspend fun getDashboard(): Result<DashboardSummary>

    suspend fun getWarga(query: String = ""): Result<List<WargaItem>>
    suspend fun createWarga(nik: String, nama: String, tempatLahir: String, tanggalLahir: String, jk: String, alamat: String, wa: String, pekerjaan: String): Result<String>
    suspend fun getKeluarga(): Result<List<KeluargaItem>>
    suspend fun createKeluarga(noKk: String, kepala: String, alamat: String, blok: String): Result<String>

    suspend fun getKasSummary(): Result<Pair<Double, List<KasTransaction>>>
    suspend fun createKasTransaction(jenis: String, kategori: String, judul: String, nominal: Double, deskripsi: String): Result<String>
    suspend fun getIuran(): Result<List<IuranItem>>
    suspend fun createIuran(nama: String, kategori: String, nominal: Double): Result<String>
    suspend fun generateTagihan(iuranId: Long, bulan: Int, tahun: Int): Result<String>
    suspend fun getTagihan(): Result<List<TagihanItem>>

    suspend fun getPembayaran(): Result<List<PembayaranItem>>
    suspend fun createPembayaran(tagihanId: Long, metode: String, channel: String): Result<PembayaranItem>
    suspend fun verifyPembayaran(pembayaranId: Long, approve: Boolean): Result<String>

    suspend fun getSuratTemplates(): Result<List<SuratTemplate>>
    suspend fun getSuratList(): Result<List<SuratItem>>
    suspend fun createSurat(templateId: Long, keperluan: String): Result<String>
    suspend fun approveSurat(suratId: Long, approve: Boolean): Result<String>

    suspend fun getPengumuman(): Result<List<PengumumanItem>>
    suspend fun createPengumuman(judul: String, isi: String, kategori: String, scope: String): Result<String>

    suspend fun getPengaduan(): Result<List<PengaduanItem>>
    suspend fun createPengaduan(kategori: String, judul: String, deskripsi: String, lokasi: String, prioritas: String): Result<String>
    suspend fun respondPengaduan(id: Long, status: String, tanggapan: String): Result<String>

    suspend fun triggerSos(lokasi: String): Result<String>
    suspend fun getEmergencyContacts(): Result<EmergencyContacts>
    suspend fun getKeamananReports(): Result<List<KeamananReport>>
    suspend fun getTamu(): Result<List<TamuItem>>
    suspend fun checkinTamu(nama: String, hp: String, tujuan: String, keperluan: String, nopol: String): Result<String>
    suspend fun checkoutTamu(id: Long): Result<String>

    suspend fun getKegiatan(): Result<List<KegiatanItem>>
    suspend fun createKegiatan(judul: String, kategori: String, lokasi: String, tanggalMulai: String, deskripsi: String): Result<String>
    suspend fun registerKegiatan(kegiatanId: Long): Result<String>

    suspend fun getNotifikasi(): Result<List<NotifikasiItem>>
    suspend fun markNotifikasiRead(id: Long? = null): Result<Unit>

    suspend fun syncPendingOfflineActions(): Int
}
