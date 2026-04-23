package com.djtaylor.wordjourney.billing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.sdk.InitializationListener
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for [RealAdManager] using IronSource LevelPlay SDK.
 *
 * Runs on a physical device or emulator. Verifies that the SDK initializes
 * and serves a test rewarded ad via the LevelPlay test suite.
 *
 * ## How to run
 *   ./gradlew :app:connectedDebugAndroidTest --tests "*.RealAdManagerInstrumentedTest"
 *
 * ## What to check in Logcat (tag = IronSource / RealAdManager)
 *   - "IronSource SDK X.Y.Z initialized successfully"  -> SDK init OK
 *   - "LevelPlay rewarded ad loaded - network=..."      -> ad ready
 *   - Any error code in onAdLoadFailed                  -> check dashboard
 *
 * ## Test mode
 * DEBUG builds call IronSource.setMetaData("is_test_suite", "enable") before
 * init, which routes all requests to the LevelPlay test suite. No device
 * registration or dashboard changes are needed for test ads to serve.
 */
@RunWith(AndroidJUnit4::class)
class RealAdManagerInstrumentedTest {

    private lateinit var adManager: RealAdManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Enable test suite mode (must be before init)
        IronSource.setMetaData("is_test_suite", "enable")

        // Initialize SDK synchronously enough for the test via a latch
        val initLatch = java.util.concurrent.CountDownLatch(1)
        IronSource.init(
            context,
            RealAdManager.APP_KEY,
            InitializationListener { initLatch.countDown() },
            IronSource.AD_UNIT.REWARDED_VIDEO
        )
        // Wait up to 10s for init to complete
        initLatch.await(10, java.util.concurrent.TimeUnit.SECONDS)

        adManager = RealAdManager(context)
    }

    /**
     * Verifies that [RealAdManager.loadRewardedAd] successfully loads a test ad
     * within 20 seconds using LevelPlay test suite mode.
     *
     * Failure modes:
     *  - TIMEOUT (20 s): SDK not initialized OR ad unit ID invalid OR no network.
     *  - isRewardedAdReady = false after completion: check Logcat for error code.
     */
    @Test
    fun loadRewardedAd_withTestSuiteMode_succeeds() = runBlocking {
        val result = withTimeoutOrNull(20_000L) {
            adManager.loadRewardedAd()
            adManager.isRewardedAdReady
        }

        assertNotNull(
            "loadRewardedAd() timed out in 20 s - check network connectivity and " +
                "that IronSource.init() has been called. " +
                "App Key: ${RealAdManager.APP_KEY}, Ad Unit: ${RealAdManager.AD_UNIT_ID}",
            result
        )
        assertTrue(
            "Ad not ready after load - check Logcat tag=IronSource for error details. " +
                "In TEST SUITE MODE, ads should always load if the SDK is properly initialized.",
            result!!
        )
    }

    /**
     * Verifies that isRewardedAdReady before any load call returns false.
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

    /**
     * Verifies loadRewardedAd() returns (does not hang forever) even when
     * called immediately after a fresh RealAdManager is created.
     */
    @Test
    fun loadRewardedAd_doesNotHangForever() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val freshManager = RealAdManager(context)

        val completed = withTimeoutOrNull(20_000L) {
            freshManager.loadRewardedAd()
            true
        }

        assertNotNull(
            "loadRewardedAd() never returned - possible hang in SDK initialization",
            completed
        )
    }
}
