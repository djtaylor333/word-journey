package com.djtaylor.wordjourney

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.djtaylor.wordjourney.BuildConfig
import com.djtaylor.wordjourney.audio.AudioSettings
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.billing.ActivityProvider
import com.djtaylor.wordjourney.billing.AdDebugHelper
import com.djtaylor.wordjourney.billing.IAdManager
import com.djtaylor.wordjourney.billing.RealAdManager
import com.djtaylor.wordjourney.data.datastore.PlayerDataStore
import com.djtaylor.wordjourney.notifications.NotificationChannels
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.sdk.InitializationListener
import com.google.android.gms.games.PlayGamesSdk
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class WordJourneysApplication : Application(), Configuration.Provider {

    // Injected after Hilt component is initialised (before any Worker is created)
    @Inject lateinit var workerFactory: HiltWorkerFactory

    /** Tracks the currently resumed Activity so RealBillingManager can launch the purchase sheet. */
    @Inject lateinit var activityProvider: ActivityProvider

    /** Needed to eagerly apply saved audio settings before any Activity starts. */
    @Inject lateinit var playerDataStore: PlayerDataStore
    @Inject lateinit var audioManager: WordJourneysAudioManager

    /**
     * Injected eagerly so RealAdManager registers its IronSource listener
     * before IronSource.init() is called below.
     */
    @Inject lateinit var adManager: IAdManager

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
        // Eagerly apply saved audio settings so the AudioManager has the correct
        // enabled/volume state before MainActivity.onResume() fires audioManager.onForeground().
        // Without this, the manager starts with defaults (sound ON) until a ViewModel loads.
        try {
            val savedProgress = runBlocking { playerDataStore.playerProgressFlow.first() }
            audioManager.updateSettings(
                AudioSettings(
                    musicEnabled = savedProgress.musicEnabled,
                    musicVolume  = savedProgress.musicVolume,
                    sfxEnabled   = savedProgress.sfxEnabled,
                    sfxVolume    = savedProgress.sfxVolume
                )
            )
        } catch (_: Exception) { /* keep defaults if DataStore unavailable */ }
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
                // Pre-fetch the first rewarded ad now that the SDK is fully initialised.
                IronSource.loadRewardedVideo()
            },
            IronSource.AD_UNIT.REWARDED_VIDEO
        )
        // Initialize Play Games SDK
        PlayGamesSdk.initialize(this)
        // Create notification channels on app start (safe to call multiple times)
        NotificationChannels.createChannels(this)
    }
}
