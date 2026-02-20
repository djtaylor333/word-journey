package com.djtaylor.wordjourney.ui.levelselect

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    difficultyKey: String,
    onNavigateToGame: (String, Int) -> Unit,
    onBack: () -> Unit,
    viewModel: LevelSelectViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val difficulty = state.difficulty
    val accent = when (difficulty) {
        Difficulty.EASY    -> AccentEasy
        Difficulty.REGULAR -> AccentRegular
        Difficulty.HARD    -> AccentHard
    }

    // Heart shrink animation
    var showHeartAnim by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (showHeartAnim) 0.5f else 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "heartScale",
        finishedListener = {
            if (showHeartAnim) showHeartAnim = false
        }
    )

    // Pending level to navigate to after animation
    var pendingLevel by remember { mutableIntStateOf(0) }

    // Watch for lifeDeducted trigger
    LaunchedEffect(state.lifeDeducted) {
        if (state.lifeDeducted) {
            showHeartAnim = true
            delay(500)
            viewModel.resetLifeAnimation()
            if (pendingLevel > 0) {
                onNavigateToGame(difficultyKey, pendingLevel)
                pendingLevel = 0
            }
        }
    }

    // Grid state — scroll to focus on current level row
    val gridState = rememberLazyGridState()
    LaunchedEffect(state.currentLevel) {
        if (!state.isLoading) {
            val row = (state.currentLevel - 1) / 5
            gridState.animateScrollToItem(maxOf(0, row * 5 - 5))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${difficulty.displayName} Levels",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "${difficulty.wordLength}-letter words",
                            fontSize = 13.sp,
                            color = accent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    // Hearts display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "❤️",
                                fontSize = 28.sp,
                                modifier = Modifier.scale(heartScale)
                            )
                            Text(
                                "${state.lives}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.offset(y = 1.dp)
                            )
                        }
                        if (state.bonusLives > 0) {
                            Text(
                                "+",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BonusHeartBlue,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                            Box(contentAlignment = Alignment.Center) {
                                Text("💙", fontSize = 28.sp)
                                Text(
                                    "${state.bonusLives}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.offset(y = 1.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Continue / Next Level button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.timerDisplayMs > 0L && state.lives + state.bonusLives <= 0) {
                        Text(
                            "⏱ Next life in ${formatTimerMs(state.timerDisplayMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            val level = state.currentLevel
                            if (viewModel.canStartLevel(level)) {
                                if (viewModel.deductLifeForLevel(level)) {
                                    pendingLevel = level
                                    // Animation will trigger navigation
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent
                        ),
                        enabled = state.lives + state.bonusLives > 0
                    ) {
                        Text(
                            "Continue — Level ${state.currentLevel}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.totalLevels) { index ->
                    val level = index + 1
                    val isCompleted = level < state.currentLevel
                    val isCurrent = level == state.currentLevel
                    val isLocked = level > state.currentLevel
                    val isPlayable = isCompleted || isCurrent

                    LevelCell(
                        level = level,
                        isCompleted = isCompleted,
                        isCurrent = isCurrent,
                        isLocked = isLocked,
                        accent = accent,
                        onClick = {
                            if (!isPlayable) return@LevelCell
                            if (isCompleted) {
                                // Replay — no life cost, go directly
                                onNavigateToGame(difficultyKey, level)
                            } else {
                                // Current level — deduct life with animation
                                if (viewModel.deductLifeForLevel(level)) {
                                    pendingLevel = level
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // No lives dialog
    if (state.showNoLivesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoLivesDialog() },
            title = { Text("Out of Lives!", fontSize = 20.sp) },
            text = {
                Column {
                    Text(
                        "You need at least 1 life to start a new level.",
                        fontSize = 16.sp
                    )
                    if (state.timerDisplayMs > 0L) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⏱ Next life in ${formatTimerMs(state.timerDisplayMs)}",
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissNoLivesDialog() }) {
                    Text("OK", fontSize = 16.sp)
                }
            }
        )
    }
}

@Composable
private fun LevelCell(
    level: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLocked: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val bgColor = when {
        isCompleted -> TileCorrect
        isCurrent   -> CoinGold
        else        -> HeartRed.copy(alpha = 0.5f)
    }
    val textColor = Color.White
    val border = if (isCurrent) BorderStroke(3.dp, accent) else null

    Surface(
        onClick = onClick,
        enabled = !isLocked,
        shape = RoundedCornerShape(12.dp),
        color = if (isLocked) bgColor.copy(alpha = 0.4f) else bgColor,
        border = border,
        modifier = Modifier
            .aspectRatio(1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%02d".format(level),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) textColor.copy(alpha = 0.4f) else textColor
                )
                if (isCompleted) {
                    Text("✓", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
                if (isLocked) {
                    Text("🔒", fontSize = 10.sp)
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
