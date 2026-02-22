package com.djtaylor.wordjourney.ui.timermode

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.domain.model.GameStatus
import com.djtaylor.wordjourney.domain.model.TileState
import com.djtaylor.wordjourney.ui.game.components.AnimatedTile
import com.djtaylor.wordjourney.ui.game.components.GameKeyboard
import com.djtaylor.wordjourney.ui.theme.*
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerModeScreen(
    onBack: () -> Unit,
    viewModel: TimerModeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val theme = LocalGameTheme.current
    val highContrast = false // could wire to player prefs if needed
    val isLight = !isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ThemeBackgroundOverlay(theme = theme)

        when (uiState.phase) {
            TimerPhase.SETUP    -> SetupContent(
                uiState = uiState,
                onBack  = onBack,
                onSelectDifficulty = viewModel::selectDifficulty,
                onDismissRules = viewModel::dismissRules,
                onBegin = viewModel::beginSession
            )
            TimerPhase.COUNTDOWN -> CountdownContent(value = uiState.countdownValue)
            TimerPhase.PLAYING   -> PlayingContent(
                uiState      = uiState,
                onBack       = onBack,
                highContrast = highContrast,
                isLight      = isLight,
                onKey        = viewModel::onKeyPressed,
                onDelete     = viewModel::onDelete,
                onSubmit     = viewModel::onSubmit,
                onAddGuess   = viewModel::useAddGuessItem,
                onRemoveLetter = viewModel::useRemoveLetterItem,
                onDefinition = viewModel::useDefinitionItem,
                onShowLetter = viewModel::useShowLetterItem,
                onDismissDefinition = viewModel::dismissDefinitionDialog
            )
            TimerPhase.RECAP    -> RecapContent(
                uiState   = uiState,
                onBack    = onBack,
                onPlayAgain = viewModel::playAgain
            )
        }
    }
}

