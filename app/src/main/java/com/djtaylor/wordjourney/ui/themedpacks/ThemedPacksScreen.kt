package com.djtaylor.wordjourney.ui.themedpacks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.domain.model.SeasonalThemeManager
import com.djtaylor.wordjourney.domain.model.SeasonalWordPacks
import com.djtaylor.wordjourney.domain.model.seasonalFirstOpenFor
import com.djtaylor.wordjourney.domain.model.seasonalLevelFor
import com.djtaylor.wordjourney.domain.model.seasonalMilestoneFor
import com.djtaylor.wordjourney.ui.theme.AccentEasy
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// ThemedPacksScreen — browse and play all 6 seasonal word packs
// Each pack has 100 levels and is always playable regardless of the date.
// ─────────────────────────────────────────────────────────────────────────────

private const val EXPIRY_DAYS = 30L   // challenge window from first open

/** Season-specific theme colours for the pack cards. */
private data class PackColors(val bg: Color, val accent: Color, val badge: Color)

private fun seasonColors(season: SeasonalThemeManager.Season): PackColors = when (season) {
    SeasonalThemeManager.Season.VALENTINES  -> PackColors(Color(0xFF3D1A2A), Color(0xFFF472B6), Color(0xFFBE185D))
    SeasonalThemeManager.Season.EASTER      -> PackColors(Color(0xFF1A3D2E), Color(0xFF86EFAC), Color(0xFF16A34A))
    SeasonalThemeManager.Season.SUMMER      -> PackColors(Color(0xFF3D2A1A), Color(0xFFFBBF24), Color(0xFFD97706))
    SeasonalThemeManager.Season.HALLOWEEN   -> PackColors(Color(0xFF2A1A3D), Color(0xFFC084FC), Color(0xFF7C3AED))
    SeasonalThemeManager.Season.THANKSGIVING-> PackColors(Color(0xFF3D2A1A), Color(0xFFFD8C3A), Color(0xFFB45309))
    SeasonalThemeManager.Season.CHRISTMAS   -> PackColors(Color(0xFF1A3D1A), Color(0xFF4ADE80), Color(0xFF15803D))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemedPacksScreen(
    onNavigateToLevelSelect: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ThemedPacksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val seasonStatuses = remember { SeasonalThemeManager.getAllSeasonStatuses() }
    val playerProgress = uiState.progress

    // Pending navigation after intro dialog
    var pendingNavigationKey by remember { mutableStateOf<String?>(null) }

    // Intro dialog for a specific season
    val introKey = uiState.introPackKey
    if (introKey != null) {
        val introSeason = seasonStatuses.find { it.season.name.lowercase() == introKey }?.season
        SeasonIntroDialog(
            season = introSeason,
            colors = introSeason?.let { seasonColors(it) }
                ?: PackColors(Color(0xFF1C1C1E), Color(0xFF60A5FA), Color(0xFF3B82F6)),
            onPlay = {
                viewModel.dismissIntroDialog()
                pendingNavigationKey?.let { key ->
                    onNavigateToLevelSelect(key)
                    pendingNavigationKey = null
                }
            },
            onDismiss = {
                viewModel.dismissIntroDialog()
                pendingNavigationKey = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🎁 Themed Packs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                "100 themed levels per pack. Complete milestones every 10 levels for item rewards!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            seasonStatuses.forEach { status ->
                val season = status.season
                val seasonKey = season.name.lowercase()
                val difficultyKey = "seasonal_$seasonKey"
                val totalLevels = SeasonalWordPacks.packSize(seasonKey)
                val currentLevel = playerProgress.seasonalLevelFor(seasonKey)
                val levelsCompleted = (currentLevel - 1).coerceAtLeast(0)
                val progress = levelsCompleted.toFloat() / totalLevels.toFloat()
                val colors = seasonColors(season)
                val firstOpenMs = playerProgress.seasonalFirstOpenFor(seasonKey)
                val milestoneClaimed = playerProgress.seasonalMilestoneFor(seasonKey)
                val nextMilestoneLevels = (milestoneClaimed + 1) * 10

                // Expiry countdown
                val daysRemaining: Long? = if (firstOpenMs > 0L) {
                    val elapsed = System.currentTimeMillis() - firstOpenMs
                    val remaining = TimeUnit.DAYS.convert(EXPIRY_DAYS * 24 * 3600 * 1000L - elapsed, TimeUnit.MILLISECONDS)
                    remaining.coerceAtLeast(0L)
                } else null

                // A seasonal pack is locked (not yet playable) if its season hasn't started
                // and the player hasn't already made progress in it.
                val isLocked = status.daysUntil > 0 && levelsCompleted == 0

                SeasonPackCard(
                    season = season,
                    isActive = status.isActive,
                    isLocked = isLocked,
                    daysUntil = status.daysUntil,
                    currentLevel = currentLevel,
                    levelsCompleted = levelsCompleted,
                    totalLevels = totalLevels,
                    progress = progress,
                    colors = colors,
                    daysRemaining = daysRemaining,
                    nextMilestoneLevels = if (levelsCompleted < totalLevels) nextMilestoneLevels else null,
                    onClick = {
                        val diffKey = "seasonal_$seasonKey"
                        val isFirstOpen = viewModel.onPackOpened(seasonKey)
                        if (isFirstOpen) {
                            // Intro dialog will be shown; navigate after dismissal
                            pendingNavigationKey = diffKey
                        } else {
                            onNavigateToLevelSelect(diffKey)
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Intro dialog shown the first time a player opens a seasonal pack ──────────
@Composable
private fun SeasonIntroDialog(
    season: SeasonalThemeManager.Season?,
    colors: PackColors,
    onPlay: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "${season?.emoji ?: "🎁"} ${season?.displayName ?: "Themed Pack"}",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    season?.description ?: "A special themed word pack awaits!",
                    style = MaterialTheme.typography.bodyLarge
                )
                HorizontalDivider()
                Text("🏆 How it works:", fontWeight = FontWeight.SemiBold)
                Text("• 100 themed levels to complete", style = MaterialTheme.typography.bodyMedium)
                Text("• Every 10 levels: reward of 3 of each power-up item", style = MaterialTheme.typography.bodyMedium)
                Text("• Complete all 100 levels: grand prize of 10,000 coins + 100 diamonds!", style = MaterialTheme.typography.bodyMedium)
                Text("• 30-day challenge window starts now — try to finish in time!", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text("▶ Start Pack!", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonPackCard(
    season: SeasonalThemeManager.Season,
    isActive: Boolean,
    isLocked: Boolean = false,
    daysUntil: Long,
    currentLevel: Int,
    levelsCompleted: Int,
    totalLevels: Int,
    progress: Float,
    colors: PackColors,
    daysRemaining: Long?,
    nextMilestoneLevels: Int?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = !isLocked,
        shape = RoundedCornerShape(18.dp),
        color = if (isLocked) colors.bg.copy(alpha = 0.5f) else colors.bg,
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = when {
                isLocked -> Color.Gray.copy(alpha = 0.2f)
                isActive -> colors.accent
                else     -> colors.accent.copy(alpha = 0.3f)
            }
        ),
        tonalElevation = 4.dp,
        shadowElevation = if (isActive && !isLocked) 8.dp else 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header row: emoji + name + status badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(season.emoji, fontSize = 40.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        season.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.accent
                    )
                    Text(
                        season.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isLocked -> Color.Gray.copy(alpha = 0.15f)
                        isActive -> AccentEasy.copy(alpha = 0.25f)
                        else     -> colors.accent.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        when {
                            isLocked -> "🔒 ${daysUntil}d"
                            isActive -> "🟢 Active"
                            else     -> "⏳ ${daysUntil}d"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = when {
                            isLocked -> Color.Gray
                            isActive -> AccentEasy
                            else     -> colors.accent
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Progress bar + stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp),
                        color = colors.accent,
                        trackColor = colors.accent.copy(alpha = 0.18f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$levelsCompleted / $totalLevels levels complete",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.accent.copy(alpha = 0.2f)
                ) {
                    Text(
                        if (levelsCompleted >= totalLevels) "✅ Done" else "▶ Level $currentLevel",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Expiry countdown + next milestone hints
            val showExtra = daysRemaining != null || nextMilestoneLevels != null
            if (showExtra) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (daysRemaining != null) {
                        val expiryColor = when {
                            daysRemaining <= 3  -> Color(0xFFEF4444)
                            daysRemaining <= 7  -> Color(0xFFF59E0B)
                            else                -> colors.accent.copy(alpha = 0.8f)
                        }
                        Text(
                            "⏰ ${daysRemaining}d left",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = expiryColor
                        )
                    }
                    if (nextMilestoneLevels != null && levelsCompleted < totalLevels) {
                        val levelsToGo = nextMilestoneLevels - levelsCompleted
                        Text(
                            "🎁 Reward in ${levelsToGo} lvl${if (levelsToGo != 1) "s" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
