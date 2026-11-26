package com.wargart.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

data class LoginRequestDto(val username: String, val password: String)

data class RegisterRequestDto(
    val username: String,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    val phone: String,
    val password: String,
    @SerializedName("rt_id") val rtId: Long = 1L
)

data class LoginResponseDto(
    val token: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val username: String,
    val email: String,
    val phone: String?,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("role_code") val roleCode: String,
    @SerializedName("role_name") val roleName: String,
    @SerializedName("rt_id") val rtId: Long?,
    @SerializedName("rw_id") val rwId: Long?
)

data class DashboardDto(
    @SerializedName("total_warga") val totalWarga: Int,
    @SerializedName("total_kk") val totalKk: Int,
    @SerializedName("total_laki") val totalLaki: Int,
    @SerializedName("total_perempuan") val totalPerempuan: Int,
    @SerializedName("total_aktif") val totalAktif: Int,
    @SerializedName("total_kas") val totalKas: Double,
    @SerializedName("tagihan_belum_lunas_count") val tagihanBelumLunasCount: Int,
    @SerializedName("tagihan_belum_lunas_nominal") val tagihanBelumLunasNominal: Double,
    @SerializedName("grafik_kas") val grafikKas: List<CashChartPointDto> = emptyList(),
    @SerializedName("notifikasi_penting") val notifikasiPenting: List<NotifikasiDto> = emptyList(),
    @SerializedName("aktivitas_terbaru") val aktivitasTerbaru: List<PengumumanDto> = emptyList()
)

data class CashChartPointDto(val bulan: String, val pemasukan: Double, val pengeluaran: Double)

data class WargaDto(
    val id: Long,
    @SerializedName("keluarga_id") val keluargaId: Long?,
    @SerializedName("rt_id") val rtId: Long,
    @SerializedName("nik_masked") val nikMasked: String,
    @SerializedName("nama_lengkap") val namaLengkap: String,
    @SerializedName("tempat_lahir") val tempatLahir: String,
    @SerializedName("tanggal_lahir") val tanggalLahir: String,
    @SerializedName("jenis_kelamin") val jenisKelamin: String,
    @SerializedName("alamat_lengkap") val alamatLengkap: String,
    @SerializedName("no_whatsapp") val noWhatsapp: String?,
    @SerializedName("status_perkawinan") val statusPerkawinan: String,
    val pekerjaan: String?,
    @SerializedName("status_domisili") val statusDomisili: String,
    @SerializedName("no_kk_masked") val noKkMasked: String?
)

data class CreateWargaRequestDto(
    val nik: String,
    @SerializedName("nama_lengkap") val namaLengkap: String,
    @SerializedName("tempat_lahir") val tempatLahir: String,
    @SerializedName("tanggal_lahir") val tanggalLahir: String,
    @SerializedName("jenis_kelamin") val jenisKelamin: String,
    @SerializedName("alamat_lengkap") val alamatLengkap: String,
    @SerializedName("no_whatsapp") val noWhatsapp: String,
    val pekerjaan: String
)

data class KeluargaDto(
    val id: Long,
    @SerializedName("rt_id") val rtId: Long,
    @SerializedName("no_kk_masked") val noKkMasked: String,
    @SerializedName("kepala_keluarga_nama") val kepalaKeluargaNama: String,
    val alamat: String,
    @SerializedName("blok_rumah") val blokRumah: String?,
    @SerializedName("status_hunian") val statusHunian: String,
    @SerializedName("jumlah_anggota") val jumlahAnggota: Int
)

data class CreateKeluargaRequestDto(
    @SerializedName("no_kk") val noKk: String,
    @SerializedName("kepala_keluarga_nama") val kepalaKeluargaNama: String,
    val alamat: String,
    @SerializedName("blok_rumah") val blokRumah: String
)

data class KasSummaryDto(val kas: List<KasDto> = emptyList())
data class KasDto(val id: Long, @SerializedName("nama_kas") val namaKas: String, @SerializedName("saldo_sekarang") val saldoSekarang: Double)