// ─────────────────────────────── SETUP ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupContent(
    uiState: TimerModeUiState,
    onBack: () -> Unit,
    onSelectDifficulty: (TimerDifficulty) -> Unit,
    onDismissRules: () -> Unit,
    onBegin: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("⏱️ Timer Mode", fontWeight = FontWeight.Bold) },
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
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select Difficulty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))

            TimerDifficulty.entries.forEach { diff ->
                DifficultySelectionCard(diff = diff, onClick = { onSelectDifficulty(diff) })
            }

            Spacer(Modifier.height(8.dp))

            // Quick rules preview
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("How it works", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    listOf(
                        "🟢 Easy — 4-letter words · 3 min",
                        "🟡 Regular — 5-letter words · 4 min",
                        "🔴 Hard — 6-letter words · 5 min",
                        "⏱️ +30 seconds per correct word",
                        "❤️ +1 life per 5 correct (VIP: +2 extra per 10)",
                        "✅ Only 'Define Word' item is free",
                        "🚫 No hearts needed to play"
                    ).forEach { line ->
                        Text(
                            line,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Rules dialog
    if (uiState.showRulesDialog) {
        val diff = uiState.selectedDifficulty ?: return
        RulesDialog(diff = diff, onDismiss = onDismissRules, onBegin = onBegin)
    }
}

@Composable
private fun DifficultySelectionCard(diff: TimerDifficulty, onClick: () -> Unit) {
    val colors = when (diff) {
        TimerDifficulty.EASY    -> Color(0xFF4CAF50)
        TimerDifficulty.REGULAR -> Color(0xFFFFC107)
        TimerDifficulty.HARD    -> Color(0xFFF44336)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(2.dp, colors.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(colors.copy(alpha = 0.1f), Color.Transparent)))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(diff.emoji, fontSize = 36.sp)
                Column {
                    Text(diff.label, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors)
                    Text(
                        "${diff.wordLength}-letter words · ${diff.startTimeSecs / 60} min start",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
            Surface(shape = RoundedCornerShape(10.dp), color = colors.copy(alpha = 0.2f)) {
                Text(
                    "Play",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = colors, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RulesDialog(diff: TimerDifficulty, onDismiss: () -> Unit, onBegin: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "${diff.emoji} ${diff.label} Mode",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rules", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val normalCoins = when (diff) {
                    TimerDifficulty.EASY -> "200"
                    TimerDifficulty.REGULAR -> "200"
                    TimerDifficulty.HARD -> "200"
                }
                listOf(
                    "• Guess as many ${diff.wordLength}-letter words as you can",
                    "• Starting time: ${diff.startTimeSecs / 60} minutes",
                    "• Correct guess = +30 seconds",
                    "• Standard Wordle rules — 6 attempts per word",
                    "• Fail a word? Move on, no time bonus",
                    "",
                    "❤️ Life rewards:",
                    "  Normal: +1 life every 5 correct words",
                    "  VIP: +1 life per 5, +2 extra per 10 correct",
                    "",
                    "🎒 Items:",
                    "  • Define Word — FREE to use",
                    "  • Other items cost from inventory",
                    "  • No coin spending in Timer Mode",
                    "",
                    "🚫 No hearts needed to play"
                ).forEach { line ->
                    if (line.isEmpty()) Spacer(Modifier.height(2.dp))
                    else Text(line, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                }
            }
        },
        confirmButton = {
            Button(onClick = onBegin) {
                Text("Begin!", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─────────────────────────────── COUNTDOWN ───────────────────────────────────

@Composable
private fun CountdownContent(value: Int) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "countdownScale"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (value > 0) value.toString() else "GO!",
                fontSize = 120.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            )
            Text(
                "Get ready…",
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────── PLAYING ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayingContent(
    uiState: TimerModeUiState,
    onBack: () -> Unit,
    highContrast: Boolean,
    isLight: Boolean,
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    onAddGuess: () -> Unit,
    onRemoveLetter: () -> Unit,
    onDefinition: () -> Unit,
    onShowLetter: () -> Unit,
    onDismissDefinition: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    // Snackbar host
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { snackbarHost.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                // Minimal top bar — just timer and back
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    // Remaining timer
                    TimerDisplay(remainingMs = uiState.remainingMs)
                    // Words correct + lives progress
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "✅ ${uiState.wordsCorrect}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = accent
                        )
                        val nextLifeAt = ((uiState.wordsCorrect / 5) + 1) * 5
                        Text(
                            "❤️ ${nextLifeAt - uiState.wordsCorrect} to next life",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                    }
                }
                // Timer bar
                TimerProgressBar(remainingMs = uiState.remainingMs,
                    difficultySecs = uiState.selectedDifficulty?.startTimeSecs?.toLong()?.times(1000L) ?: 180_000L)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 4.dp)
            ) {
                // Items bar
                TimerItemsBar(
                    addGuessItems   = uiState.addGuessItems,
                    removeLetterItems = uiState.removeLetterItems,
                    definitionItems = uiState.definitionItems,
                    showLetterItems = uiState.showLetterItems,
                    onAddGuess      = onAddGuess,
                    onRemoveLetter  = onRemoveLetter,
                    onDefinition    = onDefinition,
                    onShowLetter    = onShowLetter
                )
                Spacer(Modifier.height(4.dp))
                // Keyboard
                GameKeyboard(
                    letterStates   = uiState.letterStates,
                    removedLetters = uiState.removedLetters,
                    onKey          = { if (uiState.wordStatus == GameStatus.IN_PROGRESS) onKey(it) },
                    onDelete       = { if (uiState.wordStatus == GameStatus.IN_PROGRESS) onDelete() },
                    onEnter        = { if (uiState.wordStatus == GameStatus.IN_PROGRESS) onSubmit() }
                )
            }
        }
    ) { padding ->
        // Word status overlay banner
        val statusBanner: String? = when (uiState.wordStatus) {
            GameStatus.WON  -> "🎉 Correct! +30s"
            GameStatus.LOST -> "❌ Better luck next time!"
            else            -> null
        }

        Box(Modifier.fillMaxSize().padding(padding)) {
            TimerGrid(
                guesses            = uiState.guesses,
                currentInput       = uiState.currentInput,
                maxGuesses         = uiState.maxGuesses,
                wordLength         = uiState.wordLength,
                prefilledPositions = uiState.prefilledPositions,
                wordStatus         = uiState.wordStatus,
                shakeRow           = uiState.shakeRow,
                highContrast       = highContrast,
                isLight            = isLight,
                modifier           = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
            )

            statusBanner?.let {
                Surface(
                    modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    shape     = RoundedCornerShape(12.dp),
                    color     = if (uiState.wordStatus == GameStatus.WON) Color(0xFF388E3C).copy(alpha = 0.95f)
                                else Color(0xFFD32F2F).copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }
            }
        }
    }

    // Definition dialog
    if (uiState.showDefinitionDialog && uiState.definitionHint != null) {
        AlertDialog(
            onDismissRequest = onDismissDefinition,
            title = { Text("📖 Definition", fontWeight = FontWeight.Bold) },
            text  = { Text(uiState.definitionHint, fontSize = 15.sp) },
            confirmButton = {
                TextButton(onClick = onDismissDefinition) { Text("Got it") }
            }
        )
    }
}

@Composable
private fun TimerDisplay(remainingMs: Long) {
    val secs    = (remainingMs / 1_000).toInt()
    val minutes = secs / 60
    val seconds = secs % 60
    val color = when {
        secs <= 10  -> Color(0xFFF44336)
        secs <= 30  -> Color(0xFFFF9800)
        else        -> MaterialTheme.colorScheme.primary
    }
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (secs <= 10) 0.4f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
        label = "timerAlpha"
    )
    Text(
        text   = "%d:%02d".format(minutes, seconds),
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        color  = color.copy(alpha = if (secs <= 10) alpha else 1f)
    )
}

@Composable
private fun TimerProgressBar(remainingMs: Long, difficultySecs: Long) {
    val fraction = (remainingMs.toFloat() / difficultySecs.toFloat()).coerceIn(0f, 1f)
    val barColor by animateColorAsState(
        targetValue = when {
            fraction < 0.1f -> Color(0xFFF44336)
            fraction < 0.3f -> Color(0xFFFF9800)
            else            -> Color(0xFF4CAF50)
        },
        animationSpec = tween(300),
        label = "barColor"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(barColor)
        )
    }
}

@Composable
private fun TimerItemsBar(
    addGuessItems: Int,
    removeLetterItems: Int,
    definitionItems: Int,
    showLetterItems: Int,
    onAddGuess: () -> Unit,
    onRemoveLetter: () -> Unit,
    onDefinition: () -> Unit,
    onShowLetter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ItemButton(emoji = "➕", label = "Add\nGuess", count = addGuessItems, free = false, onClick = onAddGuess)
        ItemButton(emoji = "🚫", label = "Remove\nLetter", count = removeLetterItems, free = false, onClick = onRemoveLetter)
        ItemButton(emoji = "📖", label = "Define\nWord", count = definitionItems, free = true, onClick = onDefinition)
        ItemButton(emoji = "🔍", label = "Show\nLetter", count = showLetterItems, free = false, onClick = onShowLetter)
    }
}

@Composable
private fun ItemButton(emoji: String, label: String, count: Int, free: Boolean, onClick: () -> Unit) {
    val accent = if (free) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    val enabled = free || count > 0
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.5f) else Color.Transparent),
        modifier = Modifier.width(76.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(
                label, fontSize = 10.sp, textAlign = TextAlign.Center,
                color = if (enabled) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                lineHeight = 12.sp
            )
            if (free) {
                Text("FREE", fontSize = 9.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            } else {
                Text("x$count", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (count > 0) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}

// ─────────────────────────────── TIMER GRID ──────────────────────────────────

@Composable
private fun TimerGrid(
    guesses: List<List<Pair<Char, TileState>>>,
    currentInput: List<Char>,
    maxGuesses: Int,
    wordLength: Int,
    prefilledPositions: Map<Int, Char>,
    wordStatus: GameStatus,
    shakeRow: Boolean,
    highContrast: Boolean,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val tileSize: Dp = when (wordLength) {
        4 -> 68.dp
        5 -> 58.dp
        6 -> 50.dp
        else -> 52.dp
    }
    val fontSize: Int = when (wordLength) {
        4 -> 26; 5 -> 22; 6 -> 18; else -> 18
    }

    val shakeOffset by animateFloatAsState(
        targetValue = if (shakeRow) 1f else 0f,
        animationSpec = if (shakeRow) keyframes {
            durationMillis = 500
            0f at 0; (-12f) at 60; 12f at 120; (-10f) at 180
            10f at 240; (-6f) at 320; 6f at 400; 0f at 500
        } else tween(0),
        label = "shakeOffset"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Completed rows
        guesses.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { colIndex, (char, state) ->
                    AnimatedTile(letter = char, state = state, tileIndex = colIndex,
                        tileSize = tileSize, fontSize = fontSize, highContrast = highContrast, isLightTheme = isLight)
                }
            }
        }

        // Active row
        if (wordStatus == GameStatus.IN_PROGRESS && guesses.size < maxGuesses) {
            val freePositions = (0 until wordLength).filter { it !in prefilledPositions }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer { translationX = shakeOffset }
            ) {
                repeat(wordLength) { col ->
                    val isPrefilled = col in prefilledPositions
                    val letter: Char? = when {
                        isPrefilled -> prefilledPositions[col]
                        else -> {
                            val userIdx = freePositions.indexOf(col)
                            if (userIdx >= 0) currentInput.getOrNull(userIdx) else null
                        }
                    }
                    val state = when {
                        isPrefilled && letter != null -> TileState.HINT
                        letter != null -> TileState.FILLED
                        else -> TileState.EMPTY
                    }
                    AnimatedTile(letter = letter, state = state, tileIndex = col,
                        tileSize = tileSize, fontSize = fontSize, highContrast = highContrast, isLightTheme = isLight)
                }
            }
        }

        // Empty rows
        val filledRows = guesses.size + if (wordStatus == GameStatus.IN_PROGRESS) 1 else 0
        val emptyRows  = (maxGuesses - filledRows).coerceAtLeast(0)
        repeat(emptyRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(wordLength) { col ->
                    AnimatedTile(letter = null, state = TileState.EMPTY, tileIndex = col,
                        tileSize = tileSize, fontSize = fontSize, highContrast = highContrast, isLightTheme = isLight)
                }
            }
        }
    }
}

