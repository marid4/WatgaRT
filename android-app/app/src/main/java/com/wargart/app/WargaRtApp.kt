package com.wargart.app

import android.app.Application
import com.wargart.core.network.NetworkMonitor
import com.wargart.data.local.WargaRtDatabase
import com.wargart.data.local.datastore.SessionPreferences
import com.wargart.data.remote.RetrofitClientFactory
import com.wargart.data.repository.WargaRtRepositoryImpl
import com.wargart.domain.repository.WargaRtRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WargaRtApp : Application() {
    lateinit var repository: WargaRtRepository
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        networkMonitor = NetworkMonitor(this)
        val prefs = SessionPreferences(this, BuildConfig.BASE_API_URL)
        val db = WargaRtDatabase.build(this)
        val apiFactory = RetrofitClientFactory(prefs)
        repository = WargaRtRepositoryImpl(apiFactory, db.dao(), prefs, networkMonitor)

        // Auto-sync queued offline actions when network becomes available
        appScope.launch {
            networkMonitor.isOnlineFlow.collect { online ->
                if (online) {
                    repository.syncPendingOfflineActions()
                }
            }
        }
    }
}
