package com.djtaylor.wordjourney.ui.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
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

@Composable
fun GameGrid(
    uiState: GameUiState,
    highContrast: Boolean = false,
    isLightTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val difficulty = uiState.difficulty
    val tileSize: Dp = when (difficulty.wordLength) {
        4    -> 68.dp
        5    -> 58.dp
        else -> 50.dp
    }
    val fontSize: Int = when (difficulty.wordLength) {
        4    -> 26
        5    -> 22
        else -> 18
    }

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

    // Enable scrolling when 8+ total rows
    val totalRows = uiState.maxGuesses
    val needsScroll = totalRows > 8
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when new guess is added
    val guessCount = uiState.guesses.size
    LaunchedEffect(guessCount) {
        if (needsScroll) {
            scrollState.animateScrollTo(scrollState.maxValue)
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
                        isLightTheme = isLightTheme
                    )
                }
            }
        }

        // Active (current input) row — shown if game is still in progress
        if (uiState.status == com.djtaylor.wordjourney.domain.model.GameStatus.IN_PROGRESS &&
            uiState.currentRow < uiState.maxGuesses) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer {
                    translationX = shakeOffset
                }
            ) {
                repeat(difficulty.wordLength) { col ->
                    val letter = uiState.currentInput.getOrNull(col)
                    AnimatedTile(
                        letter = letter,
                        state = if (letter != null) TileState.FILLED else TileState.EMPTY,
                        tileIndex = col,
                        tileSize = tileSize,
                        fontSize = fontSize,
                        highContrast = highContrast,
                        isLightTheme = isLightTheme
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
                repeat(difficulty.wordLength) { col ->
                    AnimatedTile(
                        letter = null,
                        state = TileState.EMPTY,
                        tileIndex = col,
                        tileSize = tileSize,
                        fontSize = fontSize,
                        highContrast = highContrast,
                        isLightTheme = isLightTheme
                    )
                }
            }
        }
    }
}
