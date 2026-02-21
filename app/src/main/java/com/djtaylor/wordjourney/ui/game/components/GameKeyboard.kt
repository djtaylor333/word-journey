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
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeyRow(ROW1, letterStates, removedLetters, onKey, highContrast)
        KeyRow(ROW2, letterStates, removedLetters, onKey, highContrast)
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionKey(label = "ENTER", onClick = onEnter)
            KeyRow(ROW3, letterStates, removedLetters, onKey, highContrast)
            ActionKey(label = "⌫", onClick = onDelete)
        }
    }
}

@Composable
private fun KeyRow(
    letters: List<Char>,
    letterStates: Map<Char, TileState>,
    removedLetters: Set<Char>,
    onKey: (Char) -> Unit,
    highContrast: Boolean
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
                highContrast = highContrast
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
    highContrast: Boolean
) {
    var pressed by remember { mutableStateOf(false) }

    val targetBg = keyBackground(state, highContrast)
    val bg by animateColorAsState(targetBg, tween(200), label = "keyBg$letter")
    val textColor = keyTextColor(state)
    val alpha = if (enabled) 1f else 0.4f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(36.dp)
            .height(56.dp)
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = alpha)
        )
    }
}

@Composable
private fun ActionKey(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(56.dp)
            .defaultMinSize(minWidth = 52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = if (label == "ENTER") 13.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun keyBackground(state: TileState, highContrast: Boolean): Color = when (state) {
    TileState.CORRECT -> if (highContrast) TileCorrectHC else TileCorrect
    TileState.PRESENT -> if (highContrast) TilePresentHC else TilePresent
    TileState.ABSENT  -> TileAbsent
    else              -> KeyDefaultDark
}

private fun keyTextColor(state: TileState): Color = when (state) {
    TileState.CORRECT, TileState.PRESENT, TileState.ABSENT -> Color.White
    else -> KeyTextDark
}
