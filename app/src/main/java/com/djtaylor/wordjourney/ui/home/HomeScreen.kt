package com.djtaylor.wordjourney.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.SeasonalThemeManager
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
    val isLight = !isSystemInDarkTheme()
    val coinColor = if (isLight) CoinGoldDark else CoinGold
    val diamondColor = if (isLight) DiamondCyanDark else DiamondCyan

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
        // Theme background decoration
        ThemeBackgroundOverlay(theme = LocalGameTheme.current)

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
                // Currency display — larger, with emoji
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CurrencyChip(
                        value = uiState.progress.coins.toString(),
                        color = coinColor,
                        symbol = "🪙"
                    )
                    CurrencyChip(
                        value = uiState.progress.diamonds.toString(),
                        color = diamondColor,
                        symbol = "💎"
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

            for (difficulty in Difficulty.entries.filter { it != Difficulty.VIP }) {
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

            // ── THEMED LEVEL PACKS ──────────────────────────────────────
            SectionHeader(emoji = "👑", title = "Themed Level Packs")
            Spacer(Modifier.height(8.dp))

            VipPackCard(
                isVip = uiState.progress.isVip,
                currentLevel = viewModel.levelForDifficulty(Difficulty.VIP),
                onClick = {
                    if (uiState.progress.isVip) {
                        viewModel.playButtonClick()
                        onNavigateToLevelSelect(Difficulty.VIP.saveKey)
                    }
                },
                onNavigateToStore = {
                    viewModel.playButtonClick()
                    onNavigateToStore()
                }
            )

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

            val seasonStatuses = remember { SeasonalThemeManager.getAllSeasonStatuses() }
            val activeSeason = seasonStatuses.firstOrNull { it.isActive }

            if (activeSeason != null) {
                // Active seasonal pack
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    tonalElevation = 2.dp,
                    border = BorderStroke(2.dp, Primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(activeSeason.season.emoji, fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${activeSeason.season.displayName} Pack",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentEasy.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "🟢 Active Now!",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = AccentEasy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            activeSeason.season.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Word pack coming soon",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = Primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Upcoming seasons grid — only show those within 50 days; rest = "More coming soon"
            val upcomingSeasons = seasonStatuses.filter { !it.isActive }
            val nearSeasons = upcomingSeasons.filter { it.daysUntil <= 50 }.take(4)
            val hasDistantSeasons = upcomingSeasons.any { it.daysUntil > 50 }

            if (nearSeasons.isNotEmpty() || hasDistantSeasons) {
                Spacer(Modifier.height(10.dp))
                nearSeasons.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { status ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(status.season.emoji, fontSize = 28.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        status.season.displayName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${status.daysUntil} days",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                        if (row.size < 2) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // "More Coming Soon" card for distant seasons
                if (hasDistantSeasons) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎁", fontSize = 28.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "More Coming Soon",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Additional themed packs on the way!",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
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
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(2.dp, Primary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp),
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
        shadowElevation = if (enabled) 4.dp else 0.dp,
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
            .shadow(4.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 1f),
                        color.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        Text(letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun CurrencyChip(value: String, color: Color, symbol: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        shadowElevation = 4.dp,
        border = BorderStroke(1.5f.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(symbol, fontSize = 22.sp)
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
        shadowElevation = 4.dp,
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
        Difficulty.VIP     -> CoinGold
    }
    val emoji = when (difficulty) {
        Difficulty.EASY    -> "🌿"
        Difficulty.REGULAR -> "⚔️"
        Difficulty.HARD    -> "🔥"
        Difficulty.VIP     -> "👑"
    }
    val description = when (difficulty) {
        Difficulty.EASY    -> "4-letter words • 6 guesses\nEvery 10 levels = +1 life"
        Difficulty.REGULAR -> "5-letter words • 6 guesses\nEvery 5 levels = +1 life"
        Difficulty.HARD    -> "6-letter words • 6 guesses\nEvery 3 levels = +1 life"
        Difficulty.VIP     -> "3-7 letter words • 6 guesses\nx2 rewards • 100 levels"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(2.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp),
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

@Composable
private fun VipPackCard(
    isVip: Boolean,
    currentLevel: Int,
    onClick: () -> Unit,
    onNavigateToStore: () -> Unit = {}
) {
    val accent = CoinGold
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = if (isVip) onClick else onNavigateToStore,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            tonalElevation = 2.dp,
            border = BorderStroke(2.dp, if (isVip) accent.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accent.copy(alpha = if (isVip) 0.12f else 0.04f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("👑", fontSize = 42.sp)
                    Column {
                        Text(
                            "VIP Challenge",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isVip) accent else Color.Gray,
                            fontSize = 22.sp
                        )
                        Text(
                            "100 levels • 3-7 letter words\nx2 rewards • Bonus life every 5 levels",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isVip) 0.7f else 0.4f),
                            fontSize = 14.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (isVip) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Level $currentLevel",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "🔒 VIP Only",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Locked overlay for non-VIP
        if (!isVip) {
            Surface(
                onClick = onNavigateToStore,
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.matchParentSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔒", fontSize = 32.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "VIP Subscription Required",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accent
                        ) {
                            Text(
                                "View in Store",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
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
