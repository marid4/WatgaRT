package com.wargart.data.repository

import com.google.gson.Gson
import com.wargart.core.network.NetworkMonitor
import com.wargart.data.local.dao.WargaRtDao
import com.wargart.data.local.datastore.SessionPreferences
import com.wargart.data.local.entity.CachedPembayaranEntity
import com.wargart.data.local.entity.CachedPengumumanEntity
import com.wargart.data.local.entity.CachedWargaEntity
import com.wargart.data.local.entity.PendingActionEntity
import com.wargart.data.remote.RetrofitClientFactory
import com.wargart.data.remote.dto.*
import com.wargart.domain.model.*
import com.wargart.domain.repository.WargaRtRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class WargaRtRepositoryImpl(
    private val apiFactory: RetrofitClientFactory,
    private val dao: WargaRtDao,
    private val prefs: SessionPreferences,
    private val networkMonitor: NetworkMonitor
) : WargaRtRepository {

    private val gson = Gson()
    private fun api() = apiFactory.getService()

    override val sessionFlow: Flow<UserSession?> = prefs.sessionFlow
    override val darkModeFlow: Flow<Boolean> = prefs.darkModeFlow
    override val apiBaseUrlFlow: Flow<String> = prefs.apiBaseUrlFlow
    override val pendingSyncCountFlow: Flow<Int> = dao.observePendingCount()

    override suspend fun setDarkMode(enabled: Boolean) = prefs.setDarkMode(enabled)
    override suspend fun setApiBaseUrl(url: String) = prefs.setApiBaseUrl(url)

    override suspend fun login(username: String, password: String): Result<UserSession> = runCatching {
        val resp = api().login(body = LoginRequestDto(username, password))
        if (!resp.success || resp.data == null) error(resp.message ?: "Login gagal")
        val d = resp.data
        val session = UserSession(
            userId = d.user.id,
            username = d.user.username,
            email = d.user.email,
            phone = d.user.phone,
            fullName = d.user.fullName,
            roleCode = d.user.roleCode,
            roleName = d.user.roleName,
            rtId = d.user.rtId,
            rwId = d.user.rwId,
            token = d.token
        )
        prefs.saveSession(session)
        session
    }

    override suspend fun register(username: String, email: String, fullName: String, phone: String, password: String): Result<String> = runCatching {
        val resp = api().register(body = RegisterRequestDto(username, email, fullName, phone, password))
        if (!resp.success) error(resp.message ?: "Registrasi gagal")
        resp.message ?: "Registrasi berhasil"
    }

    override suspend fun logout() {
        prefs.clearSession()
    }

    override suspend fun getDashboard(): Result<DashboardSummary> = runCatching {
        val resp = api().getDashboard()
        val d = resp.data ?: error(resp.message ?: "Gagal memuat dashboard")
        DashboardSummary(
            totalWarga = d.totalWarga,
            totalKk = d.totalKk,
            totalLaki = d.totalLaki,
            totalPerempuan = d.totalPerempuan,
            totalAktif = d.totalAktif,
            totalKas = d.totalKas,
            tagihanBelumLunasCount = d.tagihanBelumLunasCount,
            tagihanBelumLunasNominal = d.tagihanBelumLunasNominal,
            grafikKas = d.grafikKas.map { CashChartPoint(it.bulan, it.pemasukan, it.pengeluaran) },
            notifikasiPenting = d.notifikasiPenting.map {
                NotifikasiItem(it.id, it.tipe, it.judul, it.pesan, it.isRead == 1, it.createdAt)
            },
            aktivitasTerbaru = d.aktivitasTerbaru.map {
                PengumumanItem(it.id, it.scope ?: "RT", it.kategori, it.judul, it.isi ?: "", it.authorName, it.isPinned == 1, it.publishedAt)
            }
        )
    }

    override suspend fun getWarga(query: String): Result<List<WargaItem>> = runCatching {
        if (networkMonitor.isCurrentlyOnline()) {
            val resp = api().getWarga(query = query)
            val list = (resp.data ?: emptyList()).map {
                WargaItem(
                    it.id, it.keluargaId, it.rtId, it.nikMasked, it.namaLengkap,
                    it.tempatLahir, it.tanggalLahir, it.jenisKelamin, it.alamatLengkap,
                    it.noWhatsapp, it.statusPerkawinan, it.pekerjaan, it.statusDomisili, it.noKkMasked
                )
            }
            if (query.isBlank()) {
                dao.clearWarga()
                dao.insertWarga(list.map {
                    CachedWargaEntity(
                        it.id, it.rtId, it.nikMasked, it.namaLengkap, it.tempatLahir,
                        it.tanggalLahir, it.jenisKelamin, it.alamatLengkap, it.noWhatsapp,
                        it.statusPerkawinan, it.pekerjaan, it.statusDomisili, it.noKkMasked
                    )
                })
            }
            list
        } else {
            dao.getAllWarga().map {
                WargaItem(
                    it.id, null, it.rtId, it.nikMasked, it.namaLengkap,
                    it.tempatLahir, it.tanggalLahir, it.jenisKelamin, it.alamatLengkap,
                    it.noWhatsapp, it.statusPerkawinan, it.pekerjaan, it.statusDomisili, it.noKkMasked
                )
            }
        }
    }

    override suspend fun createWarga(nik: String, nama: String, tempatLahir: String, tanggalLahir: String, jk: String, alamat: String, wa: String, pekerjaan: String): Result<String> = runCatching {
        val resp = api().createWarga(body = CreateWargaRequestDto(nik, nama, tempatLahir, tanggalLahir, jk, alamat, wa, pekerjaan))
        if (!resp.success) error(resp.message ?: "Gagal menyimpan warga")
        resp.message ?: "Data warga tersimpan"
    }

    override suspend fun getKeluarga(): Result<List<KeluargaItem>> = runCatching {
        val resp = api().getKeluarga()
        (resp.data ?: emptyList()).map {
            KeluargaItem(it.id, it.rtId, it.noKkMasked, it.kepalaKeluargaNama, it.alamat, it.blokRumah, it.statusHunian, it.jumlahAnggota)
        }
    }

    override suspend fun createKeluarga(noKk: String, kepala: String, alamat: String, blok: String): Result<String> = runCatching {
        val resp = api().createKeluarga(body = CreateKeluargaRequestDto(noKk, kepala, alamat, blok))
        if (!resp.success) error(resp.message ?: "Gagal menambah KK")
        resp.message ?: "Kartu Keluarga ditambahkan"
    }

    override suspend fun getKasSummary(): Result<Pair<Double, List<KasTransaction>>> = runCatching {
        val sum = api().getKasSummary().data
        val saldo = sum?.kas?.sumOf { it.saldoSekarang } ?: 0.0
        val txs = (api().getKasTransactions().data ?: emptyList()).map {
            KasTransaction(it.id, it.jenis, it.kategori, it.judul, it.deskripsi, it.nominal, it.saldoSetelah, it.tanggalTransaksi)
        }
        saldo to txs
    }

    override suspend fun createKasTransaction(jenis: String, kategori: String, judul: String, nominal: Double, deskripsi: String): Result<String> = runCatching {
        val resp = api().createKasTransaction(body = CreateKasTxRequestDto(jenis, kategori, judul, nominal, deskripsi))
        if (!resp.success) error(resp.message ?: "Gagal mencatat kas")
        resp.message ?: "Transaksi kas tercatat"
    }

    override suspend fun getIuran(): Result<List<IuranItem>> = runCatching {
        (api().getIuran().data ?: emptyList()).map {
            IuranItem(it.id, it.kodeIuran, it.namaIuran, it.kategori, it.nominal, it.periode)
        }
    }

    override suspend fun createIuran(nama: String, kategori: String, nominal: Double): Result<String> = runCatching {
        val resp = api().createIuran(body = CreateIuranRequestDto(nama, kategori, nominal))
        if (!resp.success) error(resp.message ?: "Gagal membuat iuran")
        resp.message ?: "Jenis iuran dibuat"
    }

    override suspend fun generateTagihan(iuranId: Long, bulan: Int, tahun: Int): Result<String> = runCatching {
        val resp = api().generateTagihan(body = GenerateBillsRequestDto(iuranId, bulan, tahun))
        if (!resp.success) error(resp.message ?: "Gagal menerbitkan tagihan")
        resp.message ?: "Tagihan diterbitkan"
    }

    override suspend fun getTagihan(): Result<List<TagihanItem>> = runCatching {
        (api().getTagihan().data ?: emptyList()).map {
            TagihanItem(it.id, it.nomorTagihan, it.namaIuran, it.kategori, it.kepalaKeluargaNama, it.blokRumah, it.periodeBulan, it.periodeTahun, it.totalTagihan, it.tanggalJatuhTempo, it.status)
        }
    }

    override suspend fun getPembayaran(): Result<List<PembayaranItem>> = runCatching {
        if (networkMonitor.isCurrentlyOnline()) {
            val list = (api().getPembayaran().data ?: emptyList()).map {
                PembayaranItem(it.id, it.tagihanId, it.orderId, it.nomorTagihan ?: "-", it.namaIuran ?: "-", it.pembayarNama ?: "-", it.metodePembayaran, it.channelPembayaran, it.totalBayar, it.vaNumber, it.qrisPayload, it.status, it.createdAt)
            }
            dao.insertPembayaran(list.map {
                CachedPembayaranEntity(it.id, it.tagihanId, it.orderId, it.nomorTagihan, it.namaIuran, it.pembayarNama, it.metodePembayaran, it.channelPembayaran, it.totalBayar, it.vaNumber, it.qrisPayload, it.status, it.createdAt)
            })
            list
        } else {
            dao.getAllPembayaran().map {
                PembayaranItem(it.id, it.tagihanId, it.orderId, it.nomorTagihan, it.namaIuran, it.pembayarNama, it.metodePembayaran, it.channelPembayaran, it.totalBayar, it.vaNumber, it.qrisPayload, it.status, it.createdAt)
            }
        }
    }

    override suspend fun createPembayaran(tagihanId: Long, metode: String, channel: String): Result<PembayaranItem> = runCatching {
        if (!networkMonitor.isCurrentlyOnline()) {
            error("Pembayaran online memerlukan koneksi internet aktif untuk mencegah transaksi ganda.")
        }
        val idempotencyKey = "${tagihanId}_${metode}_${UUID.randomUUID().toString().take(8)}"
        val resp = api().createPembayaran(body = CreatePembayaranRequestDto(tagihanId, metode, channel, idempotencyKey))
        val it = resp.data ?: error(resp.message ?: "Gagal membuat pembayaran")
        PembayaranItem(it.id, it.tagihanId, it.orderId, it.nomorTagihan ?: "-", it.namaIuran ?: "-", it.pembayarNama ?: "-", it.metodePembayaran, it.channelPembayaran, it.totalBayar, it.vaNumber, it.qrisPayload, it.status, it.createdAt)
    }

    override suspend fun verifyPembayaran(pembayaranId: Long, approve: Boolean): Result<String> = runCatching {
        val resp = api().verifyPembayaran(body = VerifyPembayaranRequestDto(pembayaranId, approve))
        if (!resp.success) error(resp.message ?: "Gagal verifikasi")
        resp.message ?: "Pembayaran diverifikasi"
    }

    override suspend fun getSuratTemplates(): Result<List<SuratTemplate>> = runCatching {
        (api().getSuratTemplates().data ?: emptyList()).map { SuratTemplate(it.id, it.kodeJenis, it.namaSurat, it.persyaratan) }
    }

    override suspend fun getSuratList(): Result<List<SuratItem>> = runCatching {
        (api().getSuratList().data ?: emptyList()).map {
            SuratItem(it.id, it.nomorSurat, it.jenisSurat, it.namaLengkap, it.keperluan, it.status, it.pejabatPenandatangan, it.createdAt)
        }
    }

    override suspend fun createSurat(templateId: Long, keperluan: String): Result<String> = runCatching {
        val req = CreateSuratRequestDto(templateId, keperluan)
        if (!networkMonitor.isCurrentlyOnline()) {
            dao.insertPendingAction(PendingActionEntity(actionType = "CREATE_SURAT", payloadJson = gson.toJson(req)))
            return@runCatching "Disimpan offline. Akan disinkronkan saat internet tersedia."
        }
        val resp = api().createSurat(body = req)
        if (!resp.success) error(resp.message ?: "Gagal mengajukan surat")
        resp.message ?: "Pengajuan surat dikirim"
    }

    override suspend fun approveSurat(suratId: Long, approve: Boolean): Result<String> = runCatching {
        val resp = api().approveSurat(body = ApproveSuratRequestDto(suratId, approve))
        if (!resp.success) error(resp.message ?: "Gagal memproses surat")
        resp.message ?: "Status surat diperbarui"
    }

    override suspend fun getPengumuman(): Result<List<PengumumanItem>> = runCatching {
        if (networkMonitor.isCurrentlyOnline()) {
            val list = (api().getPengumuman().data ?: emptyList()).map {
                PengumumanItem(it.id, it.scope ?: "RT", it.kategori, it.judul, it.isi ?: "", it.authorName, it.isPinned == 1, it.publishedAt)
            }
            dao.insertPengumuman(list.map {
                CachedPengumumanEntity(it.id, it.scope, it.kategori, it.judul, it.isi, it.authorName, it.isPinned, it.publishedAt)
            })
            list
        } else {
            dao.getAllPengumuman().map {
                PengumumanItem(it.id, it.scope, it.kategori, it.judul, it.isi, it.authorName, it.isPinned, it.publishedAt)
            }
        }
    }

    override suspend fun createPengumuman(judul: String, isi: String, kategori: String, scope: String): Result<String> = runCatching {
        val resp = api().createPengumuman(body = CreatePengumumanRequestDto(judul, isi, kategori, scope))
        if (!resp.success) error(resp.message ?: "Gagal membuat pengumuman")
        resp.message ?: "Pengumuman dipublikasikan"
    }

    override suspend fun getPengaduan(): Result<List<PengaduanItem>> = runCatching {
        (api().getPengaduan().data ?: emptyList()).map {
            PengaduanItem(it.id, it.kodeTiket, it.kategori, it.judul, it.deskripsi, it.lokasiKejadian, it.prioritas, it.status, it.tanggapanAdmin, it.pelaporNama, it.createdAt)
        }
    }

    override suspend fun createPengaduan(kategori: String, judul: String, deskripsi: String, lokasi: String, prioritas: String): Result<String> = runCatching {
        val req = CreatePengaduanRequestDto(kategori, judul, deskripsi, lokasi, prioritas)
        if (!networkMonitor.isCurrentlyOnline()) {
            dao.insertPendingAction(PendingActionEntity(actionType = "CREATE_PENGADUAN", payloadJson = gson.toJson(req)))
            return@runCatching "Pengaduan disimpan offline & siap disinkronkan."
        }
        val resp = api().createPengaduan(body = req)
        if (!resp.success) error(resp.message ?: "Gagal mengirim pengaduan")
        resp.message ?: "Pengaduan terkirim"
    }

    override suspend fun respondPengaduan(id: Long, status: String, tanggapan: String): Result<String> = runCatching {
        val resp = api().respondPengaduan(body = RespondPengaduanRequestDto(id, status, tanggapan))
        if (!resp.success) error(resp.message ?: "Gagal menanggapi")
        resp.message ?: "Tanggapan tersimpan"
    }

    override suspend fun triggerSos(lokasi: String): Result<String> = runCatching {
        val resp = api().triggerSos(body = SosRequestDto(lokasi, "Panggilan darurat SOS dari aplikasi wargaRT"))
        if (!resp.success) error(resp.message ?: "Gagal mengirim SOS")
        resp.message ?: "Sinyal SOS terkirim!"
    }

    override suspend fun getEmergencyContacts(): Result<EmergencyContacts> = runCatching {
        val d = api().getEmergencyContacts().data ?: error("Kontak darurat kosong")
        EmergencyContacts(d.nomorRt, d.ketuaRt, d.hpKetuaRt, d.hpKeamanan, d.nomorRw, d.ketuaRw, d.hpKetuaRw)
    }

    override suspend fun getKeamananReports(): Result<List<KeamananReport>> = runCatching {
        (api().getKeamananReports().data ?: emptyList()).map {
            KeamananReport(it.id, it.jenis, it.judul, it.deskripsi, it.lokasi, it.status, it.pelapor, it.createdAt)
        }
    }

    override suspend fun getTamu(): Result<List<TamuItem>> = runCatching {
        (api().getTamu().data ?: emptyList()).map {
            TamuItem(it.id, it.namaTamu, it.noHp, it.asalInstansi, it.tujuanAlamat, it.keperluan, it.nomorKendaraan, it.waktuMasuk, it.waktuKeluar, it.status)
        }
    }

    override suspend fun checkinTamu(nama: String, hp: String, tujuan: String, keperluan: String, nopol: String): Result<String> = runCatching {
        val req = CheckinTamuRequestDto(nama, hp, tujuan, keperluan, nopol)
        if (!networkMonitor.isCurrentlyOnline()) {
            dao.insertPendingAction(PendingActionEntity(actionType = "CHECKIN_TAMU", payloadJson = gson.toJson(req)))
            return@runCatching "Tamu disimpan offline."
        }
        val resp = api().checkinTamu(body = req)
        if (!resp.success) error(resp.message ?: "Gagal mencatat tamu")
        resp.message ?: "Tamu dicatat masuk"
    }

    override suspend fun checkoutTamu(id: Long): Result<String> = runCatching {
        val resp = api().checkoutTamu(body = IdRequestDto(id))
        if (!resp.success) error(resp.message ?: "Gagal checkout tamu")
        resp.message ?: "Tamu tercatat keluar"
    }

    override suspend fun getKegiatan(): Result<List<KegiatanItem>> = runCatching {
        (api().getKegiatan().data ?: emptyList()).map {
            KegiatanItem(it.id, it.judul, it.kategori, it.deskripsi, it.lokasi, it.tanggalMulai, it.totalPeserta, it.sudahDaftar == 1)
        }
    }

    override suspend fun createKegiatan(judul: String, kategori: String, lokasi: String, tanggalMulai: String, deskripsi: String): Result<String> = runCatching {
        val resp = api().createKegiatan(body = CreateKegiatanRequestDto(judul, kategori, lokasi, tanggalMulai, deskripsi))
        if (!resp.success) error(resp.message ?: "Gagal membuat kegiatan")
        resp.message ?: "Kegiatan dijadwalkan"
    }

    override suspend fun registerKegiatan(kegiatanId: Long): Result<String> = runCatching {
        val resp = api().registerKegiatan(body = RegisterKegiatanRequestDto(kegiatanId))
        if (!resp.success) error(resp.message ?: "Gagal mendaftar kegiatan")
        resp.message ?: "Berhasil terdaftar"
    }

    override suspend fun getNotifikasi(): Result<List<NotifikasiItem>> = runCatching {
        (api().getNotifikasi().data ?: emptyList()).map {
            NotifikasiItem(it.id, it.tipe, it.judul, it.pesan, it.isRead == 1, it.createdAt)
        }
    }

    override suspend fun markNotifikasiRead(id: Long?): Result<Unit> = runCatching {
        api().markNotifikasiRead(body = IdRequestDto(id ?: 0L))
    }

    override suspend fun syncPendingOfflineActions(): Int {
        if (!networkMonitor.isCurrentlyOnline()) return 0
        val pending = dao.getPendingActions()
        var synced = 0
        for (item in pending) {
            val ok = runCatching {
                when (item.actionType) {
                    "CREATE_SURAT" -> api().createSurat(body = gson.fromJson(item.payloadJson, CreateSuratRequestDto::class.java)).success
                    "CREATE_PENGADUAN" -> api().createPengaduan(body = gson.fromJson(item.payloadJson, CreatePengaduanRequestDto::class.java)).success
                    "CHECKIN_TAMU" -> api().checkinTamu(body = gson.fromJson(item.payloadJson, CheckinTamuRequestDto::class.java)).success
                    else -> true
                }
            }.getOrDefault(false)
            if (ok) {
                dao.deletePendingAction(item.localId)
                synced++
            }
        }
        return synced
    }
}
