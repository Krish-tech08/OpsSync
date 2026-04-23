package com.example.opssync.network

import com.example.opssync.data.local.TokenManager
import com.example.opssync.data.remote.AuthApi
import com.example.opssync.data.remote.EscalationApi
import com.example.opssync.data.remote.IncidentApi
import com.example.opssync.data.remote.PipelineApi
import com.example.opssync.data.remote.TeamsApi
import com.example.opssync.repository.NotificationApi
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val BASE_URL = "https://opssync-npmv.onrender.com/api/"

    private const val CONNECT_TIMEOUT = 40L
    private const val READ_TIMEOUT    = 40L
    private const val WRITE_TIMEOUT   = 40L

    // ─── Auth Interceptor ─────────────────────────────────────
    // FIX: actually reads the token and attaches it to every request
    private class AuthInterceptor(
        private val tokenManager: TokenManager
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = runBlocking { tokenManager.getToken() }  // ← was missing
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .apply {
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")  // ← was missing
                    }
                }
                .build()
            return chain.proceed(request)
        }
    }

    // ─── 401 Interceptor ──────────────────────────────────────
    private class UnauthorizedInterceptor(
        private val tokenManager: TokenManager
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            if (response.code == 401) {
                runBlocking { tokenManager.clearAuthData() }
            }
            return response
        }
    }

    // ─── Build OkHttpClient ───────────────────────────────────
    // FIX: interceptors are now actually added to the client
    fun buildOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenManager))        // ← was missing
            .addInterceptor(UnauthorizedInterceptor(tokenManager)) // ← was missing
            .addInterceptor(logging)
            .build()
    }

    // ─── Build Retrofit ───────────────────────────────────────
    private fun buildRetrofit(tokenManager: TokenManager): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(buildOkHttpClient(tokenManager))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun provideNotificationApi(tokenManager: TokenManager): NotificationApi =
        buildRetrofit(tokenManager).create(NotificationApi::class.java)

    fun provideAuthApi(tokenManager: TokenManager): AuthApi =
        buildRetrofit(tokenManager).create(AuthApi::class.java)

    fun provideIncidentApi(tokenManager: TokenManager): IncidentApi =
        buildRetrofit(tokenManager).create(IncidentApi::class.java)

    fun providePipelineApi(tokenManager: TokenManager): PipelineApi =
        buildRetrofit(tokenManager).create(PipelineApi::class.java)

    fun provideEscalationApi(tokenManager: TokenManager): EscalationApi =
        buildRetrofit(tokenManager).create(EscalationApi::class.java)

    fun provideTeamsApi(tokenManager: TokenManager): TeamsApi =
        buildRetrofit(tokenManager).create(TeamsApi::class.java)
}
