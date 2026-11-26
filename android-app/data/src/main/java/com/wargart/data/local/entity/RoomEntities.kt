package com.wargart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_warga")
data class CachedWargaEntity(
    @PrimaryKey val id: Long,
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

@Entity(tableName = "cached_pengumuman")
data class CachedPengumumanEntity(
    @PrimaryKey val id: Long,
    val scope: String,
    val kategori: String,
    val judul: String,
    val isi: String,
    val authorName: String?,
    val isPinned: Boolean,
    val publishedAt: String
)

@Entity(tableName = "cached_pembayaran")
data class CachedPembayaranEntity(
    @PrimaryKey val id: Long,
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

@Entity(tableName = "pending_offline_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val actionType: String, // CREATE_SURAT, CREATE_PENGADUAN, CHECKIN_TAMU
    val payloadJson: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
