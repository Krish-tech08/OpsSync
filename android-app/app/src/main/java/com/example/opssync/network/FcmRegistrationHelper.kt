package com.example.opssync.network

import android.content.Context
import com.example.opssync.data.local.TokenManager
import com.example.opssync.data.models.FcmTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FcmRegistrationHelper {

    // Registers the FCM token with your backend.
    // Called on app launch and whenever FCM rotates the token.
    suspend fun registerToken(context: Context, fcmToken: String) {
        withContext(Dispatchers.IO) {
            try {
                val tokenManager = TokenManager(context)
                val notificationApi = NetworkModule.provideNotificationApi(tokenManager)
                notificationApi.registerFcmToken(FcmTokenRequest(fcmToken = fcmToken))
            } catch (e: Exception) {
                android.util.Log.e("FCM", "registerToken failed: ${e.message}")
            }
        }
    }

    suspend fun removeToken(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val tokenManager = TokenManager(context)
                val notificationApi = NetworkModule.provideNotificationApi(tokenManager)
                notificationApi.removeFcmToken()
            } catch (e: Exception) {
                android.util.Log.e("FCM", "removeToken failed: ${e.message}")
            }
        }
    }
}
