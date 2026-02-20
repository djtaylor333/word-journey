package com.djtaylor.wordjourney.ui.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.djtaylor.wordjourney.ui.theme.*

@Composable
fun WinDialog(
    word: String,
    definition: String,
    coinsEarned: Long,
    bonusLifeEarned: Boolean,
    onNextLevel: () -> Unit,
    onMainMenu: () -> Unit
) {
    // Confetti bounce animation per letter tile
    val infiniteTransition = rememberInfiniteTransition(label = "winBounce")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -16f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Dialog(onDismissRequest = { /* must press button */ }) {
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
                // Animated trophy
                Text(
                    "🏆",
                    fontSize = 56.sp,
                    modifier = Modifier.graphicsLayer { translationY = bounce }
                )

                Text(
                    "Congratulations!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // The word — only shown here after winning
                Text(
                    word,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TileCorrect,
                    letterSpacing = 6.sp
                )

                // Definition
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Definition",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            definition.ifBlank { "No definition available." },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Coins earned
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CoinGold.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⬡", color = CoinGold, fontSize = 20.sp)
                        Text(
                            "+$coinsEarned coins",
                            color = CoinGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Bonus life notification
                if (bonusLifeEarned) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HeartRed.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("❤️", fontSize = 20.sp)
                            Text(
                                "Bonus life earned!",
                                color = HeartRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Buttons
                Button(
                    onClick = onNextLevel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TileCorrect)
                ) {
                    Text("Next Level ➡", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Main Menu")
                }
            }
        }
    }
}
