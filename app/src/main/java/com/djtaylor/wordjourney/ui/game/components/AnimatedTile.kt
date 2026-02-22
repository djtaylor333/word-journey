package com.djtaylor.wordjourney.ui.game.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
 *
 * Colors are derived from [LocalGameTheme] so they change with the selected theme.
 */
@Composable
fun AnimatedTile(
    letter: Char?,
    state: TileState,
    tileIndex: Int,            // position in row, used for stagger delay
    tileSize: Dp,
    fontSize: Int,
    highContrast: Boolean = false,
    isLightTheme: Boolean = false,
    textScale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val isEvaluated = state in listOf(TileState.CORRECT, TileState.PRESENT, TileState.ABSENT)
    val isFilled = state == TileState.FILLED || state == TileState.HINT

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

    val theme = LocalGameTheme.current
    val rawBgColor = tileBackground(state, flipAngle, highContrast, isLightTheme, theme)
    val rawBorderColor = tileBorder(state, isLightTheme, theme)
    val bgColor by animateColorAsState(rawBgColor, animationSpec = tween(300), label = "tileBg")
    val borderColor by animateColorAsState(rawBorderColor, animationSpec = tween(300), label = "tileBorder")
    // High-contrast letters: white on dark/colored tiles, dark on light tiles
    val textColor = when {
        isEvaluated -> Color.White
        isLightTheme -> Color(0xFF1A1A1B)   // dark text on light tile background
        else -> Color.White                 // white text on dark tile background
    }

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
                // Hide only during the brief edge-on moment of the flip (around 90°)
                text = if (flipAngle in 80f..100f) "" else letter.toString(),
                fontSize = (fontSize * textScale).sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private fun tileBackground(state: TileState, flipAngle: Float, highContrast: Boolean, isLightTheme: Boolean, theme: com.djtaylor.wordjourney.domain.model.GameTheme): Color {
    if (flipAngle <= 90f) {
        return when (state) {
            TileState.FILLED -> if (isLightTheme) TileFilledLight else theme.tileFilled
            else             -> if (isLightTheme) TileEmptyLight else theme.tileEmpty
        }
    }
    return when (state) {
        TileState.CORRECT -> if (highContrast) TileCorrectHC else theme.tileCorrect
        TileState.PRESENT -> if (highContrast) TilePresentHC else theme.tilePresent
        TileState.ABSENT  -> theme.tileAbsent
        TileState.HINT   -> if (isLightTheme) Color(0xFFB3EBF2) else Color(0xFF00838F)
        TileState.FILLED -> if (isLightTheme) TileFilledLight else theme.tileFilled
        TileState.EMPTY   -> if (isLightTheme) TileEmptyLight else theme.tileEmpty
    }
}

private fun tileBorder(state: TileState, isLightTheme: Boolean, theme: com.djtaylor.wordjourney.domain.model.GameTheme): Color = when (state) {
    TileState.EMPTY   -> if (isLightTheme) TileBorderEmptyLight else TileBorderEmpty
    TileState.HINT    -> Color(0xFF00BCD4)
    TileState.FILLED  -> if (isLightTheme) TileBorderFilledLight else TileBorderFilled
    TileState.CORRECT -> theme.tileCorrect
    TileState.PRESENT -> theme.tilePresent
    TileState.ABSENT  -> theme.tileAbsent
}
