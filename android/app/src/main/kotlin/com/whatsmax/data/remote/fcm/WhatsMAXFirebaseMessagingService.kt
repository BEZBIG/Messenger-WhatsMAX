/** FCM-сервис: push-уведомления и обновление токена. */
package com.whatsmax.data.remote.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.whatsmax.R
import com.whatsmax.data.remote.api.ApiService
import com.whatsmax.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WhatsMAXFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var apiService: ApiService

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try { apiService.updateFcmToken(mapOf("token" to token)) }
            catch (e: Exception) { /* игнорируем */ }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "WhatsMAX"
        val body  = message.notification?.body  ?: data["body"]  ?: "Новое сообщение"
        val type  = data["type"] ?: "message"

        when (type) {
            "call"       -> showCallNotification(title, body)
            "video_call" -> showCallNotification(title, body, isVideo = true)
            else         -> showMessageNotification(title, body)
        }
    }

    private fun showMessageNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, "whatsmax_messages")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showCallNotification(title: String, body: String, isVideo: Boolean = false) {
        val notification = NotificationCompat.Builder(this, "whatsmax_calls")
            .setSmallIcon(if (isVideo) android.R.drawable.ic_menu_camera else android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1001, notification)
    }
}
