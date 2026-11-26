package com.wargart.data.remote.api

import com.wargart.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@JvmSuppressWildcards
interface WargaRtApiService {

    @POST("index.php")
    suspend fun login(
        @Query(value = "route", encoded = true) route: String = "auth/login",
        @Body body: LoginRequestDto
    ): ApiResponse<LoginResponseDto>

    @POST("index.php")
    suspend fun register(
        @Query(value = "route", encoded = true) route: String = "auth/register",
        @Body body: RegisterRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getDashboard(
        @Query(value = "route", encoded = true) route: String = "dashboard/summary"
    ): ApiResponse<DashboardDto>

    @GET("index.php")
    suspend fun getWarga(
        @Query(value = "route", encoded = true) route: String = "warga/list",
        @Query("q") query: String = ""
    ): ApiResponse<List<WargaDto>>

    @POST("index.php")
    suspend fun createWarga(
        @Query(value = "route", encoded = true) route: String = "warga/create",
        @Body body: CreateWargaRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getKeluarga(
        @Query(value = "route", encoded = true) route: String = "kk/list"
    ): ApiResponse<List<KeluargaDto>>

    @POST("index.php")
    suspend fun createKeluarga(
        @Query(value = "route", encoded = true) route: String = "kk/create",
        @Body body: CreateKeluargaRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getKasSummary(
        @Query(value = "route", encoded = true) route: String = "kas/summary"
    ): ApiResponse<KasSummaryDto>

    @GET("index.php")
    suspend fun getKasTransactions(
        @Query(value = "route", encoded = true) route: String = "kas/transactions"
    ): ApiResponse<List<KasTransactionDto>>

    @POST("index.php")
    suspend fun createKasTransaction(
        @Query(value = "route", encoded = true) route: String = "kas/create-transaction",
        @Body body: CreateKasTxRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getIuran(
        @Query(value = "route", encoded = true) route: String = "iuran/list"
    ): ApiResponse<List<IuranDto>>

    @POST("index.php")
    suspend fun createIuran(
        @Query(value = "route", encoded = true) route: String = "iuran/create",
        @Body body: CreateIuranRequestDto
    ): ApiResponse<Any>

    @POST("index.php")
    suspend fun generateTagihan(
        @Query(value = "route", encoded = true) route: String = "iuran/generate-bills",
        @Body body: GenerateBillsRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getTagihan(
        @Query(value = "route", encoded = true) route: String = "tagihan/list"
    ): ApiResponse<List<TagihanDto>>

    @GET("index.php")
    suspend fun getPembayaran(
        @Query(value = "route", encoded = true) route: String = "pembayaran/list"
    ): ApiResponse<List<PembayaranDto>>

    @POST("index.php")
    suspend fun createPembayaran(
        @Query(value = "route", encoded = true) route: String = "pembayaran/create",
        @Body body: CreatePembayaranRequestDto
    ): ApiResponse<PembayaranDto>

    @POST("index.php")
    suspend fun verifyPembayaran(
        @Query(value = "route", encoded = true) route: String = "pembayaran/verify",
        @Body body: VerifyPembayaranRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getSuratTemplates(
        @Query(value = "route", encoded = true) route: String = "surat/templates"
    ): ApiResponse<List<SuratTemplateDto>>

    @GET("index.php")
    suspend fun getSuratList(
        @Query(value = "route", encoded = true) route: String = "surat/list"
    ): ApiResponse<List<SuratDto>>

    @POST("index.php")
    suspend fun createSurat(
        @Query(value = "route", encoded = true) route: String = "surat/create",
        @Body body: CreateSuratRequestDto
    ): ApiResponse<Any>

    @POST("index.php")
    suspend fun approveSurat(
        @Query(value = "route", encoded = true) route: String = "surat/approve",
        @Body body: ApproveSuratRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getPengumuman(
        @Query(value = "route", encoded = true) route: String = "pengumuman/list"
    ): ApiResponse<List<PengumumanDto>>

    @POST("index.php")
    suspend fun createPengumuman(
        @Query(value = "route", encoded = true) route: String = "pengumuman/create",
        @Body body: CreatePengumumanRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getPengaduan(
        @Query(value = "route", encoded = true) route: String = "pengaduan/list"
    ): ApiResponse<List<PengaduanDto>>

    @POST("index.php")
    suspend fun createPengaduan(
        @Query(value = "route", encoded = true) route: String = "pengaduan/create",
        @Body body: CreatePengaduanRequestDto
    ): ApiResponse<Any>

    @POST("index.php")
    suspend fun respondPengaduan(
        @Query(value = "route", encoded = true) route: String = "pengaduan/respond",
        @Body body: RespondPengaduanRequestDto
    ): ApiResponse<Any>

    @POST("index.php")
    suspend fun triggerSos(
        @Query(value = "route", encoded = true) route: String = "keamanan/sos",
        @Body body: SosRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getEmergencyContacts(
        @Query(value = "route", encoded = true) route: String = "keamanan/contacts"
    ): ApiResponse<EmergencyContactsDto>

    @GET("index.php")
    suspend fun getKeamananReports(
        @Query(value = "route", encoded = true) route: String = "keamanan/reports"
    ): ApiResponse<List<KeamananReportDto>>

    @GET("index.php")
    suspend fun getTamu(
        @Query(value = "route", encoded = true) route: String = "keamanan/tamu-list"
    ): ApiResponse<List<TamuDto>>

    @POST("index.php")
    suspend fun checkinTamu(
        @Query(value = "route", encoded = true) route: String = "keamanan/tamu-checkin",
        @Body body: CheckinTamuRequestDto
    ): ApiResponse<Any>

    @POST("index.php")
    suspend fun checkoutTamu(
        @Query(value = "route", encoded = true) route: String = "keamanan/tamu-checkout",
        @Body body: IdRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getKegiatan(
        @Query(value = "route", encoded = true) route: String = "kegiatan/list"
    ): ApiResponse<List<KegiatanDto>>

    @POST("index.php")
    suspend fun createKegiatan(
        @Query(value = "route", encoded = true) route: String = "kegiatan/create",
        @Body body: CreateKegiatanRequestDto
    ): ApiResponse<Any>

    @POST("index.php")
    suspend fun registerKegiatan(
        @Query(value = "route", encoded = true) route: String = "kegiatan/register",
        @Body body: RegisterKegiatanRequestDto
    ): ApiResponse<Any>

    @GET("index.php")
    suspend fun getNotifikasi(
        @Query(value = "route", encoded = true) route: String = "notifikasi/list"
    ): ApiResponse<List<NotifikasiDto>>

    @POST("index.php")
    suspend fun markNotifikasiRead(
        @Query(value = "route", encoded = true) route: String = "notifikasi/read",
        @Body body: IdRequestDto
    ): ApiResponse<Any>
}
