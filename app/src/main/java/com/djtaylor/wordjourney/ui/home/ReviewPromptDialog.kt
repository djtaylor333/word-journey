package com.djtaylor.wordjourney.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Two-step in-app review prompt dialog.
 *
 * Step 1: Asks "Do you enjoy Word Journeys?" (Yes / Not Really).
 * Step 2: Asks for a Play Store review regardless of the answer in step 1.
 *   - If the player said Yes → "Help others discover the game" framing.
 *   - If the player said No  → "Help us improve" framing.
 *
 * Tapping the review CTA button calls [onConfirmReview] which launches the
 * Play In-App Review API and grants the reward (5 lives + 1000 coins + 10 diamonds).
 * Tapping "Maybe Later" at any step calls [onDismiss].
 *
 * @param onConfirmReview Called when the player agrees to leave a review.
 * @param onDismiss       Called when the player dismisses without reviewing.
 */
@Composable
fun ReviewPromptDialog(
    onConfirmReview: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var likedGame by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = if (step == 1) "🎮" else if (likedGame) "⭐" else "💬",
                fontSize = 40.sp
            )
        },
        title = {
            Text(
                text = when {
                    step == 1 -> "Enjoying Word Journeys?"
                    likedGame -> "You're Amazing!"
                    else      -> "We Hear You!"
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = when {
                        step == 1 ->
                            "Have you been enjoying your Word Journeys experience so far?"
                        likedGame ->
                            "Can you please leave a review on the Play Store? Your support means the world to us and helps others discover the game! 🙏"
                        else ->
                            "Could you share some feedback or areas for improvement? Leave your thoughts on the Play Store — it really helps us make the game better for everyone!"
                    },
                    textAlign = TextAlign.Center
                )
                if (step == 2) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Text(
                        text = "🎁 Reward: +500 ⬡  +5 ❤️  +25 💎",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Rewards go to your inbox when you return!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1) {
                        likedGame = true
                        step = 2
                    } else {
                        onConfirmReview()
                    }
                }
            ) {
                Text(
                    text = when {
                        step == 1 -> "Yes! 😊"
                        likedGame -> "Rate on Play Store ⭐"
                        else      -> "Leave Feedback 💬"
                    }
                )
            }
        },
        dismissButton = {
            Column {
                if (step == 1) {
                    TextButton(
                        onClick = {
                            likedGame = false
                            step = 2
                        }
                    ) {
                        Text("Not Really")
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("Maybe Later")
                    }
                }
            }
        }
    )
}
