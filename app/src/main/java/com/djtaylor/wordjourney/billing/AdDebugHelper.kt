package com.djtaylor.wordjourney.billing

import android.content.Context
import android.util.Log
import com.djtaylor.wordjourney.BuildConfig
import com.ironsource.mediationsdk.IronSource

/**
 * Debug helpers for IronSource LevelPlay (Unity) integration.
 *
 * All public functions are no-ops in release builds.
 *
 * ## Quick-start: why are ads not loading?
 *
 * Step 1 - Check SDK initialisation in Logcat after launch:
 *   adb logcat -s IronSource:V
 *   Should see: "IronSource SDK version X.Y.Z initialized successfully"
 *
 * Step 2 - Check ad load in Logcat:
 *   adb logcat -s RealAdManager:V
 *   Should see: "LevelPlay rewarded ad loaded - network=..."
 *
 * Step 3 - Test mode in DEBUG builds:
 *   IronSource.setMetaData("is_test_suite", "enable") is set before init,
 *   which routes all ad requests to the LevelPlay test suite. No device
 *   registration or dashboard changes are needed.
 */
object AdDebugHelper {

    private const val TAG = "AdDebugHelper"

    /**
     * Prints LevelPlay SDK status and configuration to Logcat (DEBUG only).
     */
    fun printSdkStatus(context: Context) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, buildString {
            appendLine("IronSource LevelPlay Status")
            appendLine("  App Key       : ${RealAdManager.APP_KEY}")
            appendLine("  Ad Unit ID    : ${RealAdManager.AD_UNIT_ID}  (Rewarded Android)")
            appendLine("  Unity Game ID : ${RealAdManager.UNITY_GAME_ID}")
            appendLine("  Advertiser ID : ${IronSource.getAdvertiserId(context)}")
            appendLine("  Integration check: adb logcat -s IronSource:V")
            appendLine("  Dashboard: https://platform.ironsrc.com -> Word Journeys")
        })
    }

    /**
     * LevelPlay test mode is enabled via IronSource.setMetaData("is_test_suite", "enable").
     * Called in Application.onCreate() for DEBUG builds before IronSource.init().
     * No device ID allowlisting or dashboard configuration needed.
     */
    fun printTestModeStatus() {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "LevelPlay test mode: is_test_suite=enable (DEBUG builds only). " +
            "Confirm: adb logcat -s RealAdManager:V -> LevelPlay rewarded ad loaded")
    }

    /**
     * Prints full diagnostics. Call from a debug button or test bootstrap.
     */
    fun forceTestModeAndPrintDiagnostics(context: Context) {
        if (!BuildConfig.DEBUG) return
        printSdkStatus(context)
        printTestModeStatus()
    }

    // Backward-compat alias
    fun printHashedDeviceId() = printTestModeStatus()
}
