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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ── Overall Stats ──
            StatSection("📊 Overview") {
                StatRow("Total Levels Completed", "${p.totalLevelsCompleted}")
                StatRow("Total Wins", "${p.totalWins}")
                StatRow("Total Guesses", "${p.totalGuesses}")
                StatRow("Win Rate", if (p.totalGuesses > 0)
                    "${(p.totalWins * 100.0 / maxOf(p.totalLevelsCompleted + p.totalDailyChallengesCompleted, 1)).toInt()}%"
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

            // ── Economy ──
            StatSection("💰 Economy") {
                StatRow("Current Coins", "${p.coins}")
                StatRow("Total Coins Earned", "${p.totalCoinsEarned}")
                StatRow("Diamonds", "${p.diamonds}")
                StatRow("VIP", if (p.isVip) "Active" else "Not Active")
            }

            Spacer(Modifier.height(24.dp))
        }
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
