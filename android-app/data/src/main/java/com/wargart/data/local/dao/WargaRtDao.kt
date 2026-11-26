package com.wargart.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wargart.data.local.entity.CachedPembayaranEntity
import com.wargart.data.local.entity.CachedPengumumanEntity
import com.wargart.data.local.entity.CachedWargaEntity
import com.wargart.data.local.entity.PendingActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WargaRtDao {
    @Query("SELECT * FROM cached_warga ORDER BY namaLengkap ASC")
    suspend fun getAllWarga(): List<CachedWargaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarga(items: List<CachedWargaEntity>)

    @Query("DELETE FROM cached_warga")
    suspend fun clearWarga()

    @Query("SELECT * FROM cached_pengumuman ORDER BY isPinned DESC, publishedAt DESC")
    suspend fun getAllPengumuman(): List<CachedPengumumanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPengumuman(items: List<CachedPengumumanEntity>)

    @Query("SELECT * FROM cached_pembayaran ORDER BY createdAt DESC")
    suspend fun getAllPembayaran(): List<CachedPembayaranEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPembayaran(items: List<CachedPembayaranEntity>)

    @Query("SELECT * FROM pending_offline_actions ORDER BY createdAtMillis ASC")
    suspend fun getPendingActions(): List<PendingActionEntity>

    @Query("SELECT COUNT(*) FROM pending_offline_actions")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAction(action: PendingActionEntity)

    @Query("DELETE FROM pending_offline_actions WHERE localId = :localId")
    suspend fun deletePendingAction(localId: Long)
}
