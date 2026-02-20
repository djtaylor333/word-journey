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
import androidx.navigation.compose.rememberNavController
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.ui.navigation.AppNavigation
import com.djtaylor.wordjourney.ui.theme.WordJourneysTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var audioManager: WordJourneysAudioManager

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
            // Read dark mode preference — defaulting to true here; SettingsViewModel
            // will override once DataStore loads
            var darkMode by remember { mutableStateOf(true) }
            var highContrast by remember { mutableStateOf(false) }

            WordJourneysTheme(darkTheme = darkMode, highContrast = highContrast) {
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
