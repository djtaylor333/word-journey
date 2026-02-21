package com.djtaylor.wordjourney.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.ui.theme.*
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    onNavigateToLevelSelect: (String) -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDailyChallenge: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Rotating compass animation
    val infiniteTransition = rememberInfiniteTransition(label = "compass")
    val compassRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "compassRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency display — larger
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CurrencyChip(
                        value = uiState.progress.coins.toString(),
                        color = CoinGold,
                        symbol = "⬡"
                    )
                    CurrencyChip(
                        value = uiState.progress.diamonds.toString(),
                        color = DiamondCyan,
                        symbol = "◆"
                    )
                }
                Row {
                    IconButton(
                        onClick = { viewModel.playButtonClick(); onNavigateToStore() },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Store",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.playButtonClick(); onNavigateToSettings() },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Animated logo ─────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .rotate(compassRotation)
                        .clip(CircleShape)
                        .border(3.dp, Primary, CircleShape)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        LogoTile(TileCorrect, "W")
                        LogoTile(TilePresent, "J")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        LogoTile(TileAbsent, "?")
                        LogoTile(TilePresent, "!")
                    }
                }
            }

            Text(
                text = "Word Journeys",
                style = MaterialTheme.typography.headlineLarge,
                color = Primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Conquer the Lexicon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // ── Hearts display ────────────────────────────────────────────────
            HeartsBar(
                lives = uiState.progress.lives,
                timerMs = uiState.timerDisplayMs
            )

            Spacer(Modifier.height(20.dp))

            // ── ADVENTURE MODE ───────────────────────────────────────────────
            SectionHeader(emoji = "🗺️", title = "Adventure")
            Spacer(Modifier.height(8.dp))

            for (difficulty in Difficulty.entries) {
                DifficultyCard(
                    difficulty = difficulty,
                    currentLevel = viewModel.levelForDifficulty(difficulty),
                    onClick = {
                        viewModel.playButtonClick()
                        onNavigateToLevelSelect(difficulty.saveKey)
                    }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(16.dp))

            // ── DAILY CHALLENGE ──────────────────────────────────────────────
            SectionHeader(emoji = "📅", title = "Daily Challenge")
            Spacer(Modifier.height(8.dp))

            DailyChallengeCard(
                streak = uiState.dailyChallengeStreak,
                onClick = {
                    viewModel.playButtonClick()
                    onNavigateToDailyChallenge()
                }
            )

            Spacer(Modifier.height(16.dp))

            // ── THEMED PACKS ─────────────────────────────────────────────────
            SectionHeader(emoji = "🎁", title = "Themed Packs")
            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎃🎄🐣☀️❄️🦃", fontSize = 28.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Seasonal word packs — Coming Soon!",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Halloween, Christmas, Easter & more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── STATS & ACHIEVEMENTS ROW ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickNavCard(
                    emoji = "📊",
                    title = "Statistics",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.playButtonClick()
                        onNavigateToStatistics()
                    }
                )
                QuickNavCard(
                    emoji = "🏆",
                    title = "Achievements",
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    onClick = { /* Coming soon */ }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(emoji: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DailyChallengeCard(
    streak: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, Primary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("📅", fontSize = 42.sp)
                Column {
                    Text(
                        "Daily Challenge",
                        style = MaterialTheme.typography.titleLarge,
                        color = Primary,
                        fontSize = 22.sp
                    )
                    Text(
                        "3 new words every day\n4, 5, and 6 letters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
            if (streak > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥", fontSize = 24.sp)
                    Text(
                        "$streak",
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        fontSize = 18.sp
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        "Play!",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickNavCard(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (enabled) Primary.copy(alpha = 0.3f) else Color.Transparent),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            if (!enabled) {
                Text(
                    "Soon",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun LogoTile(color: Color, letter: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
    ) {
        Text(letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun CurrencyChip(value: String, color: Color, symbol: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(symbol, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HeartsBar(lives: Int, timerMs: Long) {
    val regularLives = minOf(lives, 10)
    val bonusLives = maxOf(lives - 10, 0)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Red heart with count inside
                Box(contentAlignment = Alignment.Center) {
                    Text("❤️", fontSize = 44.sp)
                    Text(
                        "$regularLives",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Plus sign
                Text(
                    "+",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BonusHeartBlue,
                )

                Spacer(Modifier.width(8.dp))

                // Blue heart with bonus count inside
                Box(contentAlignment = Alignment.Center) {
                    Text("💙", fontSize = 44.sp)
                    Text(
                        "$bonusLives",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
            }

            if (timerMs > 0L) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Next life in ${formatTimerMs(timerMs)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    difficulty: Difficulty,
    currentLevel: Int,
    onClick: () -> Unit
) {
    val accent = when (difficulty) {
        Difficulty.EASY    -> AccentEasy
        Difficulty.REGULAR -> AccentRegular
        Difficulty.HARD    -> AccentHard
    }
    val emoji = when (difficulty) {
        Difficulty.EASY    -> "🌿"
        Difficulty.REGULAR -> "⚔️"
        Difficulty.HARD    -> "🔥"
    }
    val description = when (difficulty) {
        Difficulty.EASY    -> "4-letter words • 6 guesses\nEvery 10 levels = +1 life"
        Difficulty.REGULAR -> "5-letter words • 6 guesses\nEvery 5 levels = +1 life"
        Difficulty.HARD    -> "6-letter words • 6 guesses\nEvery 3 levels = +1 life"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(emoji, fontSize = 42.sp)
                Column {
                    Text(
                        difficulty.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = accent,
                        fontSize = 24.sp
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.2f)
                ) {
                    Text(
                        "Level $currentLevel",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

private fun formatTimerMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
