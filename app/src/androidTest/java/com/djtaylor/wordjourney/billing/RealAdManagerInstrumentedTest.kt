package com.djtaylor.wordjourney.billing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.ads.AdSettings
import com.facebook.ads.AudienceNetworkAds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for [RealAdManager].
 *
 * Runs on a physical device or emulator. Verifies that the Meta Audience Network
 * SDK initializes correctly and serves a test rewarded ad.
 *
 * ## How to run
 *   ./gradlew :app:connectedDebugAndroidTest --tests "*.RealAdManagerInstrumentedTest"
 *
 * ## What to check in Logcat (tag = RealAdManager / AudienceNetworkAds)
 *   - "Meta rewarded ad loaded and ready"  → SDK + placement working ✅
 *   - "Meta ad error 1001: …"              → No fill (normal on new/unreviewed apps)
 *   - "Meta ad error 2001: …"              → Network error
 *   - "Waiting for Meta SDK init (attempt …)" → SDK init delay detected (race condition)
 *
 * ## Test mode
 * Debug builds call AdSettings.setTestMode(true) in Application.onCreate().
 * This bypasses Meta's review/approval requirement and serves a mock "Test Ad" overlay.
 * If even test ads are failing, the placement ID or Meta app setup is misconfigured.
 */
@RunWith(AndroidJUnit4::class)
class RealAdManagerInstrumentedTest {

    private lateinit var adManager: RealAdManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Ensure test mode is active (mirrors Application.onCreate() in debug builds)
        AdSettings.setTestMode(true)
        AdSettings.turnOnSDKDebugger(context)

        // Initialize SDK synchronously enough for the test
        if (!AudienceNetworkAds.isInitialized(context)) {
            AudienceNetworkAds
                .buildInitSettings(context)
                .withInitListener { result ->
                    android.util.Log.d(
                        "AdManagerTest",
                        "SDK init result: ${if (result.isSuccess) "SUCCESS" else "FAILED: ${result.message}"}"
                    )
                }
                .initialize()
        }

        adManager = RealAdManager(context)
    }

    /**
     * Verifies that [RealAdManager.loadRewardedAd] successfully loads a test ad within 20 s.
     *
     * Failure modes:
     *  - TIMEOUT (20 s): SDK not initialized OR placement ID invalid OR no network.
     *  - isRewardedAdReady = false after completion: Meta returned an error (check Logcat for code).
     *
     * Common error codes:
     *  1001 – No fill (new app, pending Meta review — enable test mode to bypass)
     *  2001 – Network/connectivity error
     */
    @Test
    fun loadRewardedAd_withTestMode_succeeds() = runBlocking {
        val result = withTimeoutOrNull(20_000L) {
            adManager.loadRewardedAd()
            adManager.isRewardedAdReady
        }

        assertNotNull(
            "loadRewardedAd() timed out in 20 s — check network connectivity and that " +
            "AudienceNetworkAds.initialize() has been called. SDK version: 6.21.0",
            result
        )
        assertTrue(
            "Ad not ready after load — check Logcat tag=RealAdManager for error code. " +
            "In TEST MODE, ads should always load if the SDK is properly initialized. " +
            "Placement ID: ${RealAdManager.PLACEMENT_ID}",
            result!!
        )
    }

    /**
     * Verifies the SDK initialization race condition is handled.
     * Creates a fresh RealAdManager and calls loadRewardedAd() immediately — the wait
     * loop in loadRewardedAd() should bridge the gap until SDK init finishes.
     */
    @Test
    fun loadRewardedAd_beforeSdkInitCompletes_doesNotHangForever() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val freshManager = RealAdManager(context)

        // This should not hang (the 15 s timeout in RealAdManager + the SDK init wait handles it)
        val completed = withTimeoutOrNull(20_000L) {
            freshManager.loadRewardedAd()
            true
        }

        assertNotNull(
            "loadRewardedAd() never returned — possible infinite hang in init wait loop",
            completed
        )
        // We don't assert isRewardedAdReady = true here because in a
        // freshly-initialized SDK the first call might hit no-fill; the important
        // thing is that the method returns within the timeout.
    }

    /**
     * Verifies that calling isRewardedAdReady before a load always returns false (not
     * crashing or returning stale state).
     */
    @Test
    fun isRewardedAdReady_beforeLoad_isFalse() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val freshManager = RealAdManager(context)
        assertFalse(
            "isRewardedAdReady should be false before any loadRewardedAd() call",
            freshManager.isRewardedAdReady
        )
    }
}
