package com.djtaylor.wordjourney.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.GameStatus
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import com.djtaylor.wordjourney.ui.game.components.*
import com.djtaylor.wordjourney.ui.theme.*
import com.djtaylor.wordjourney.ui.theme.LocalHighContrast
import com.djtaylor.wordjourney.ui.theme.LocalColorblindMode
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    difficultyKey: String,
    levelArg: Int,
    onNavigateHome: () -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNextLevel: (String, Int) -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLightTheme = !isSystemInDarkTheme()
    val textScale = LocalTextScale.current

    // Show snackbar for transient messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissSnackbar()
        }
    }

    // Timer for no-lives countdown
    var timerMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(uiState.lives, uiState.showNoLivesDialog) {
        if (uiState.lives < LifeRegenUseCase.TIME_REGEN_CAP) {
            while (true) {
                delay(1000L)
                timerMs = (timerMs - 1000L).coerceAtLeast(0L)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Theme background decoration
            ThemeBackgroundOverlay(theme = LocalGameTheme.current)
            // VIP golden shimmer overlay
            if (uiState.difficulty == com.djtaylor.wordjourney.domain.model.Difficulty.VIP) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF59E0B).copy(alpha = 0.09f),
                                    Color(0xFFD97706).copy(alpha = 0.13f),
                                    Color(0xFFF59E0B).copy(alpha = 0.09f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // ── TOP BAR ───────────────────────────────────────────────────────
            GameTopBar(
                uiState = uiState,
                textScale = textScale,
                onBack = onNavigateHome,
                onStore = onNavigateToStore,
                onSettings = onNavigateToSettings
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                // Replay badge
                if (uiState.isReplay) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentEasy.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "🔄 Replay — No rewards or life cost",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = (15 * textScale).sp,
                            color = AccentEasy
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // ── GAME GRID ─────────────────────────────────────────────────
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    GameGrid(
                        uiState = uiState,
                        highContrast = LocalHighContrast.current || LocalColorblindMode.current != "none",
                        isLightTheme = isLightTheme
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── ITEMS BAR ─────────────────────────────────────────────────
                ItemsBar(
                    coins = uiState.coins,
                    addGuessCount = uiState.addGuessItems,
                    removeLetterCount = uiState.removeLetterItems,
                    definitionCount = uiState.definitionItems,
                    showLetterCount = uiState.showLetterItems,
                    definitionUsed = uiState.definitionUsedThisLevel,
                    isDailyChallenge = uiState.isDailyChallenge,
                    wordHasDefinition = uiState.wordHasDefinition,
                    textScale = textScale,
                    onAddGuess = { viewModel.useAddGuessItem() },
                    onRemoveLetter = { viewModel.useRemoveLetterItem() },
                    onDefinition = { viewModel.useDefinitionItem() },
                    onShowLetter = { viewModel.useShowLetterItem() }
                )

                Spacer(Modifier.height(8.dp))

                // ── KEYBOARD ─────────────────────────────────────────────────
                GameKeyboard(
                    letterStates = uiState.letterStates,
                    removedLetters = uiState.removedLetters,
                    onKey = { viewModel.onKeyPressed(it) },
                    onDelete = { viewModel.onDelete() },
                    onEnter = { viewModel.onSubmit() }
                )

                Spacer(Modifier.height(8.dp))
            }
        }
        }
    }

    // ── DIALOGS ───────────────────────────────────────────────────────────────
    if (uiState.showWinDialog) {
        WinDialog(
            word = uiState.winWord,
            definition = uiState.winDefinition,
            coinsEarned = uiState.winCoinEarned,
            bonusLifeEarned = uiState.bonusLifeEarned,
            starsEarned = uiState.starsEarned,
            isDailyChallenge = uiState.isDailyChallenge,
            onNextLevel = {
                val (diff, lvl) = viewModel.getNextLevelRoute()
                viewModel.nextLevel()
                onNavigateToNextLevel(diff, lvl)
            },
            onMainMenu = onNavigateHome
        )
    }

    if (uiState.showNeedMoreGuessesDialog && !uiState.showNoLivesDialog) {
        NeedMoreGuessesDialog(
            difficulty = uiState.difficulty,
            currentLives = uiState.lives,
            coins = uiState.coins,
            diamonds = uiState.diamonds,
            onUseLife = { viewModel.useLifeForMoreGuesses() },
            onUseAddGuessItem = { viewModel.useAddGuessItem() },
            onGoToStore = onNavigateToStore
        )
    }

    if (uiState.showNoLivesDialog) {
        NoLivesDialog(
            timerUntilNextLifeMs = timerMs,
            coins = uiState.coins,
            diamonds = uiState.diamonds,
            onTradeCoins = { viewModel.tradeCoinsForLife() },
            onTradeDiamonds = { viewModel.tradeDiamondsForLife() },
            onGoToStore = onNavigateToStore,
            onWait = { /* close dialog but stay on screen */ }
        )
    }

    if (uiState.showDefinitionDialog && uiState.definitionHint != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDefinitionDialog() },
            title = { Text("📖 Word Definition") },
            text = {
                Text(
                    uiState.definitionHint!!,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDefinitionDialog() }) {
                    Text("Got it")
                }
            }
        )
    }

    if (uiState.showDailyLossDialog) {
        AlertDialog(
            onDismissRequest = { /* non-dismissable */ },
            title = { Text("📅 Challenge Failed", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "The word was:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        uiState.dailyLossWord.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your daily challenge streak has been reset. Try again tomorrow!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = onNavigateHome) {
                    Text("Back to Challenges")
                }
            }
        )
    }
}

