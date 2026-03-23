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
 * The word is NEVER revealed here — player must continue or exit.
 */
@Composable
fun NeedMoreGuessesDialog(
    difficulty: Difficulty,
    currentLives: Int,
    coins: Long,
    diamonds: Int,
    addGuessItems: Int,
    onUseLife: () -> Unit,
    onUseAddGuessItem: () -> Unit,
    onUseCoinsForSingleGuess: () -> Unit,
    onGoToStore: () -> Unit,
    onMainMenu: () -> Unit
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
                    "Keep trying — you can still guess the word!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                HorizontalDivider()

                // Option 1: Use a life for full bonus attempts
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

                // Option 2: Use 200 coins for +1 guess
                Button(
                    onClick = onUseCoinsForSingleGuess,
                    enabled = coins >= 200L,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TilePresent)
                ) {
                    Text(
                        "⬡ Use 200 Coins (+1 guess)   [$coins coins]",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Option 3: Use Add Guess item from inventory
                OutlinedButton(
                    onClick = onUseAddGuessItem,
                    enabled = addGuessItems > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = if (addGuessItems > 0)
                        "➕ Use Guess Item (+1 guess)   [$addGuessItems in bag]"
                    else
                        "➕ No Guess Items in bag"
                    Text(label)
                }

                // Go to store
                TextButton(onClick = onGoToStore, modifier = Modifier.fillMaxWidth()) {
                    Text("Go to Store for more lives / coins")
                }

                HorizontalDivider()

                // Main menu / exit level
                TextButton(onClick = onMainMenu, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "🏠 Exit to Main Menu",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}
