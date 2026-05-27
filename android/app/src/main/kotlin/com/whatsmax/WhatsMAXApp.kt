/** Application-класс: Hilt DI и каналы push-уведомлений. */
package com.whatsmax

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WhatsMAXApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    "whatsmax_messages",
                    "Сообщения",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Входящие сообщения WhatsMAX" }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    "whatsmax_calls",
                    "Звонки",
                    NotificationManager.IMPORTANCE_MAX
                ).apply { description = "Входящие звонки WhatsMAX" }
            )
        }
    }
}
