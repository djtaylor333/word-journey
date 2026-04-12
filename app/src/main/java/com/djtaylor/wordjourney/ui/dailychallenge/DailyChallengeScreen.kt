package com.djtaylor.wordjourney.ui.dailychallenge

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun DailyChallengeScreen(
    onNavigateToGame: (Int) -> Unit,   // wordLength
    onBack: () -> Unit,
    viewModel: DailyChallengeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Pulsing glow animation for playable cards
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Challenge", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A0533),
                            Color(0xFF0D1B2A)
                        )
                    )
                )
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Primary
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Streak Banner ──
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2A1F45),
                        border = BorderStroke(1.dp, Color(0xFFFF6B35).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StreakStat("🔥", "Streak", uiState.streak.toString())
                            StreakStat("⭐", "Best", uiState.bestStreak.toString())
                            StreakStat("🏆", "Wins", uiState.totalWins.toString())
                            StreakStat("📊", "Played", uiState.totalPlayed.toString())
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Today's Challenges",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    Text(
                        "One attempt per word length per day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Challenge Cards ──
                    ChallengeCard(
                        wordLength = 4,
                        emoji = "🌿",
                        label = "4-Letter Challenge",
                        description = "Easy • 6 guesses",
                        played = uiState.played4,
                        result = uiState.todayResults.firstOrNull { it.wordLength == 4 },
                        streak = uiState.streak4,
                        pulseAlpha = pulseAlpha,
                        accentColor = AccentEasy,
                        onClick = {
                            viewModel.playButtonClick()
                            onNavigateToGame(4)
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    ChallengeCard(
                        wordLength = 5,
                        emoji = "⚔️",
                        label = "5-Letter Challenge",
                        description = "Regular • 6 guesses",
                        played = uiState.played5,
                        result = uiState.todayResults.firstOrNull { it.wordLength == 5 },
                        streak = uiState.streak5,
                        pulseAlpha = pulseAlpha,
                        accentColor = AccentRegular,
                        onClick = {
                            viewModel.playButtonClick()
                            onNavigateToGame(5)
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    ChallengeCard(
                        wordLength = 6,
                        emoji = "🔥",
                        label = "6-Letter Challenge",
                        description = "Hard • 6 guesses",
                        played = uiState.played6,
                        result = uiState.todayResults.firstOrNull { it.wordLength == 6 },
                        streak = uiState.streak6,
                        pulseAlpha = pulseAlpha,
                        accentColor = AccentHard,
                        onClick = {
                            viewModel.playButtonClick()
                            onNavigateToGame(6)
                        }
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Streak Reward Info ──
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "🎁 Streak Rewards",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            StreakRewardRow("3-day streak", "+100 bonus coins")
                            StreakRewardRow("7-day streak", "+500 coins + 1 diamond")
                            StreakRewardRow("14-day streak", "+1000 coins + 3 diamonds")
                            StreakRewardRow("30-day streak", "+2000 coins + 5 diamonds + 1 life")
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    // ── Streak Shield Dialog ──────────────────────────────────────────────────
    if (uiState.showStreakShieldDialog) {
        StreakShieldDialog(
            streakDays = uiState.streakBeforeBreak,
            gemCost = uiState.streakShieldCostGems,
            onActivate = { viewModel.activateStreakShield() },
            onDismiss = { viewModel.dismissStreakShieldDialog() }
        )
    }
}

@Composable
private fun StreakShieldDialog(
    streakDays: Int,
    gemCost: Int,
    onActivate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A1F45),
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("💥", fontSize = 48.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Streak Broken!",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "You missed a day and lost your $streakDays-day streak.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF3D2B5A)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💎", fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Restore for $gemCost gems",
                            color = Color(0xFFB388FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "This allows you to return to your $streakDays-day streak.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onActivate,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text("🛡️ Restore Streak", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No thanks", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
private fun StreakStat(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 28.sp)
        Text(value, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

@Composable
private fun ChallengeCard(
    wordLength: Int,
    emoji: String,
    label: String,
    description: String,
    played: Boolean,
    result: com.djtaylor.wordjourney.data.db.DailyChallengeResultEntity?,
    streak: Int = 0,
    pulseAlpha: Float,
    accentColor: Color,
    onClick: () -> Unit
) {
    val cardColor = if (played) Color(0xFF1A2332) else Color(0xFF1E293B)
    val borderColor = when {
        result?.won == true -> TileCorrect
        played -> Color(0xFF475569)
        else -> accentColor.copy(alpha = pulseAlpha)
    }

    Surface(
        onClick = { if (!played) onClick() },
        enabled = !played,
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 42.sp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                if (streak > 0) {
                    Text(
                        "🔥 $streak-day streak",
                        color = Color(0xFFFF9800),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (result != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (result.won) {
                            val starStr = "⭐".repeat(result.stars)
                            Text(
                                "$starStr  ${result.guessCount}/6 guesses",
                                color = TileCorrect,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                "Not solved",
                                color = Color(0xFFEF4444),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            if (!played) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        "PLAY",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (result?.won == true) TileCorrect.copy(alpha = 0.2f)
                            else Color(0xFF475569).copy(alpha = 0.3f)
                        )
                ) {
                    Text(
                        if (result?.won == true) "✓" else "✗",
                        color = if (result?.won == true) TileCorrect else Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakRewardRow(milestone: String, reward: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(milestone, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        Text(reward, color = adaptiveCoinColor(!isSystemInDarkTheme()), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
