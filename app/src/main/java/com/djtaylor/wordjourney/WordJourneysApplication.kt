package com.djtaylor.wordjourney

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.djtaylor.wordjourney.BuildConfig
import com.djtaylor.wordjourney.billing.ActivityProvider
import com.djtaylor.wordjourney.billing.AdDebugHelper
import com.djtaylor.wordjourney.billing.RealAdManager
import com.djtaylor.wordjourney.notifications.NotificationChannels
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.sdk.InitializationListener
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
        // Enable LevelPlay test suite in DEBUG builds (must be set BEFORE init)
        if (BuildConfig.DEBUG) {
            IronSource.setMetaData("is_test_suite", "enable")
        }
        // Pass Unity Game ID to the Unity Ads mediation adapter
        IronSource.setMetaData("unityads_game_id", RealAdManager.UNITY_GAME_ID)
        // Initialize IronSource LevelPlay SDK.
        // Docs: https://developers.is.com/ironsource-mobile/android/android-sdk/
        IronSource.init(
            this,
            RealAdManager.APP_KEY,
            InitializationListener {
                android.util.Log.d("WordJourneysApp",
                    "✅ IronSource LevelPlay SDK initialized (debug=${BuildConfig.DEBUG})")
                if (BuildConfig.DEBUG) {
                    AdDebugHelper.printSdkStatus(this)
                    AdDebugHelper.printTestModeStatus()
                }
            },
            IronSource.AD_UNIT.REWARDED_VIDEO
        )
        // Initialize Play Games SDK
        PlayGamesSdk.initialize(this)
        // Create notification channels on app start (safe to call multiple times)
        NotificationChannels.createChannels(this)
    }
}
