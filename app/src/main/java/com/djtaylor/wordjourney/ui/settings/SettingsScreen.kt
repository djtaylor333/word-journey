package com.djtaylor.wordjourney.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.djtaylor.wordjourney.domain.model.GameTheme
import com.djtaylor.wordjourney.domain.model.SeasonalThemeManager
import com.djtaylor.wordjourney.domain.model.ThemeCategory
import com.djtaylor.wordjourney.domain.model.ThemeRegistry
import java.time.LocalDate
import com.djtaylor.wordjourney.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    val theme = LocalGameTheme.current
    // Tap counter for version easter egg: 10 taps → dev mode on, then 3 taps → dev mode off
    var versionTapCount by remember { mutableIntStateOf(0) }

    // Notification permission launcher (Android 13+)
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setNotifyLivesFull(true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ThemeBackgroundOverlay(theme = theme)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // ── Music ─────────────────────────────────────────────────────────
            SettingsSection(title = "Music")

            SettingsToggleRow(
                label = "Background Music",
                description = "Play ambient music while solving",
                checked = state.musicEnabled,
                onCheckedChange = viewModel::setMusicEnabled
            )

            if (state.musicEnabled) {
                SettingsSliderRow(
                    label = "Music Volume",
                    value = state.musicVolume,
                    onValueChange = viewModel::setMusicVolume
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Sound Effects ─────────────────────────────────────────────────
            SettingsSection(title = "Sound Effects")

            SettingsToggleRow(
                label = "Sound Effects",
                description = "Tile flips, correct guesses, keyboard taps",
                checked = state.sfxEnabled,
                onCheckedChange = viewModel::setSfxEnabled
            )

            if (state.sfxEnabled) {
                SettingsSliderRow(
                    label = "SFX Volume",
                    value = state.sfxVolume,
                    onValueChange = viewModel::setSfxVolume
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Notifications ─────────────────────────────────────────────────
            SettingsSection(title = "Notifications")

            SettingsToggleRow(
                label = "Lives Replenished",
                description = "Notify when your lives reach 10",
                checked = state.notifyLivesFull,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        viewModel.setNotifyLivesFull(false)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val alreadyGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (alreadyGranted) {
                            viewModel.setNotifyLivesFull(true)
                        } else {
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        viewModel.setNotifyLivesFull(enabled)
                    }
                }
            )

            SettingsToggleRow(
                label = "Daily Challenge Reminder",
                description = "Noon reminder to keep your streak going",
                checked = state.notifyDailyChallenge,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        viewModel.setNotifyDailyChallenge(false)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val alreadyGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (alreadyGranted) {
                            viewModel.setNotifyDailyChallenge(true)
                        } else {
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        viewModel.setNotifyDailyChallenge(enabled)
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Appearance ────────────────────────────────────────────────────
            SettingsSection(title = "Appearance")

            SettingsToggleRow(
                label = "Dark Mode",
                description = "Use dark color scheme",
                checked = state.darkMode,
                onCheckedChange = viewModel::setDarkMode
            )

            SettingsToggleRow(
                label = "High Contrast",
                description = "Increase contrast for tiles and text",
                checked = state.highContrast,
                onCheckedChange = viewModel::setHighContrast
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Themes ────────────────────────────────────────────────────────
            SettingsSection(title = "Themes")

            Text(
                text = "Choose a visual theme for your game",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(8.dp))

            // Diamond balance
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("💎", fontSize = 18.sp)
                Text(
                    "${state.diamonds} diamonds available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = adaptiveDiamondColor(!isSystemInDarkTheme()),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            // Free themes
            ThemeCategoryLabel("🎨 Free Themes")
            ThemeGrid(
                themes = ThemeRegistry.FREE_THEMES,
                selectedTheme = state.selectedTheme,
                ownedThemes = state.ownedThemes,
                isVip = state.isVip,
                onSelect = { viewModel.selectTheme(it) },
                onPurchase = { viewModel.purchaseTheme(it) }
            )

            Spacer(Modifier.height(12.dp))

            // VIP themes
            ThemeCategoryLabel("👑 VIP Themes")
            Box(modifier = Modifier.fillMaxWidth()) {
                ThemeGrid(
                    themes = ThemeRegistry.VIP_THEMES,
                    selectedTheme = state.selectedTheme,
                    ownedThemes = state.ownedThemes,
                    isVip = state.isVip,
                    onSelect = { viewModel.selectTheme(it) },
                    onPurchase = { viewModel.purchaseTheme(it) }
                )
                // Locked overlay for non-VIP users
                if (!state.isVip) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier.matchParentSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔒", fontSize = 28.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "VIP Subscription Required",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Subscribe to unlock exclusive themes",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Seasonal themes — lock overlays based on season status
            ThemeCategoryLabel("🌟 Seasonal Themes")
            Text(
                "Past events: VIP members can purchase. Future events: coming soon.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            ThemeRegistry.SEASONAL_THEMES.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { seTheme ->
                        val lockInfo = SeasonalThemeManager.getThemeLockInfo(seTheme.id, LocalDate.now())
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            ThemeCard(
                                theme = seTheme,
                                isSelected = state.selectedTheme == seTheme.id,
                                isOwned = seTheme.id in state.ownedThemes,
                                isVip = state.isVip,
                                onSelect = { viewModel.selectTheme(seTheme.id) },
                                onPurchase = { viewModel.purchaseTheme(seTheme.id) }
                            )
                            // Lock overlay for non-active seasonal themes
                            when (lockInfo.status) {
                                SeasonalThemeManager.SeasonalLockStatus.FUTURE -> {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.68f),
                                        modifier = Modifier.matchParentSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🔒", fontSize = 22.sp)
                                                Text(
                                                    "Coming ${lockInfo.season?.startMonth?.name?.take(3)?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Soon"}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                SeasonalThemeManager.SeasonalLockStatus.PAST -> {
                                    if (!state.isVip && seTheme.id !in state.ownedThemes) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.Black.copy(alpha = 0.55f),
                                            modifier = Modifier.matchParentSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("🔒", fontSize = 20.sp)
                                                    Text(
                                                        "VIP Only",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                    Text(
                                                        "Past Event",
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                SeasonalThemeManager.SeasonalLockStatus.ACTIVE -> { /* no overlay – purchasable */ }
                            }
                        }
                    }
                    // Fill empty cells in last row
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Accessibility ─────────────────────────────────────────────────
            SettingsSection(title = "Accessibility")

            // Colorblind mode — hidden for now (feature retained)
            // To re-enable: remove the wrapping condition below

            // Text scale slider
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Text Size",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Scale text throughout the app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = "${(state.textScaleFactor * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Slider(
                    value = state.textScaleFactor,
                    onValueChange = viewModel::setTextScaleFactor,
                    valueRange = 0.8f..1.5f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                // Preview text
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Preview: The quick brown fox",
                        modifier = Modifier.padding(12.dp),
                        fontSize = (16 * state.textScaleFactor).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── VIP Subscription ──────────────────────────────────────────────
            SettingsSection(title = "VIP")

            if (state.isVip) {
                var showCancelDialog by remember { mutableStateOf(false) }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("👑", fontSize = 22.sp)
                            Column {
                                Text(
                                    "VIP Active",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Enjoy exclusive VIP difficulty and themes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Text("Cancel VIP Subscription", fontSize = 14.sp)
                        }
                    }
                }

                if (showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { showCancelDialog = false },
                        title = { Text("Cancel VIP?", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Are you sure you want to cancel your VIP subscription?")
                                Text(
                                    "You will lose access to VIP difficulty mode and any active VIP theme.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.cancelVip()
                                    showCancelDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel VIP")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCancelDialog = false }) {
                                Text("Keep VIP")
                            }
                        }
                    )
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🔒", fontSize = 20.sp)
                        Column {
                            Text(
                                "VIP Not Active",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Subscribe in the Store to unlock VIP perks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Account ───────────────────────────────────────────────────────
            SettingsSection(title = "Account")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Google Play Games",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (state.playGamesSignedIn) {
                            state.playerDisplayName ?: "Signed in"
                        } else {
                            "Sign in for cloud saves"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = if (state.playGamesSignedIn) "Connected" else "Not connected",
                    color = if (state.playGamesSignedIn) TileCorrect else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── About ─────────────────────────────────────────────────────────
            SettingsSection(title = "About")

            Surface(
                onClick = { showAboutDialog = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Word Journeys",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap for more info",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        text = "v${state.appVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Dev Mode Panel (only visible when dev mode is active) ──────────
            if (state.devModeEnabled) {
                DevModePanel(
                    onResetDailyChallenges = { viewModel.devResetDailyChallenges() },
                    onResetStatistics      = { viewModel.devResetStatistics() },
                    onDisableDevMode       = {
                        viewModel.setDevModeEnabled(false)
                        versionTapCount = 0
                        Toast.makeText(context, "Dev mode disabled", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        } // end Box
    }

    // ── About dialog ──────────────────────────────────────────────────────────
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = {
                showAboutDialog = false
                versionTapCount = 0  // reset counter on dialog close
            },
            title = {
                Text("Word Journeys", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Made by David Taylor",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    // Tappable version string: 10 taps = enable dev mode, then 3 taps = disable
                    Text(
                        text = "Version ${state.appVersion}" + if (state.devModeEnabled) " [DEV]" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.devModeEnabled)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable {
                                versionTapCount++
                                if (!state.devModeEnabled && versionTapCount >= 10) {
                                    viewModel.setDevModeEnabled(true)
                                    versionTapCount = 0
                                    Toast.makeText(context, "🛠 Dev mode enabled!", Toast.LENGTH_SHORT).show()
                                } else if (state.devModeEnabled && versionTapCount >= 3) {
                                    viewModel.setDevModeEnabled(false)
                                    versionTapCount = 0
                                    Toast.makeText(context, "Dev mode disabled", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp)
                    )
                    HorizontalDivider()
                    Text(
                        "Inspired by Wordle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        "A level-based word puzzle adventure with multiple difficulties, " +
                            "items, and progression.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAboutDialog = false
                    versionTapCount = 0
                }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DevModePanel(
    onResetDailyChallenges: () -> Unit,
    onResetStatistics: () -> Unit,
    onDisableDevMode: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛠", fontSize = 20.sp)
                Text(
                    "Developer Options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                "Internal testing tools. Tap version number 3 times to disable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))

            // Reset daily challenges
            OutlinedButton(
                onClick = onResetDailyChallenges,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset Daily Challenges")
            }

            // Reset statistics
            OutlinedButton(
                onClick = onResetStatistics,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                )
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset Statistics")
            }

            // Disable dev mode
            TextButton(
                onClick = onDisableDevMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disable Dev Mode", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun ThemeCategoryLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun ThemeGrid(
    themes: List<GameTheme>,
    selectedTheme: String,
    ownedThemes: Set<String>,
    isVip: Boolean,
    onSelect: (String) -> Unit,
    onPurchase: (String) -> Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = theme.id == selectedTheme,
                        isOwned = theme.id in ownedThemes,
                        isVip = isVip,
                        modifier = Modifier.weight(1f),
                        onSelect = { onSelect(theme.id) },
                        onPurchase = { onPurchase(theme.id) }
                    )
                }
                // Fill empty slots
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: GameTheme,
    isSelected: Boolean,
    isOwned: Boolean,
    isVip: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onPurchase: () -> Boolean
) {
    val borderColor = when {
        isSelected -> Primary
        isOwned -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Surface(
        onClick = {
            if (isOwned) onSelect()
            else onPurchase()
        },
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isSelected) 6.dp else 2.dp,
        modifier = modifier
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            theme.backgroundDark,
                            theme.surfaceDark
                        )
                    )
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color preview tiles
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(theme.tileCorrect))
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(theme.tilePresent))
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(theme.tileAbsent))
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = theme.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            // Status badge
            when {
                isSelected -> {
                    Text("✓ Active", fontSize = 9.sp, color = Primary, fontWeight = FontWeight.Bold)
                }
                isOwned -> {
                    Text("Owned", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                }
                theme.category == ThemeCategory.VIP && !isVip -> {
                    Text("👑 VIP", fontSize = 9.sp, color = adaptiveCoinColor(!isSystemInDarkTheme()))
                }
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("💎", fontSize = 9.sp)
                        Text("${theme.diamondCost}", fontSize = 9.sp, color = adaptiveDiamondColor(!isSystemInDarkTheme()), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
