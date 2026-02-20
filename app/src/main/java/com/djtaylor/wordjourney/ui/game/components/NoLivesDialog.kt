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
import com.djtaylor.wordjourney.ui.theme.AccentRegular
import com.djtaylor.wordjourney.ui.theme.CoinGold
import com.djtaylor.wordjourney.ui.theme.DiamondCyan
import com.djtaylor.wordjourney.ui.theme.HeartRed
import java.util.concurrent.TimeUnit

/**
 * Shown when [lives] == 0 and the player tries to enter a level or
 * continue with more guesses mid-level.
 */
@Composable
fun NoLivesDialog(
    timerUntilNextLifeMs: Long,
    coins: Long,
    diamonds: Int,
    onTradeCoins: () -> Unit,
    onTradeDiamonds: () -> Unit,
    onGoToStore: () -> Unit,
    onWait: () -> Unit
) {
    Dialog(onDismissRequest = onWait) {
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
                Text("💔", fontSize = 52.sp)

                Text(
                    "Out of Lives!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HeartRed,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "You need at least 1 life to continue. You can wait for regeneration, trade coins, or buy more.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Countdown timer
                if (timerUntilNextLifeMs > 0L) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "⏱ Next life in ${formatMs(timerUntilNextLifeMs)}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = AccentRegular,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                HorizontalDivider()

                // Trade 1000 coins
                Button(
                    onClick = onTradeCoins,
                    enabled = coins >= 1000,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CoinGold)
                ) {
                    Text(
                        "⬡ Trade 1000 Coins for 1 Life   [$coins coins]",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Trade 3 diamonds
                OutlinedButton(
                    onClick = onTradeDiamonds,
                    enabled = diamonds >= 3,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("◆ Trade 3 Diamonds for 1 Life   [$diamonds diamonds]")
                }

                // Go to store
                TextButton(onClick = onGoToStore, modifier = Modifier.fillMaxWidth()) {
                    Text("Go to Store")
                }

                // Wait
                TextButton(onClick = onWait) {
                    Text("Wait for a free life")
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
