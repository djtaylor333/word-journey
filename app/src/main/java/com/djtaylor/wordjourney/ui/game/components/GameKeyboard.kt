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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djtaylor.wordjourney.domain.model.TileState
import com.djtaylor.wordjourney.ui.theme.*

private val ROW1 = listOf('Q','W','E','R','T','Y','U','I','O','P')
private val ROW2 = listOf('A','S','D','F','G','H','J','K','L')
private val ROW3 = listOf('Z','X','C','V','B','N','M')

// Gap between keys (dp). Slightly tighter than before so more room goes to key width.
private val KEY_GAP = 4.dp

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

    // Measure available width and derive key dimensions from it so the keyboard
    // scales correctly on every screen size without any key getting clipped.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        // Row 1 has 10 letter keys with 9 gaps → solve for key width.
        val keyWidth: Dp  = (maxWidth - KEY_GAP * 9) / 10
        // Action keys (ENTER / delete) are 1.5× a letter key.
        val actionWidth: Dp = keyWidth * 1.5f
        val keyHeight: Dp = 56.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Row 1: Q W E R T Y U I O P (10 keys) ──────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                ROW1.forEach { ch ->
                    LetterKey(ch, letterStates, removedLetters, onKey, highContrast, textScale,
                        Modifier.width(keyWidth).height(keyHeight))
                }
            }

            // ── Row 2: A S D F G H J K L (9 keys, centered) ───────────────
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                ROW2.forEach { ch ->
                    LetterKey(ch, letterStates, removedLetters, onKey, highContrast, textScale,
                        Modifier.width(keyWidth).height(keyHeight))
                }
            }

            // ── Row 3: ENTER + Z…M + ⌫ ───────────────────────────────────
            // Total: actionWidth + 7×keyWidth + actionWidth + 8×gap
            Row(
                horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionKey(
                    label = "ENTER",
                    onClick = onEnter,
                    textScale = textScale,
                    modifier = Modifier.width(actionWidth).height(keyHeight)
                )
                ROW3.forEach { ch ->
                    LetterKey(ch, letterStates, removedLetters, onKey, highContrast, textScale,
                        Modifier.width(keyWidth).height(keyHeight))
                }
                ActionKey(
                    label = "⌫",
                    onClick = onDelete,
                    textScale = textScale,
                    modifier = Modifier.width(actionWidth).height(keyHeight)
                )
            }
        }
    }
}

@Composable
private fun LetterKey(
    letter: Char,
    letterStates: Map<Char, TileState>,
    removedLetters: Set<Char>,
    onKey: (Char) -> Unit,
    highContrast: Boolean,
    textScale: Float,
    modifier: Modifier = Modifier
) {
    val state = when {
        removedLetters.contains(letter) -> TileState.ABSENT
        else -> letterStates[letter] ?: TileState.EMPTY
    }
    val enabled = !removedLetters.contains(letter)

    var pressed by remember { mutableStateOf(false) }
    val theme = LocalGameTheme.current
    val targetBg = keyBackground(state, highContrast, theme)
    val bg by animateColorAsState(targetBg, tween(200), label = "keyBg$letter")
    val textColor = keyTextColor(state, theme)
    val alpha = if (enabled) 1f else 0.4f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg.copy(alpha = alpha))
            .clickable(enabled = enabled) {
                pressed = true
                onKey(letter)
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
private fun ActionKey(
    label: String,
    onClick: () -> Unit,
    textScale: Float = 1f,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = ((if (label == "ENTER") 13f else 20f) * textScale).sp,
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
