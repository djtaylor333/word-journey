package com.djtaylor.wordjourney.ui.game.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.ui.theme.HeartRed
import com.djtaylor.wordjourney.ui.theme.TilePresent

/**
 * Shown when guesses are exhausted mid-level.
 * The word is NEVER revealed here — player must continue.
 */
@Composable
fun NeedMoreGuessesDialog(
    difficulty: Difficulty,
    currentLives: Int,
    coins: Long,
    diamonds: Int,
    onUseLife: () -> Unit,
    onUseAddGuessItem: () -> Unit,
    onGoToStore: () -> Unit
) {
    Dialog(onDismissRequest = { /* cannot dismiss — must choose */ }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("😰", fontSize = 48.sp)
                Text(
                    "Out of Guesses!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TilePresent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Keep trying — you can still guess the word! Use a life to get more attempts.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                HorizontalDivider()

                // Use a life button
                Button(
                    onClick = onUseLife,
                    enabled = currentLives > 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                ) {
                    Text(
                        "❤️ Use a Life (+${difficulty.bonusAttemptsPerLife} guesses)   [$currentLives remaining]",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Add a Guess item (costs 200 coins)
                OutlinedButton(
                    onClick = onUseAddGuessItem,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("➕ Add 1 Guess (200 coins)   [${coins} coins]")
                }

                // Go to store for more lives/coins
                TextButton(onClick = onGoToStore) {
                    Text("Go to Store for more lives / coins")
                }
            }
        }
    }
}
