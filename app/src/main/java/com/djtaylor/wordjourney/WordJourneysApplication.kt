package com.djtaylor.wordjourney

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.djtaylor.wordjourney.BuildConfig
import com.djtaylor.wordjourney.billing.ActivityProvider
import com.djtaylor.wordjourney.billing.AdDebugHelper
import com.djtaylor.wordjourney.notifications.NotificationChannels
import com.facebook.ads.AdSettings
import com.facebook.ads.AudienceNetworkAds
import com.google.android.gms.games.PlayGamesSdk
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WordJourneysApplication : Application(), Configuration.Provider {

    // Injected after Hilt component is initialised (before any Worker is created)
    @Inject lateinit var workerFactory: HiltWorkerFactory

    /** Tracks the currently resumed Activity so RealBillingManager can launch the purchase sheet. */
    @Inject lateinit var activityProvider: ActivityProvider

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
        // Register before anything else so activity references are available immediately
        registerActivityLifecycleCallbacks(activityProvider)
        // Initialize Meta Audience Network SDK (must be called before any ad load)
        // IMPORTANT: setTestMode / setSDKDebugger MUST be called BEFORE initialize().
        if (BuildConfig.DEBUG) {
            AdSettings.setTestMode(true)
            AdSettings.turnOnSDKDebugger(this)
            android.util.Log.d("WordJourneysApp", "Meta Audience Network: TEST MODE + SDK debugger enabled (debug build)")
        }
        AudienceNetworkAds
            .buildInitSettings(this)
            .withInitListener { result ->
                if (result.isSuccess) {
                    android.util.Log.d("WordJourneysApp", "✅ Meta Audience Network initialized (testMode=${BuildConfig.DEBUG})")
                    // In debug builds, print the full diagnostic block so any setup
                    // problems (wrong App ID, missing client token, etc.) are immediately visible.
                    if (BuildConfig.DEBUG) {
                        AdDebugHelper.printSdkStatus(this)
                        AdDebugHelper.printHashedDeviceId()
                    }
                } else {
                    android.util.Log.e("WordJourneysApp", "❌ Meta AAN init failed: ${result.message}\n" +
                        "   Check: strings.xml facebook_app_id / facebook_client_token\n" +
                        "   Check: AndroidManifest.xml has both com.facebook.sdk.* <meta-data> entries")
                }
            }
            .initialize()
        // Initialize Play Games SDK
        PlayGamesSdk.initialize(this)
        // Create notification channels on app start (safe to call multiple times)
        NotificationChannels.createChannels(this)
    }
}