data class KasTransactionDto(
    val id: Long,
    val jenis: String,
    val kategori: String,
    val judul: String,
    val deskripsi: String?,
    val nominal: Double,
    @SerializedName("saldo_setelah") val saldoSetelah: Double,
    @SerializedName("tanggal_transaksi") val tanggalTransaksi: String
)

data class CreateKasTxRequestDto(
    val jenis: String,
    val kategori: String,
    val judul: String,
    val nominal: Double,
    val deskripsi: String
)

data class IuranDto(
    val id: Long,
    @SerializedName("kode_iuran") val kodeIuran: String,
    @SerializedName("nama_iuran") val namaIuran: String,
    val kategori: String,
    val nominal: Double,
    val periode: String
)

data class CreateIuranRequestDto(
    @SerializedName("nama_iuran") val namaIuran: String,
    val kategori: String,
    val nominal: Double
)

data class GenerateBillsRequestDto(
    @SerializedName("iuran_id") val iuranId: Long,
    val bulan: Int,
    val tahun: Int
)

data class TagihanDto(
    val id: Long,
    @SerializedName("nomor_tagihan") val nomorTagihan: String,
    @SerializedName("nama_iuran") val namaIuran: String,
    val kategori: String,
    @SerializedName("kepala_keluarga_nama") val kepalaKeluargaNama: String,
    @SerializedName("blok_rumah") val blokRumah: String?,
    @SerializedName("periode_bulan") val periodeBulan: Int,
    @SerializedName("periode_tahun") val periodeTahun: Int,
    @SerializedName("total_tagihan") val totalTagihan: Double,
    @SerializedName("tanggal_jatuh_tempo") val tanggalJatuhTempo: String,
    val status: String
)

data class PembayaranDto(
    val id: Long,
    @SerializedName("tagihan_id") val tagihanId: Long,
    @SerializedName("order_id") val orderId: String,
    @SerializedName("nomor_tagihan") val nomorTagihan: String?,
    @SerializedName("nama_iuran") val namaIuran: String?,
    @SerializedName("pembayar_nama") val pembayarNama: String?,
    @SerializedName("metode_pembayaran") val metodePembayaran: String,
    @SerializedName("channel_pembayaran") val channelPembayaran: String?,
    @SerializedName("total_bayar") val totalBayar: Double,
    @SerializedName("va_number") val vaNumber: String?,
    @SerializedName("qris_payload") val qrisPayload: String?,
    val status: String,
    @SerializedName("created_at") val createdAt: String
)

data class CreatePembayaranRequestDto(
    @SerializedName("tagihan_id") val tagihanId: Long,
    @SerializedName("metode_pembayaran") val metodePembayaran: String,
    @SerializedName("channel_pembayaran") val channelPembayaran: String,
    @SerializedName("idempotency_key") val idempotencyKey: String
)

data class VerifyPembayaranRequestDto(
    @SerializedName("pembayaran_id") val pembayaranId: Long,
    val approve: Boolean
)

data class SuratTemplateDto(val id: Long, @SerializedName("kode_jenis") val kodeJenis: String, @SerializedName("nama_surat") val namaSurat: String, val persyaratan: String?)

data class SuratDto(
    val id: Long,
    @SerializedName("nomor_surat") val nomorSurat: String?,
    @SerializedName("jenis_surat") val jenisSurat: String,
    @SerializedName("nama_lengkap") val namaLengkap: String,
    val keperluan: String,
    val status: String,
    @SerializedName("pejabat_penandatangan") val pejabatPenandatangan: String?,
    @SerializedName("created_at") val createdAt: String
)

data class CreateSuratRequestDto(@SerializedName("template_id") val templateId: Long, val keperluan: String)
data class ApproveSuratRequestDto(val id: Long, val approve: Boolean)