@Composable
private fun GameTopBar(
    uiState: GameUiState,
    textScale: Float = 1f,
    onBack: () -> Unit,
    onStore: () -> Unit,
    onSettings: () -> Unit
) {
    val isLight = !isSystemInDarkTheme()
    val coinColor = adaptiveCoinColor(isLight)
    val diamondColor = adaptiveDiamondColor(isLight)
    val difficultyColor = if (uiState.isDailyChallenge) Primary else when (uiState.difficulty) {
        Difficulty.EASY    -> AccentEasy
        Difficulty.REGULAR -> AccentRegular
        Difficulty.HARD    -> AccentHard
        Difficulty.VIP     -> CoinGold
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── ROW 1: Back, Level/Difficulty ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Level + difficulty tag centered
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (uiState.isDailyChallenge) "Daily Challenge" else "Level ${uiState.level}",
                    fontWeight = FontWeight.Bold,
                    fontSize = (if (uiState.isDailyChallenge) 20f else 24f).let { (it * textScale).sp },
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = difficultyColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        if (uiState.isDailyChallenge) "${uiState.difficulty.wordLength} Letters" else uiState.difficulty.displayName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        color = difficultyColor,
                        fontSize = (14 * textScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Hearts
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    Text("❤️", fontSize = (30 * textScale).sp)
                    Text(
                        "${uiState.regularLives}",
                        fontSize = (12 * textScale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
                if (uiState.bonusLives > 0) {
                    Text("+", fontSize = (12 * textScale).sp, color = BonusHeartBlue, fontWeight = FontWeight.Bold)
                    Box(contentAlignment = Alignment.Center) {
                        Text("💙", fontSize = (22 * textScale).sp)
                        Text(
                            "${uiState.bonusLives}",
                            fontSize = (10 * textScale).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.offset(y = 1.dp)
                        )
                    }
                }
            }
        }

        // ── ROW 2: Coins, Diamonds, Store, Settings ──────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coins
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙", fontSize = (20 * textScale).sp)
                Spacer(Modifier.width(2.dp))
                Text(
                    "${uiState.coins}",
                    fontWeight = FontWeight.Bold,
                    color = coinColor,
                    fontSize = (17 * textScale).sp
                )
            }
            Spacer(Modifier.width(12.dp))
            // Diamonds
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💎", fontSize = (18 * textScale).sp)
                Spacer(Modifier.width(2.dp))
                Text(
                    "${uiState.diamonds}",
                    fontWeight = FontWeight.Bold,
                    color = diamondColor,
                    fontSize = (17 * textScale).sp
                )
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = onStore, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Store",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(26.dp)
                )
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ItemsBar(
    coins: Long,
    addGuessCount: Int,
    removeLetterCount: Int,
    definitionCount: Int,
    showLetterCount: Int,
    definitionUsed: Boolean,
    isDailyChallenge: Boolean,
    wordHasDefinition: Boolean = true,
    textScale: Float = 1f,
    onAddGuess: () -> Unit,
    onRemoveLetter: () -> Unit,
    onDefinition: () -> Unit,
    onShowLetter: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        ItemButton(
            icon = "➕",
            label = "+1 Guess",
            ownedCount = addGuessCount,
            coinCost = 200,
            enabled = addGuessCount > 0 || coins >= 200,
            textScale = textScale,
            onClick = onAddGuess
        )
        ItemButton(
            icon = "🚫",
            label = "Remove",
            ownedCount = removeLetterCount,
            coinCost = 150,
            enabled = removeLetterCount > 0 || coins >= 150,
            textScale = textScale,
            onClick = onRemoveLetter
        )
        if (!isDailyChallenge) {
            ItemButton(
                icon = "📖",
                label = "Define",
                ownedCount = definitionCount,
                coinCost = 300,
                enabled = wordHasDefinition && (definitionUsed || definitionCount > 0 || coins >= 300),
                subtitle = if (definitionUsed) "View 📖" else if (!wordHasDefinition) "N/A" else null,
                textScale = textScale,
                onClick = onDefinition
            )
        }
        ItemButton(
            icon = "💡",
            label = "Reveal",
            ownedCount = showLetterCount,
            coinCost = 250,
            enabled = showLetterCount > 0 || coins >= 250,
            textScale = textScale,
            onClick = onShowLetter
        )
    }
}

@Composable
private fun ItemButton(
    icon: String,
    label: String,
    ownedCount: Int,
    coinCost: Int,
    enabled: Boolean,
    subtitle: String? = null,
    textScale: Float = 1f,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (enabled) 4.dp else 0.dp,
        color = if (enabled)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon row — show owned badge if any
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = (24 * textScale).sp)
                if (ownedCount > 0) {
                    Spacer(Modifier.width(3.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentEasy.copy(alpha = 0.25f),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            "×$ownedCount",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = (13 * textScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEasy.copy(alpha = alpha)
                        )
                    }
                }
            }
            Text(
                label,
                fontSize = (16 * textScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            // Subtitle / cost
            Text(
                text = subtitle ?: if (ownedCount > 0) "$ownedCount left" else "$coinCost 🪙",
                fontSize = (13 * textScale).sp,
                fontWeight = if (ownedCount > 0 && subtitle == null) FontWeight.Bold else FontWeight.Normal,
                color = if (ownedCount > 0 && subtitle == null)
                    AccentEasy.copy(alpha = alpha)
                else
                    CoinGold.copy(alpha = alpha)
            )
        }
    }
}
