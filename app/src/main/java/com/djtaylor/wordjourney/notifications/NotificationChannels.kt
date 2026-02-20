package com.djtaylor.wordjourney.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {

    const val CHANNEL_LIVES_READY = "lives_ready"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val livesChannel = NotificationChannel(
            CHANNEL_LIVES_READY,
            "Lives Ready",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies you when all your lives have regenerated"
            enableVibration(true)
        }
        nm.createNotificationChannel(livesChannel)
    }
}
