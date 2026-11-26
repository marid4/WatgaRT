package com.wargart.domain.model

sealed class ResourceState<out T> {
    data object Loading : ResourceState<Nothing>()
    data class Success<T>(val data: T) : ResourceState<T>()
    data class Error(val message: String) : ResourceState<Nothing>()
}

data class UserSession(
    val userId: Long,
    val username: String,
    val email: String,
    val phone: String?,
    val fullName: String,
    val roleCode: String,
    val roleName: String,
    val rtId: Long?,
    val rwId: Long?,
    val token: String
)

data class CashChartPoint(
    val bulan: String,
    val pemasukan: Double,
    val pengeluaran: Double
)

data class DashboardSummary(
    val totalWarga: Int,
    val totalKk: Int,
    val totalLaki: Int,
    val totalPerempuan: Int,
    val totalAktif: Int,
    val totalKas: Double,
    val tagihanBelumLunasCount: Int,
    val tagihanBelumLunasNominal: Double,
    val grafikKas: List<CashChartPoint>,
    val notifikasiPenting: List<NotifikasiItem>,
    val aktivitasTerbaru: List<PengumumanItem>
)

data class WargaItem(
    val id: Long,
    val keluargaId: Long?,
    val rtId: Long,
    val nikMasked: String,
    val namaLengkap: String,
    val tempatLahir: String,
    val tanggalLahir: String,
    val jenisKelamin: String,
    val alamatLengkap: String,
    val noWhatsapp: String?,
    val statusPerkawinan: String,
    val pekerjaan: String?,
    val statusDomisili: String,
    val noKkMasked: String?
)

data class KeluargaItem(
    val id: Long,
    val rtId: Long,
    val noKkMasked: String,
    val kepalaKeluargaNama: String,
    val alamat: String,
    val blokRumah: String?,
    val statusHunian: String,
    val jumlahAnggota: Int
)

data class KasTransaction(
    val id: Long,
    val jenis: String,
    val kategori: String,
    val judul: String,
    val deskripsi: String?,
    val nominal: Double,
    val saldoSetelah: Double,
    val tanggalTransaksi: String
)

data class IuranItem(
    val id: Long,
    val kodeIuran: String,
    val namaIuran: String,
    val kategori: String,
    val nominal: Double,
    val periode: String
)

data class TagihanItem(
    val id: Long,
    val nomorTagihan: String,
    val namaIuran: String,
    val kategori: String,
    val kepalaKeluargaNama: String,
    val blokRumah: String?,
    val periodeBulan: Int,
    val periodeTahun: Int,
    val totalTagihan: Double,
    val tanggalJatuhTempo: String,
    val status: String
)

data class PembayaranItem(
    val id: Long,
    val tagihanId: Long,
    val orderId: String,
    val nomorTagihan: String,
    val namaIuran: String,
    val pembayarNama: String,
    val metodePembayaran: String,
    val channelPembayaran: String?,
    val totalBayar: Double,
    val vaNumber: String?,
    val qrisPayload: String?,
    val status: String,
    val createdAt: String
)

data class SuratTemplate(
    val id: Long,
    val kodeJenis: String,
    val namaSurat: String,
    val persyaratan: String?
)

data class SuratItem(
    val id: Long,
    val nomorSurat: String?,
    val jenisSurat: String,
    val namaLengkap: String,
    val keperluan: String,
    val status: String,
    val pejabatPenandatangan: String?,
    val createdAt: String
)

data class PengumumanItem(
    val id: Long,
    val scope: String,
    val kategori: String,
    val judul: String,
    val isi: String,
    val authorName: String?,
    val isPinned: Boolean,
    val publishedAt: String
)

data class PengaduanItem(
    val id: Long,
    val kodeTiket: String,
    val kategori: String,
    val judul: String,
    val deskripsi: String,
    val lokasiKejadian: String?,
    val prioritas: String,
    val status: String,
    val tanggapanAdmin: String?,
    val pelaporNama: String?,
    val createdAt: String
)

data class KegiatanItem(
    val id: Long,
    val judul: String,
    val kategori: String,
    val deskripsi: String?,
    val lokasi: String,
    val tanggalMulai: String,
    val totalPeserta: Int,
    val sudahDaftar: Boolean
)

data class TamuItem(
    val id: Long,
    val namaTamu: String,
    val noHp: String?,
    val asalInstansi: String?,
    val tujuanAlamat: String,
    val keperluan: String,
    val nomorKendaraan: String?,
    val waktuMasuk: String,
    val waktuKeluar: String?,
    val status: String
)

data class KeamananReport(
    val id: Long,
    val jenis: String,
    val judul: String,
    val deskripsi: String?,
    val lokasi: String?,
    val status: String,
    val pelapor: String,
    val createdAt: String
)

data class EmergencyContacts(
    val nomorRt: String,
    val ketuaRt: String?,
    val hpKetuaRt: String?,
    val hpKeamanan: String?,
    val nomorRw: String,
    val ketuaRw: String?,
    val hpKetuaRw: String?
)

data class NotifikasiItem(
    val id: Long,
    val tipe: String,
    val judul: String,
    val pesan: String,
    val isRead: Boolean,
    val createdAt: String
)
