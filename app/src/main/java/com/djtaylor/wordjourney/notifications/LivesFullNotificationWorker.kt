package com.djtaylor.wordjourney.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.djtaylor.wordjourney.MainActivity
import com.djtaylor.wordjourney.R
import com.djtaylor.wordjourney.data.datastore.PlayerDataStore
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class LivesFullNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataStore: PlayerDataStore,
    private val lifeRegenUseCase: LifeRegenUseCase
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_TAG = "lives_full_notification"
        const val NOTIFICATION_ID = 1001

        /**
         * Schedules (or re-schedules) the notification worker to fire when
         * lives will reach 10, based on current state.
         * Safe to call repeatedly — cancels any existing work and replaces it.
         */
        fun schedule(
            context: Context,
            currentLives: Int,
            lastRegenTimestamp: Long,
            notificationsEnabled: Boolean
        ) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(WORK_TAG)

            if (!notificationsEnabled || currentLives >= LifeRegenUseCase.TIME_REGEN_CAP) return

            val regenUseCase = LifeRegenUseCase()
            val delayMs = regenUseCase.msUntilFull(
                currentLives, lastRegenTimestamp
            ).coerceAtLeast(1_000L)

            val request = OneTimeWorkRequestBuilder<LivesFullNotificationWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG)
                .build()

            workManager.enqueue(request)
        }

        /**
         * [DEV] Immediately posts the lives-full notification without scheduling
         * through WorkManager. Bypasses all condition checks — for testing only.
         */
        fun devDirectFire(context: Context) {
            val intent = android.content.Intent(context, MainActivity::class.java).apply {
                action = android.content.Intent.ACTION_VIEW
                data   = android.net.Uri.parse("wordjourney://home")
                flags  = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                         android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val title = context.getString(R.string.notification_lives_full_title)
            val body  = context.getString(R.string.notification_lives_full_body)
            val notification = androidx.core.app.NotificationCompat
                .Builder(context, NotificationChannels.CHANNEL_LIVES_READY)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_REMINDER)
                .setVibrate(longArrayOf(0, 300, 100, 300))
                .build()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as
                android.app.NotificationManager
            nm.notify(NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result {
        val progress = dataStore.playerProgressFlow.first()

        // Re-calculate regen to grant lives and write to DataStore if needed
        val regen = lifeRegenUseCase(
            progress.lives,
            progress.lastLifeRegenTimestamp
        )

        val updatedProgress = progress.copy(
            lives = regen.updatedLives,
            lastLifeRegenTimestamp = regen.updatedTimestamp
        )
        dataStore.savePlayerProgress(updatedProgress)

        // Only notify if lives actually reached the cap now
        if (updatedProgress.lives >= LifeRegenUseCase.TIME_REGEN_CAP) {
            postNotification()
        }

        return Result.success()
    }

    private fun postNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("wordjourney://home")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_lives_full_title)
        val body  = context.getString(R.string.notification_lives_full_body)

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_LIVES_READY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
}
