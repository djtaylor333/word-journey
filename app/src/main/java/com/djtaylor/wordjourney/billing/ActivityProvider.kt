package com.djtaylor.wordjourney.billing

import android.app.Activity
import android.app.Application
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the currently resumed [Activity] via [Application.ActivityLifecycleCallbacks].
 *
 * Injected by [RealBillingManager] to get an Activity reference for [launchBillingFlow].
 * Register this in [WordJourneysApplication.onCreate] via:
 *   `registerActivityLifecycleCallbacks(activityProvider)`
 */
@Singleton
class ActivityProvider @Inject constructor() : Application.ActivityLifecycleCallbacks {

    @Volatile
    var currentActivity: Activity? = null
        private set

    override fun onActivityResumed(activity: Activity)  { currentActivity = activity }
    override fun onActivityPaused(activity: Activity)   { if (currentActivity === activity) currentActivity = null }

    // No-ops — only care about resumed/paused
    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) = Unit
    override fun onActivityStarted(activity: Activity)  = Unit
    override fun onActivityStopped(activity: Activity)  = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
