package com.djtaylor.wordjourney

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.cloud.CloudSaveManager
import com.djtaylor.wordjourney.data.datastore.PlayerDataStore
import com.djtaylor.wordjourney.domain.model.ThemeRegistry
import com.djtaylor.wordjourney.ui.navigation.AppNavigation
import com.djtaylor.wordjourney.ui.theme.WordJourneysTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var audioManager: WordJourneysAudioManager
    @Inject lateinit var playerDataStore: PlayerDataStore
    @Inject lateinit var cloudSaveManager: CloudSaveManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by SettingsViewModel observer */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Edge-to-edge without the deprecated Window color/cutout APIs.
        // - WindowCompat.setDecorFitsSystemWindows(false) lets content draw behind system bars
        //   without calling the deprecated Window.setStatusBarColor / setNavigationBarColor.
        // - LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS (API 30+) extends content into the camera
        //   notch area; not needed on API 29 where notched phones were rare.
        // enableEdgeToEdge() (the activity-ktx helper) is intentionally NOT called here:
        // its bytecode calls all three deprecated APIs that Play Console flags, and R8 will
        // strip it entirely from the release build since nothing references it.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            // Read dark mode and high contrast from persisted settings
            val progress by playerDataStore.playerProgressFlow
                .collectAsStateWithLifecycle(
                    initialValue = com.djtaylor.wordjourney.domain.model.PlayerProgress()
                )

            val activeGameTheme = remember(progress.selectedTheme) {
                ThemeRegistry.getThemeById(progress.selectedTheme) ?: ThemeRegistry.CLASSIC
            }

            WordJourneysTheme(
                darkTheme = progress.darkMode,
                highContrast = progress.highContrast,
                colorblindMode = progress.colorblindMode,
                textScale = progress.textScaleFactor,
                gameTheme = activeGameTheme
            ) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    hasCompletedOnboarding = progress.hasCompletedOnboarding
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        audioManager.onForeground()
        cloudSaveManager.setActivity(this)
    }

    override fun onPause() {
        super.onPause()
        audioManager.onBackground()
        cloudSaveManager.setActivity(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}
