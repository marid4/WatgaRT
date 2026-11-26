package com.wargart.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wargart.data.local.dao.WargaRtDao
import com.wargart.data.local.entity.CachedPembayaranEntity
import com.wargart.data.local.entity.CachedPengumumanEntity
import com.wargart.data.local.entity.CachedWargaEntity
import com.wargart.data.local.entity.PendingActionEntity

@Database(
    entities = [
        CachedWargaEntity::class,
        CachedPengumumanEntity::class,
        CachedPembayaranEntity::class,
        PendingActionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WargaRtDatabase : RoomDatabase() {
    abstract fun dao(): WargaRtDao

    companion object {
        fun build(context: Context): WargaRtDatabase =
            Room.databaseBuilder(context.applicationContext, WargaRtDatabase::class.java, "wargart_cache.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
