package com.djtaylor.wordjourney.billing

import android.content.Context
import android.util.Log
import com.djtaylor.wordjourney.BuildConfig

/**
 * Debug helpers for Yandex Mobile Ads integration.
 *
 * All public functions are no-ops in release builds.
 *
 * ## Quick-start: why are ads not loading?
 *
 * Step 1 — Check SDK initialisation in Logcat after launch:
 *   adb logcat -v brief '*:S YandexAds'
 *   Should see: "[Integration] Ad type rewarded was integrated successfully"
 *   Then:       "Yandex Mobile Ads 7.x initialized successfully"
 *
 * Step 2 — Check ad load in Logcat:
 *   adb logcat -s RealAdManager:V
 *   Should see: "✅ Yandex rewarded ad loaded (adUnitId=demo-rewarded-yandex)"
 *
 * Step 3 — No device registration needed.
 *   DEBUG builds use "demo-rewarded-yandex" which always serves test ads.
 */
object AdDebugHelper {

    private const val TAG = "AdDebugHelper"

    /**
     * Prints Yandex SDK status and unit ID configuration to Logcat (DEBUG only).
     */
    fun printSdkStatus(context: Context) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, """
            ┌─ Yandex Mobile Ads Status ──────────────────────────────────────
            │  Ad Unit (debug)  : ${RealAdManager.TEST_AD_UNIT_ID} (demo — always fills)
            │  Ad Unit (release): ${RealAdManager.PROD_AD_UNIT_ID}
            │  Currently using  : ${RealAdManager.AD_UNIT_ID}
            │
            │  Integration check (run after first launch):
            │    adb logcat -v brief '*:S YandexAds'
            │    → "[Integration] Ad type rewarded was integrated successfully"
            │    → "Yandex Mobile Ads 7.x initialized successfully"
            │
            │  Dashboard: partner.yandex.com → Word Journeys → Android app
            └────────────────────────────────────────────────────────────────
        """.trimIndent())
    }

    /**
     * Yandex SDK does not use device-hash allowlisting.
     * Test ads are served automatically via the demo ad unit in DEBUG builds.
     */
    fun printHashedDeviceId() {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, """
            ┌─ Yandex Ads Test Mode ──────────────────────────────────────────
            │  No device registration needed.
            │  All DEBUG builds use: demo-rewarded-yandex
            │  → guarantees a test ad on every device, no allowlisting required.
            │
            │  To confirm test ads are loading:
            │    adb logcat -s RealAdManager:V
            │    → "✅ Yandex rewarded ad loaded (adUnitId=demo-rewarded-yandex)"
            └────────────────────────────────────────────────────────────────
        """.trimIndent())
    }

    /**
     * Prints full diagnostics. Call from a debug button or test bootstrap.
     */
    fun forceTestModeAndPrintDiagnostics(context: Context) {
        if (!BuildConfig.DEBUG) return
        printSdkStatus(context)
        printHashedDeviceId()
    }
}

