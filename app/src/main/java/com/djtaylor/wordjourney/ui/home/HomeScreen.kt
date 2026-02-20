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
import androidx.compose.ui.graphics.Brush
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
    onNavigateToGame: (String) -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showNoLivesDialog by remember { mutableStateOf(false) }
    var pendingDifficulty by remember { mutableStateOf<Difficulty?>(null) }

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency display
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
                    IconButton(onClick = onNavigateToStore) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Store",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Animated logo ─────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                // Outer rotating ring
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(compassRotation)
                        .clip(CircleShape)
                        .border(3.dp, Primary, CircleShape)
                )
                // Inner grid of 4 coloured tiles
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LogoTile(TileCorrect,  "W")
                        LogoTile(TilePresent,  "J")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LogoTile(TileAbsent,   "?")
                        LogoTile(TilePresent,  "!")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title
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

            Spacer(Modifier.height(20.dp))

            // ── Lives bar ─────────────────────────────────────────────────────
            LivesBar(
                lives = uiState.progress.lives,
                timerMs = uiState.timerDisplayMs
            )

            Spacer(Modifier.height(24.dp))

            // ── Difficulty cards ──────────────────────────────────────────────
            Text(
                "Choose Your Journey",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))

            for (difficulty in Difficulty.entries) {
                DifficultyCard(
                    difficulty = difficulty,
                    currentLevel = viewModel.levelForDifficulty(difficulty),
                    onClick = {
                        val result = viewModel.enterLevel(difficulty)
                        if (result == EnterLevelResult.NoLives) {
                            pendingDifficulty = difficulty
                            showNoLivesDialog = true
                        } else {
                            onNavigateToGame(difficulty.saveKey)
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // No-lives dialog is inline – implemented in game screen but triggered here too
    if (showNoLivesDialog) {
        AlertDialog(
            onDismissRequest = { showNoLivesDialog = false },
            title = { Text("Out of Lives!") },
            text  = {
                Column {
                    Text("You need at least 1 life to start a level.")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⏱ Next life in: ${formatTimerMs(uiState.timerDisplayMs)}",
                        color = AccentRegular,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showNoLivesDialog = false
                    onNavigateToStore()
                }) { Text("Go to Store") }
            },
            dismissButton = {
                TextButton(onClick = { showNoLivesDialog = false }) { Text("Wait") }
            }
        )
    }
}

@Composable
private fun LogoTile(color: Color, letter: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    ) {
        Text(letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(symbol, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LivesBar(lives: Int, timerMs: Long) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(minOf(lives, 10)) {
                    Text("❤️", fontSize = 20.sp)
                }
                if (lives > 10) {
                    Text("+${lives - 10}", color = HeartRed, fontWeight = FontWeight.Bold)
                }
            }
            if (timerMs > 0L) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Next life in ${formatTimerMs(timerMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(emoji, fontSize = 32.sp)
                Column {
                    Text(
                        difficulty.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = accent
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.2f)
                ) {
                    Text(
                        "Level $currentLevel",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
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
