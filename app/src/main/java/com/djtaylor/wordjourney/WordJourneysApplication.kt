package com.djtaylor.wordjourney

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.djtaylor.wordjourney.notifications.NotificationChannels
import com.google.android.gms.games.PlayGamesSdk
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WordJourneysApplication : Application(), Configuration.Provider {

    // Injected after Hilt component is initialised (before any Worker is created)
    @Inject lateinit var workerFactory: HiltWorkerFactory

    /**
     * WorkManager queries this before constructing any Worker, so the factory
     * is always available. Auto-init is disabled in the manifest so that
     * WorkManager uses THIS configuration and can inject @HiltWorker dependencies.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Initialize Play Games SDK
        PlayGamesSdk.initialize(this)
        // Create notification channels on app start (safe to call multiple times)
        NotificationChannels.createChannels(this)
    }
}
