package com.djtaylor.wordjourney.ui.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.TileState
import com.djtaylor.wordjourney.ui.game.GameUiState
import com.djtaylor.wordjourney.ui.theme.LocalTextScale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameGrid(
    uiState: GameUiState,
    highContrast: Boolean = false,
    isLightTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val difficulty = uiState.difficulty
    val wordLen = uiState.wordLength
    val tileSize: Dp = when (wordLen) {
        3    -> 72.dp
        4    -> 68.dp
        5    -> 58.dp
        6    -> 50.dp
        7    -> 44.dp
        else -> 44.dp
    }
    val fontSize: Int = when (wordLen) {
        3    -> 28
        4    -> 26
        5    -> 22
        6    -> 18
        7    -> 16
        else -> 16
    }

    val textScale = LocalTextScale.current

    // Shake animation for invalid word on active row
    val shakeOffset by animateFloatAsState(
        targetValue = if (uiState.shakeCurrentRow) 1f else 0f,
        animationSpec = if (uiState.shakeCurrentRow)
            keyframes {
                durationMillis = 500
                0f at 0
                (-12f) at 60
                12f at 120
                (-10f) at 180
                10f at 240
                (-6f) at 320
                6f at 400
                0f at 500
            }
        else tween(0),
        label = "shakeOffset"
    )

    // Always enable scrolling so all rows are reachable on any screen size /
    // resolution. BringIntoViewRequester keeps the active row visible after
    // each submission; users can manually scroll up to review past guesses.
    val scrollState = rememberScrollState()
    val needsScroll = true

    // BringIntoViewRequester on the active row so we scroll just enough to reveal it
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val guessCount = uiState.guesses.size
    LaunchedEffect(guessCount) {
        if (guessCount == 0) {
            // Level just loaded — sit at the very top
            scrollState.scrollTo(0)
        } else {
            // A guess was submitted — bring the active input row into view
            // (BringIntoViewRequester scrolls the minimum amount needed)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = modifier.then(
            if (needsScroll) Modifier.verticalScroll(scrollState) else Modifier
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Completed guess rows
        uiState.guesses.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { colIndex, (char, state) ->
                    AnimatedTile(
                        letter = char,
                        state = state,
                        tileIndex = colIndex,
                        tileSize = tileSize,
                        fontSize = fontSize,
                        highContrast = highContrast,
                        isLightTheme = isLightTheme,
                        textScale = textScale
                    )
                }
            }
        }

        // Active (current input) row — shown if game is still in progress
        if (uiState.status == com.djtaylor.wordjourney.domain.model.GameStatus.IN_PROGRESS &&
            uiState.currentRow < uiState.maxGuesses) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    // Attach requester so bringIntoView() scrolls to show this row
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .graphicsLayer {
                        translationX = shakeOffset
                    }
            ) {
                val nonRevealedCols = (0 until wordLen).filter { !uiState.revealedLetters.containsKey(it) }
                repeat(wordLen) { col ->
                    val isRevealed = uiState.revealedLetters.containsKey(col)
                    val letter: Char? = when {
                        isRevealed -> uiState.revealedLetters[col]
                        else -> {
                            val userIdx = nonRevealedCols.indexOf(col)
                            if (userIdx >= 0) uiState.currentInput.getOrNull(userIdx) else null
                        }
                    }
                    val tileState = when {
                        isRevealed && letter != null -> TileState.HINT
                        letter != null -> TileState.FILLED
                        else -> TileState.EMPTY
                    }
                    AnimatedTile(
                        letter = letter,
                        state = tileState,
                        tileIndex = col,
                        tileSize = tileSize,
                        fontSize = fontSize,
                        highContrast = highContrast,
                        isLightTheme = isLightTheme,
                        textScale = textScale
                    )
                }
            }
        }

        // Empty placeholder rows for remaining guesses (after current row)
        val filledRows = uiState.guesses.size +
            (if (uiState.status == com.djtaylor.wordjourney.domain.model.GameStatus.IN_PROGRESS) 1 else 0)
        val emptyRows = (uiState.maxGuesses - filledRows).coerceAtLeast(0)

        repeat(emptyRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(wordLen) { col ->
                    AnimatedTile(
                        letter = null,
                        state = TileState.EMPTY,
                        tileIndex = col,
                        tileSize = tileSize,
                        fontSize = fontSize,
                        highContrast = highContrast,
                        isLightTheme = isLightTheme,
                        textScale = textScale
                    )
                }
            }
        }
    }
}
