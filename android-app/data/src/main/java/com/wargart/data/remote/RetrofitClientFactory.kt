package com.wargart.data.remote

import com.wargart.data.local.datastore.SessionPreferences
import com.wargart.data.remote.api.WargaRtApiService
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClientFactory(private val prefs: SessionPreferences) {
    @Volatile
    private var cachedBaseUrl: String? = null
    @Volatile
    private var cachedService: WargaRtApiService? = null

    fun getService(): WargaRtApiService {
        val url = runBlocking { prefs.getApiBaseUrl() }
        val normalized = if (url.endsWith("/")) url else "$url/"
        val baseForRetrofit = normalized.removeSuffix("index.php/")
        if (cachedService != null && cachedBaseUrl == baseForRetrofit) {
            return cachedService!!
        }

        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { prefs.getToken() }
            val req = chain.request().newBuilder().apply {
                addHeader("Accept", "application/json")
                if (!token.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }.build()
            chain.proceed(req)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseForRetrofit)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WargaRtApiService::class.java)
        cachedBaseUrl = baseForRetrofit
        cachedService = service
        return service
    }
}
