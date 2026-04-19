package com.djtaylor.wordjourney.billing

import android.content.Context
import android.util.Log
import com.djtaylor.wordjourney.BuildConfig
import com.facebook.ads.AdSettings
import com.facebook.ads.AudienceNetworkAds

/**
 * Debug helpers for Meta Audience Network ad testing.
 *
 * Only meaningful in DEBUG builds — all public functions are no-ops in release.
 *
 * ## Quick-start: why are ads showing "Retry"?
 *
 * Step 1 — Check initialization
 *   Call [printSdkStatus] right after Application.onCreate() completes.
 *   If "SDK initialized: false" → your App ID or Client Token is wrong.
 *
 * Step 2 — Get your hashed device ID
 *   Call [printHashedDeviceId] and paste the result into Meta Dashboard:
 *   developers.facebook.com → your app → Audience Network → Test Devices
 *   (Only needed if setTestMode(true) alone isn't serving test ads)
 *
 * Step 3 — Run the live instrumented test on a connected device:
 *   ./gradlew :app:connectedDebugAndroidTest \
 *       --tests "com.djtaylor.wordjourney.billing.RealAdManagerInstrumentedTest"
 *
 * Step 4 — Watch Logcat in real time:
 *   adb logcat -s RealAdManager:V AudienceNetworkAds:V FBAudienceNetwork:V AdDebugHelper:V
 *
 * Step 5 — Interpret error codes
 *   1001 = No fill (use test mode in debug — already enabled automatically)
 *   2000 = Init failed (App ID / Client Token missing from Manifest)
 *   1000 = No network
 *   6    = Wrong placement ID / ad unit paused
 */
object AdDebugHelper {

    private const val TAG = "AdDebugHelper"

    /**
     * Prints the current SDK + test-mode status to Logcat (DEBUG only).
     * Call this from HomeViewModel or Application.onCreate() to verify setup.
     */
    fun printSdkStatus(context: Context) {
        if (!BuildConfig.DEBUG) return
        val initialized = AudienceNetworkAds.isInitialized(context)
        val testMode    = BuildConfig.DEBUG  // test mode is gated on debug build
        Log.d(TAG, """
            ┌─ Meta AAN SDK Status ──────────────────────────────────────
            │  SDK initialized : $initialized
            │  Test mode active: $testMode  (setTestMode called in Application.onCreate)
            │  Placement ID    : ${RealAdManager.PLACEMENT_ID}
            │
            │  If initialized=false check:
            │    • strings.xml → facebook_app_id  (numeric, e.g. "1685702049238776")
            │    • strings.xml → facebook_client_token  (from Meta → Settings → Advanced)
            │    • AndroidManifest.xml has both <meta-data> entries
            │    • AudienceNetworkAds.initialize() is called in Application.onCreate()
            │      AFTER AdSettings.setTestMode(true)
            └────────────────────────────────────────────────────────────
        """.trimIndent())
    }

    /**
     * Prints the hashed device ID that can be registered in Meta's test-device list.
     * Useful when setTestMode(true) isn't sufficient (e.g. specific network configs).
     */
    fun printHashedDeviceId() {
        if (!BuildConfig.DEBUG) return
        // Meta AAN SDK 6.x does not expose a programmatic API to retrieve the hashed device ID.
        // The SDK logs it automatically to Logcat on first initialization under the tag
        // "FBAudienceNetwork" as "Test Device Hash: XXXXXXXXXX".
        Log.d(TAG, """
            ┌─ Meta AAN Test Device Hash ─────────────────────────────
            │  The hashed device ID is printed automatically by the Meta SDK
            │  to Logcat on first launch. To find it:
            │
            │  Run the app and search Logcat for:
            │    tag: FBAudienceNetwork
            │    text: "Test Device Hash"
            │
            │  Then paste that hash in Meta Dashboard:
            │  developers.facebook.com → your app → Audience Network
            │    → Test Devices → Add Device
            │
            │  Note: If AdSettings.setTestMode(true) is called BEFORE initialize()
            │  test ads load WITHOUT device registration.
            └─────────────────────────────────────────────────────────────
        """.trimIndent())
    }

    /**
     * Manually forces the SDK into test mode and re-prints diagnostics.
     * Call this from a debug button or from a unit test bootstrap.
     */
    fun forceTestModeAndPrintDiagnostics(context: Context) {
        if (!BuildConfig.DEBUG) return
        AdSettings.setTestMode(true)
        AdSettings.turnOnSDKDebugger(context)
        printSdkStatus(context)
        printHashedDeviceId()
    }
}
