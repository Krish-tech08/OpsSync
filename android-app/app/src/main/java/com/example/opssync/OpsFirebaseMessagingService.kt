package com.example.opssync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OpsFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID   = "opssync_alerts"
        const val CHANNEL_NAME = "OpsSync Alerts"
    }

    // ── Called when a new FCM token is generated ──────────────
    // This fires on first install and whenever FCM rotates the token.
    // We save it locally and register it with our backend.
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tokenManager = com.example.opssync.data.local.TokenManager(applicationContext)
                tokenManager.saveFcmToken(token)
                // Register with backend if user is already logged in
                val jwt = tokenManager.getToken()
                if (jwt != null) {
                    com.example.opssync.network.FcmRegistrationHelper
                        .registerToken(applicationContext, token)
                }
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Token registration failed: ${e.message}")
            }
        }
    }

    // ── Called when a notification arrives while app is in foreground ──
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "OpsSync"
        val body  = remoteMessage.notification?.body  ?: remoteMessage.data["body"]  ?: ""
        val type  = remoteMessage.data["type"]       ?: ""
        val id    = remoteMessage.data["incidentId"] ?: remoteMessage.data["pipelineId"] ?: ""

        showNotification(title, body, type, id)
    }

    private fun showNotification(title: String, body: String, type: String, id: String) {
        createNotificationChannel()

        // Deep link intent — tapping opens the correct screen
        val deepLinkIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            putExtra("notification_id",   id)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description       = "Pipeline failures and incident alerts"
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