data class PengumumanDto(
    val id: Long,
    val scope: String?,
    val kategori: String,
    val judul: String,
    val isi: String?,
    @SerializedName("author_name") val authorName: String?,
    @SerializedName("is_pinned") val isPinned: Int?,
    @SerializedName("published_at") val publishedAt: String
)

data class CreatePengumumanRequestDto(val judul: String, val isi: String, val kategori: String, val scope: String)

data class PengaduanDto(
    val id: Long,
    @SerializedName("kode_tiket") val kodeTiket: String,
    val kategori: String,
    val judul: String,
    val deskripsi: String,
    @SerializedName("lokasi_kejadian") val lokasiKejadian: String?,
    val prioritas: String,
    val status: String,
    @SerializedName("tanggapan_admin") val tanggapanAdmin: String?,
    @SerializedName("pelapor_nama") val pelaporNama: String?,
    @SerializedName("created_at") val createdAt: String
)

data class CreatePengaduanRequestDto(
    val kategori: String,
    val judul: String,
    val deskripsi: String,
    @SerializedName("lokasi_kejadian") val lokasiKejadian: String,
    val prioritas: String
)

data class RespondPengaduanRequestDto(
    val id: Long,
    val status: String,
    @SerializedName("tanggapan_admin") val tanggapanAdmin: String
)

data class KegiatanDto(
    val id: Long,
    val judul: String,
    val kategori: String,
    val deskripsi: String?,
    val lokasi: String,
    @SerializedName("tanggal_mulai") val tanggalMulai: String,
    @SerializedName("total_peserta") val totalPeserta: Int,
    @SerializedName("sudah_daftar") val sudahDaftar: Int
)

data class CreateKegiatanRequestDto(
    val judul: String,
    val kategori: String,
    val lokasi: String,
    @SerializedName("tanggal_mulai") val tanggalMulai: String,
    val deskripsi: String
)

data class RegisterKegiatanRequestDto(@SerializedName("kegiatan_id") val kegiatanId: Long)

data class TamuDto(
    val id: Long,
    @SerializedName("nama_tamu") val namaTamu: String,
    @SerializedName("no_hp") val noHp: String?,
    @SerializedName("asal_instansi") val asalInstansi: String?,
    @SerializedName("tujuan_alamat") val tujuanAlamat: String,
    val keperluan: String,
    @SerializedName("nomor_kendaraan") val nomorKendaraan: String?,
    @SerializedName("waktu_masuk") val waktuMasuk: String,
    @SerializedName("waktu_keluar") val waktuKeluar: String?,
    val status: String
)

data class CheckinTamuRequestDto(
    @SerializedName("nama_tamu") val namaTamu: String,
    @SerializedName("no_hp") val noHp: String,
    @SerializedName("tujuan_alamat") val tujuanAlamat: String,
    val keperluan: String,
    @SerializedName("nomor_kendaraan") val nomorKendaraan: String
)

data class IdRequestDto(val id: Long)
data class SosRequestDto(val lokasi: String, val deskripsi: String)

data class KeamananReportDto(
    val id: Long,
    val jenis: String,
    val judul: String,
    val deskripsi: String?,
    val lokasi: String?,
    val status: String,
    val pelapor: String,
    @SerializedName("created_at") val createdAt: String
)

data class EmergencyContactsDto(
    @SerializedName("nomor_rt") val nomorRt: String,
    @SerializedName("ketua_rt") val ketuaRt: String?,
    @SerializedName("hp_ketua_rt") val hpKetuaRt: String?,
    @SerializedName("hp_keamanan") val hpKeamanan: String?,
    @SerializedName("nomor_rw") val nomorRw: String,
    @SerializedName("ketua_rw") val ketuaRw: String?,
    @SerializedName("hp_ketua_rw") val hpKetuaRw: String?
)

data class NotifikasiDto(
    val id: Long,
    val tipe: String,
    val judul: String,
    val pesan: String,
    @SerializedName("is_read") val isRead: Int,
    @SerializedName("created_at") val createdAt: String
)
