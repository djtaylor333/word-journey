package com.djtaylor.wordjourney.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    difficultyKey: String,
    levelArg: Int,
    onNavigateHome: () -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToNextLevel: (String, Int) -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLightTheme = !isSystemInDarkTheme()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── TOP BAR ───────────────────────────────────────────────────────
            GameTopBar(
                uiState = uiState,
                onBack = onNavigateHome,
                onStore = onNavigateToStore
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
                            fontSize = 15.sp,
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
                        highContrast = false,
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
                    definitionUsed = uiState.definitionUsedThisLevel,
                    onAddGuess = { viewModel.useAddGuessItem() },
                    onRemoveLetter = { viewModel.useRemoveLetterItem() },
                    onDefinition = { viewModel.useDefinitionItem() }
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

    // ── DIALOGS ───────────────────────────────────────────────────────────────
    if (uiState.showWinDialog) {
        WinDialog(
            word = uiState.winWord,
            definition = uiState.winDefinition,
            coinsEarned = uiState.winCoinEarned,
            bonusLifeEarned = uiState.bonusLifeEarned,
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
}

@Composable
private fun GameTopBar(
    uiState: GameUiState,
    onBack: () -> Unit,
    onStore: () -> Unit
) {
    val difficultyColor = when (uiState.difficulty) {
        Difficulty.EASY    -> AccentEasy
        Difficulty.REGULAR -> AccentRegular
        Difficulty.HARD    -> AccentHard
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(34.dp)
            )
        }

        // Level + difficulty
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Level ${uiState.level}",
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = difficultyColor.copy(alpha = 0.2f)
            ) {
                Text(
                    uiState.difficulty.displayName,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    color = difficultyColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Lives + currencies — larger
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hearts: red with count + blue bonus
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    Text("❤️", fontSize = 34.sp)
                    Text(
                        "${uiState.regularLives}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
                if (uiState.bonusLives > 0) {
                    Text("+", fontSize = 14.sp, color = BonusHeartBlue, fontWeight = FontWeight.Bold)
                    Box(contentAlignment = Alignment.Center) {
                        Text("💙", fontSize = 26.sp)
                        Text(
                            "${uiState.bonusLives}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.offset(y = 1.dp)
                        )
                    }
                }
            }
            // Coins
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⬡", fontSize = 20.sp, color = CoinGold)
                Text(
                    "${uiState.coins}",
                    fontWeight = FontWeight.Bold,
                    color = CoinGold,
                    fontSize = 19.sp
                )
            }
            IconButton(onClick = onStore, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Store",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
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
    definitionUsed: Boolean,
    onAddGuess: () -> Unit,
    onRemoveLetter: () -> Unit,
    onDefinition: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        ItemButton(
            icon = "➕",
            label = "+1 Guess",
            ownedCount = addGuessCount,
            coinCost = 200,
            enabled = addGuessCount > 0 || coins >= 200,
            onClick = onAddGuess
        )
        ItemButton(
            icon = "🚫",
            label = "Remove",
            ownedCount = removeLetterCount,
            coinCost = 150,
            enabled = removeLetterCount > 0 || coins >= 150,
            onClick = onRemoveLetter
        )
        ItemButton(
            icon = "📖",
            label = "Define",
            ownedCount = definitionCount,
            coinCost = 300,
            enabled = definitionUsed || definitionCount > 0 || coins >= 300,
            subtitle = if (definitionUsed) "View 📖" else null,
            onClick = onDefinition
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
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
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
                Text(icon, fontSize = 24.sp)
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEasy.copy(alpha = alpha)
                        )
                    }
                }
            }
            Text(
                label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            // Subtitle / cost
            Text(
                text = subtitle ?: if (ownedCount > 0) "$ownedCount left" else "$coinCost ⬡",
                fontSize = 13.sp,
                fontWeight = if (ownedCount > 0 && subtitle == null) FontWeight.Bold else FontWeight.Normal,
                color = if (ownedCount > 0 && subtitle == null)
                    AccentEasy.copy(alpha = alpha)
                else
                    CoinGold.copy(alpha = alpha)
            )
        }
    }
}