// ─────────────────────────────── RECAP ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecapContent(
    uiState: TimerModeUiState,
    onBack: () -> Unit,
    onPlayAgain: () -> Unit
) {
    val diff = uiState.selectedDifficulty
    val accent = MaterialTheme.colorScheme.primary

    fun Int.toTimeStr(): String {
        val m = this / 60
        val s = this % 60
        return "%d:%02d".format(m, s)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("⏱️ Time's Up!", fontWeight = FontWeight.Bold) },
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
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Big score
            Text(
                text = "${uiState.wordsCorrect}",
                fontSize = 80.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                "words correct",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(4.dp))

            // Session stats
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This Session", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    HorizontalDivider()
                    RecapRow("⏱️ Total time played", uiState.recapTotalSecs.toTimeStr())
                    RecapRow("✅ Words correct", "${uiState.wordsCorrect}")
                    RecapRow("📝 Words attempted", "${uiState.wordsAttempted}")
                    RecapRow("❤️ Lives earned (inbox)", "${uiState.livesEarned}")
                    RecapRow("⚡ Bonus time earned", "${uiState.totalBonusSecs}s (${uiState.wordsCorrect} × 30s)")
                    if (diff != null) RecapRow("🎯 Difficulty", "${diff.emoji} ${diff.label}")
                }
            }

            // Best score
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🏆 Best Scores${if (diff != null) " · ${diff.emoji} ${diff.label}" else ""}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    HorizontalDivider()
                    RecapRow("🥇 Best words", "${uiState.bestLevels}")
                    RecapRow("⏱️ Best time", uiState.bestTimeSecs.toTimeStr())
                    if (uiState.wordsCorrect >= uiState.bestLevels && uiState.wordsCorrect > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "🎉 NEW BEST SCORE!",
                            color = Color(0xFF4CAF50), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            if (uiState.livesEarned > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF388E3C).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "💌 ${uiState.livesEarned} ${if (uiState.livesEarned == 1) "life has" else "lives have"} been added to your Inbox!",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = Color(0xFF388E3C),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Action buttons
            Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Play Again", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Back to Home", fontSize = 17.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RecapRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
