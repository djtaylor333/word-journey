package com.djtaylor.wordjourney

import android.app.Application
import com.djtaylor.wordjourney.notifications.NotificationChannels
import com.google.android.gms.games.PlayGamesSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WordJourneysApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Play Games SDK
        PlayGamesSdk.initialize(this)
        // Create notification channels on app start (safe to call multiple times)
        NotificationChannels.createChannels(this)
    }
}
