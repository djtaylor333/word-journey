package com.djtaylor.wordjourney.ui.game.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djtaylor.wordjourney.domain.model.TileState
import com.djtaylor.wordjourney.ui.theme.*

private val ROW1 = listOf('Q','W','E','R','T','Y','U','I','O','P')
private val ROW2 = listOf('A','S','D','F','G','H','J','K','L')
private val ROW3 = listOf('Z','X','C','V','B','N','M')

@Composable
fun GameKeyboard(
    letterStates: Map<Char, TileState>,
    removedLetters: Set<Char>,
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    highContrast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textScale = LocalTextScale.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeyRow(ROW1, letterStates, removedLetters, onKey, highContrast, textScale)
        KeyRow(ROW2, letterStates, removedLetters, onKey, highContrast, textScale)
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionKey(label = "ENTER", onClick = onEnter, textScale = textScale)
            KeyRow(ROW3, letterStates, removedLetters, onKey, highContrast, textScale)
            ActionKey(label = "⌫", onClick = onDelete, textScale = textScale)
        }
    }
}

@Composable
private fun KeyRow(
    letters: List<Char>,
    letterStates: Map<Char, TileState>,
    removedLetters: Set<Char>,
    onKey: (Char) -> Unit,
    highContrast: Boolean,
    textScale: Float
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        letters.forEach { ch ->
            LetterKey(
                letter = ch,
                state = when {
                    removedLetters.contains(ch) -> TileState.ABSENT
                    else -> letterStates[ch] ?: TileState.EMPTY
                },
                enabled = !removedLetters.contains(ch),
                onClick = { onKey(ch) },
                highContrast = highContrast,
                textScale = textScale
            )
        }
    }
}

@Composable
private fun LetterKey(
    letter: Char,
    state: TileState,
    enabled: Boolean,
    onClick: () -> Unit,
    highContrast: Boolean,
    textScale: Float
) {
    var pressed by remember { mutableStateOf(false) }

    val theme = LocalGameTheme.current
    val targetBg = keyBackground(state, highContrast, theme)
    val bg by animateColorAsState(targetBg, tween(200), label = "keyBg$letter")
    val textColor = keyTextColor(state, theme)
    val alpha = if (enabled) 1f else 0.4f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(38.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg.copy(alpha = alpha))
            .clickable(enabled = enabled) {
                pressed = true
                onClick()
                pressed = false
            }
    ) {
        Text(
            text = letter.toString(),
            fontSize = (17 * textScale).sp,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = alpha)
        )
    }
}

@Composable
private fun ActionKey(label: String, onClick: () -> Unit, textScale: Float = 1f) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(58.dp)
            .defaultMinSize(minWidth = 54.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = ((if (label == "ENTER") 14f else 20f) * textScale).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun keyBackground(state: TileState, highContrast: Boolean, theme: com.djtaylor.wordjourney.domain.model.GameTheme): Color = when (state) {
    TileState.CORRECT -> if (highContrast) TileCorrectHC else theme.tileCorrect
    TileState.PRESENT -> if (highContrast) TilePresentHC else theme.tilePresent
    TileState.ABSENT  -> theme.tileAbsent
    else              -> theme.keyDefault
}

private fun keyTextColor(state: TileState, theme: com.djtaylor.wordjourney.domain.model.GameTheme): Color = when (state) {
    TileState.CORRECT, TileState.PRESENT, TileState.ABSENT -> Color.White
    else -> theme.keyText
}
