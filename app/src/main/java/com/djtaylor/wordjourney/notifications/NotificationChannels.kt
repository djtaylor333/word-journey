package com.djtaylor.wordjourney.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {

    const val CHANNEL_LIVES_READY      = "lives_ready"
    const val CHANNEL_DAILY_REMINDER   = "daily_challenge_reminder"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ── Lives Full ─────────────────────────────────────────────────────
        val livesChannel = NotificationChannel(
            CHANNEL_LIVES_READY,
            "Lives Ready",
            NotificationManager.IMPORTANCE_HIGH        // HIGH = heads-up display
        ).apply {
            description = "Notifies you when all your lives have regenerated"
            enableVibration(true)
            enableLights(true)
        }
        nm.createNotificationChannel(livesChannel)

        // ── Daily Challenge Reminder ───────────────────────────────────────
        val dailyChannel = NotificationChannel(
            CHANNEL_DAILY_REMINDER,
            "Daily Challenge Reminder",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily noon reminder to play your daily challenge"
            enableVibration(true)
            enableLights(true)
        }
        nm.createNotificationChannel(dailyChannel)
    }
}
