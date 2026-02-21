package com.djtaylor.wordjourney

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.datastore.PlayerDataStore
import com.djtaylor.wordjourney.ui.navigation.AppNavigation
import com.djtaylor.wordjourney.ui.theme.WordJourneysTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var audioManager: WordJourneysAudioManager
    @Inject lateinit var playerDataStore: PlayerDataStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by SettingsViewModel observer */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

            WordJourneysTheme(
                darkTheme = progress.darkMode,
                highContrast = progress.highContrast,
                colorblindMode = progress.colorblindMode,
                textScale = progress.textScaleFactor
            ) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        audioManager.onForeground()
    }

    override fun onPause() {
        super.onPause()
        audioManager.onBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}
