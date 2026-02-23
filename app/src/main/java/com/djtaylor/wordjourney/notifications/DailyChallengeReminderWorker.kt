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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that fires a daily notification at noon device time if:
 *  - The user has notifications enabled (notifyDailyChallenge = true)
 *  - The user has NOT yet completed today's daily challenge
 *
 * After each run, it reschedules itself for noon the following day.
 */
@HiltWorker
class DailyChallengeReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataStore: PlayerDataStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_TAG        = "daily_challenge_reminder"
        const val NOTIFICATION_ID = 1002

        /**
         * Schedules (or re-schedules) the noon daily reminder.
         * Safe to call repeatedly – cancels existing work and replaces it.
         */
        fun schedule(context: Context, notificationsEnabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(WORK_TAG)

            if (!notificationsEnabled) return

            val delayMs = msUntilNextNoon()

            val request = OneTimeWorkRequestBuilder<DailyChallengeReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG)
                .build()

            workManager.enqueue(request)
        }

        /** Milliseconds until 12:00:00 tomorrow (or today if it's before noon). */
        internal fun msUntilNextNoon(): Long {
            val now  = LocalDateTime.now()
            val noon = LocalTime.of(12, 0, 0)
            val nextNoon = if (now.toLocalTime().isBefore(noon)) {
                now.toLocalDate().atTime(noon)
            } else {
                now.toLocalDate().plusDays(1).atTime(noon)
            }
            return java.time.Duration.between(now, nextNoon).toMillis().coerceAtLeast(1_000L)
        }
    }

    override suspend fun doWork(): Result {
        val progress = dataStore.playerProgressFlow.first()

        // Bail out if user disabled this notification type
        if (!progress.notifyDailyChallenge) return Result.success()

        // Check if today's daily challenge was already completed
        val today = LocalDate.now().toString()
        val alreadyDoneToday = progress.dailyChallengeLastDate == today

        if (!alreadyDoneToday) {
            postNotification(progress.dailyChallengeStreak)
        }

        // Re-schedule for noon tomorrow regardless (so we keep firing daily)
        schedule(context, notificationsEnabled = progress.notifyDailyChallenge)

        return Result.success()
    }

    private fun postNotification(streak: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data   = android.net.Uri.parse("wordjourney://daily")
            flags  = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_daily_title)
        val body  = if (streak > 0) {
            context.getString(R.string.notification_daily_body_streak, streak)
        } else {
            context.getString(R.string.notification_daily_body_no_streak)
        }

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
}
