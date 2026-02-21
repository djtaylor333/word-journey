package com.djtaylor.wordjourney.ui.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.djtaylor.wordjourney.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

// ── Confetti particle data ───────────────────────────────────────────────────
private data class ConfettiParticle(
    val x: Float,        // 0..1 fraction of width
    val speed: Float,     // vertical speed multiplier
    val size: Float,      // base size in dp
    val color: Color,
    val rotation: Float,  // rotation speed
    val delay: Float      // start delay fraction
)

private val confettiColors = listOf(
    Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFF3B82F6),
    Color(0xFFEF4444), Color(0xFFA855F7), Color(0xFF06B6D4),
    Color(0xFFF97316), Color(0xFFEC4899)
)

@Composable
fun WinDialog(
    word: String,
    definition: String,
    coinsEarned: Long,
    bonusLifeEarned: Boolean,
    starsEarned: Int = 0,
    isDailyChallenge: Boolean = false,
    onNextLevel: () -> Unit,
    onMainMenu: () -> Unit
) {
    // Trophy bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "winBounce")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -16f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Confetti animation progress
    val confettiProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        confettiProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
        )
    }

    // Generate confetti particles once
    val particles = remember {
        val rng = Random(42)
        (0 until 30).map {
            ConfettiParticle(
                x = rng.nextFloat(),
                speed = 0.5f + rng.nextFloat() * 0.8f,
                size = 4f + rng.nextFloat() * 6f,
                color = confettiColors[rng.nextInt(confettiColors.size)],
                rotation = rng.nextFloat() * 360f,
                delay = rng.nextFloat() * 0.3f
            )
        }
    }

    // Animated coin counter
    var displayedCoins by remember { mutableLongStateOf(0L) }
    LaunchedEffect(coinsEarned) {
        if (coinsEarned <= 0) { displayedCoins = 0; return@LaunchedEffect }
        val steps = 20
        val stepDelay = 600L / steps
        for (i in 1..steps) {
            displayedCoins = (coinsEarned * i) / steps
            delay(stepDelay)
        }
        displayedCoins = coinsEarned
    }

    // Coin pop scale animation
    val coinScale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        delay(200)
        coinScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    // Bonus life pop
    val lifeScale = remember { Animatable(0f) }
    LaunchedEffect(bonusLifeEarned) {
        if (bonusLifeEarned) {
            delay(700)
            lifeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Dialog entry scale
    val dialogScale = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        dialogScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Dialog(onDismissRequest = { /* must press button */ }) {
        Box(contentAlignment = Alignment.Center) {
            // ── Confetti layer ───────────────────────────────────────────
            Canvas(modifier = Modifier.matchParentSize()) {
                val progress = confettiProgress.value
                for (p in particles) {
                    val t = ((progress - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                    if (t <= 0f) continue
                    val px = p.x * size.width
                    val py = -20f + t * (size.height + 40f) * p.speed
                    val wobble = sin(t * 8f + p.rotation) * 15f
                    val alpha = if (t > 0.7f) (1f - t) / 0.3f else 1f
                    drawRect(
                        color = p.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                        topLeft = Offset(px + wobble, py),
                        size = Size(p.size.dp.toPx(), p.size.dp.toPx() * 0.6f)
                    )
                }
            }

            // ── Dialog card ──────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.scale(dialogScale.value)
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

                    // Star rating
                    if (starsEarned > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(3) { i ->
                                Text(
                                    if (i < starsEarned) "⭐" else "☆",
                                    fontSize = if (i < starsEarned) 32.sp else 28.sp,
                                    color = if (i < starsEarned)
                                        Color.Unspecified
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    Text(
                        if (isDailyChallenge) "Daily Challenge Complete!" else "Congratulations!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // The word
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
                    if (coinsEarned > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CoinGold.copy(alpha = 0.15f),
                            modifier = Modifier.scale(coinScale.value)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⬡", color = CoinGold, fontSize = 24.sp)
                                Text(
                                    "+$displayedCoins coins",
                                    color = CoinGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }

                    // Bonus life notification
                    if (bonusLifeEarned) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HeartRed.copy(alpha = 0.15f),
                            modifier = Modifier.scale(lifeScale.value)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("❤️", fontSize = 22.sp)
                                Text(
                                    "+1 Bonus Life!",
                                    color = HeartRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Buttons
                    if (!isDailyChallenge) {
                        Button(
                            onClick = onNextLevel,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TileCorrect)
                        ) {
                            Text("Next Level ➡", fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = onMainMenu,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isDailyChallenge) "Back to Challenges" else "Main Menu")
                    }
                }
            }
        }
    }
}
