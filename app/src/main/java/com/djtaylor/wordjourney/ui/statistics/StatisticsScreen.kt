package com.djtaylor.wordjourney.ui.statistics

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val p = uiState.progress
    val theme = LocalGameTheme.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            ThemeBackgroundOverlay(theme = theme)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ── Overall Stats ──
            StatSection("📊 Overview") {
                StatRow("Total Levels Completed", "${p.totalLevelsCompleted}")
                StatRow("Total Wins", "${p.totalWins}")
                StatRow("Total Guesses", "${p.totalGuesses}")
                StatRow("Win Rate", if (p.totalDailyChallengesPlayed + p.totalLevelsCompleted > 0)
                    "${(p.totalWins * 100.0 / (p.totalLevelsCompleted + maxOf(p.totalDailyChallengesPlayed, p.totalDailyChallengesCompleted))).toInt()}%"
                else "N/A")
                StatRow("Avg Guesses/Win", if (p.totalWins > 0)
                    "%.1f".format(p.totalGuesses.toFloat() / p.totalWins) else "N/A")
                StatRow("Items Used", "${p.totalItemsUsed}")
            }

            Spacer(Modifier.height(16.dp))

            // ── Star Ratings ──
            StatSection("⭐ Stars") {
                StatRow("Total Stars", "${uiState.totalStars}")
                StatRow("Perfect Levels (3★)", "${uiState.perfectLevels}")
                StatRow("Easy Stars", "${uiState.easyStars}")
                StatRow("Regular Stars", "${uiState.regularStars}")
                StatRow("Hard Stars", "${uiState.hardStars}")
            }

            Spacer(Modifier.height(16.dp))

            // ── Adventure Progress ──
            StatSection("🗺️ Adventure Progress") {
                StatRow("Easy Level", "${p.easyLevel}")
                StatRow("Regular Level", "${p.regularLevel}")
                StatRow("Hard Level", "${p.hardLevel}")
            }

            Spacer(Modifier.height(16.dp))

            // ── Daily Challenge ──
            StatSection("📅 Daily Challenge") {
                StatRow("Challenges Completed", "${p.totalDailyChallengesCompleted}")
                StatRow("Daily Wins", "${uiState.dailyWins}")
                StatRow("Daily Played", "${uiState.dailyPlayed}")
                StatRow("Current Streak", "${p.dailyChallengeStreak} 🔥")
                StatRow("Best Streak", "${p.dailyChallengeBestStreak}")
            }

            Spacer(Modifier.height(16.dp))

            // ── Login Streak ──
            StatSection("🏠 Login Streaks") {
                StatRow("Current Login Streak", "${p.loginStreak}")
                StatRow("Best Login Streak", "${p.loginBestStreak}")
            }

            Spacer(Modifier.height(16.dp))

            // ── Time Played ──
            StatSection("⏱️ Time Played") {
                StatRow("Total Time Played", p.totalTimePlayedMs.toReadableDuration())
                StatRow("Easy Levels", p.easyTimePlayedMs.toReadableDuration())
                StatRow("Regular Levels", p.regularTimePlayedMs.toReadableDuration())
                StatRow("Hard Levels", p.hardTimePlayedMs.toReadableDuration())
                StatRow("VIP Levels", p.vipTimePlayedMs.toReadableDuration())
                StatRow("Daily Challenge", p.dailyTimePlayedMs.toReadableDuration())
                StatRow("Timer Mode", p.timerTimePlayedMs.toReadableDuration())
            }

            Spacer(Modifier.height(16.dp))

            // ── Timer Mode ──
            StatSection("⏱️ Timer Mode") {
                fun Int.toTimerStr() = if (this > 0) "${this / 60}m ${this % 60}s" else "—"
                StatRow("Best Score (Easy)", if (p.timerBestLevelsEasy > 0)
                    "${p.timerBestLevelsEasy} words · ${p.timerBestTimeSecsEasy.toTimerStr()}" else "Not played")
                StatRow("Best Score (Regular)", if (p.timerBestLevelsRegular > 0)
                    "${p.timerBestLevelsRegular} words · ${p.timerBestTimeSecsRegular.toTimerStr()}" else "Not played")
                StatRow("Best Score (Hard)", if (p.timerBestLevelsHard > 0)
                    "${p.timerBestLevelsHard} words · ${p.timerBestTimeSecsHard.toTimerStr()}" else "Not played")
            }

            Spacer(Modifier.height(16.dp))

            // ── Economy ──
            StatSection("💰 Economy") {
                StatRow("Current Coins", "${p.coins}")
                StatRow("Total Coins Earned", "${p.totalCoinsEarned}")
                StatRow("Diamonds", "${p.diamonds}")
                StatRow("VIP", if (p.isVip) "Active" else "Not Active")
            }

            Spacer(Modifier.height(16.dp))

            // ── Achievements (Coming Soon) ──
            Box(modifier = Modifier.fillMaxWidth()) {
                StatSection("🏆 Achievements") {
                    StatRow("First Win", "???")
                    StatRow("Level Master", "???")
                    StatRow("Daily Streak", "???")
                    StatRow("Coin Collector", "???")
                    StatRow("VIP Explorer", "???")
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier.matchParentSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🏆", fontSize = 36.sp)
                            Text(
                                "Achievements",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                "Coming Soon!",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Track your progress and earn badges as you play",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
        } // end Box
    }
}

@Composable
private fun StatSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp
        )
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp
        )
    }
}

/**
 * Converts a millisecond duration to a human-readable string.
 * Examples: "2d 3h 15m", "45m 10s", "0m"
 */
private fun Long.toReadableDuration(): String {
    if (this <= 0L) return "0m"
    val totalSeconds = this / 1000
    val days    = totalSeconds / 86400
    val hours   = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val parts = buildList {
        if (days > 0) add("${days}d")
        if (hours > 0) add("${hours}h")
        if (minutes > 0 || (days == 0L && hours == 0L)) add("${minutes}m")
    }
    return parts.joinToString(" ")
}
