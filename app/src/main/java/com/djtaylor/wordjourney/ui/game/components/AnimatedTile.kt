package com.djtaylor.wordjourney.ui.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djtaylor.wordjourney.domain.model.TileState
import com.djtaylor.wordjourney.ui.theme.*

/**
 * A single animated letter tile.
 *
 * When [state] transitions to CORRECT/PRESENT/ABSENT the tile performs:
 *  - Scale-up bounce when [state] == FILLED (letter just typed)
 *  - Y-axis flip reveal when the state changes to an evaluated state
 */
@Composable
fun AnimatedTile(
    letter: Char?,
    state: TileState,
    tileIndex: Int,            // position in row, used for stagger delay
    tileSize: Dp,
    fontSize: Int,
    highContrast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isEvaluated = state in listOf(TileState.CORRECT, TileState.PRESENT, TileState.ABSENT)
    val isFilled = state == TileState.FILLED

    // ── Flip animation ────────────────────────────────────────────────────────
    var revealed by remember { mutableStateOf(!isEvaluated) }
    LaunchedEffect(state) {
        if (isEvaluated) revealed = true
    }

    val flipAngle by animateFloatAsState(
        targetValue = if (isEvaluated) 180f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = if (isEvaluated) tileIndex * 100 else 0,
            easing = LinearEasing
        ),
        label = "flipAngle"
    )

    // ── Scale bounce when letter typed ────────────────────────────────────────
    val scale by animateFloatAsState(
        targetValue = if (isFilled) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tileScale"
    )

    val bgColor = tileBackground(state, flipAngle, highContrast)
    val borderColor = tileBorder(state)
    val textColor = if (isEvaluated) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tileSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationY = if (flipAngle <= 90f) flipAngle else 180f - flipAngle
                cameraDistance = 8 * density
            }
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(
                width = if (state == TileState.EMPTY) 2.dp else 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        if (letter != null) {
            Text(
                // Flip the letter back on the second half of rotation so it's readable
                text = if (flipAngle > 90f && flipAngle < 270f) "" else letter.toString(),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private fun tileBackground(state: TileState, flipAngle: Float, highContrast: Boolean): Color {
    if (flipAngle <= 90f) {
        return when (state) {
            TileState.FILLED -> TileFilled
            else             -> TileEmpty
        }
    }
    return when (state) {
        TileState.CORRECT -> if (highContrast) TileCorrectHC else TileCorrect
        TileState.PRESENT -> if (highContrast) TilePresentHC else TilePresent
        TileState.ABSENT  -> TileAbsent
        TileState.FILLED  -> TileFilled
        TileState.EMPTY   -> TileEmpty
    }
}

private fun tileBorder(state: TileState): Color = when (state) {
    TileState.EMPTY   -> TileBorderEmpty
    TileState.FILLED  -> TileBorderFilled
    TileState.CORRECT -> TileCorrect
    TileState.PRESENT -> TilePresent
    TileState.ABSENT  -> TileAbsent
}
