package com.djtaylor.wordjourney.ui.starrewards

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.domain.model.ChestReward
import com.djtaylor.wordjourney.domain.model.StarChest
import com.djtaylor.wordjourney.ui.theme.Primary

// Chest emojis escalating from basic → legendary
private val CHEST_EMOJIS = listOf("🗃️", "📦", "🎁", "🧳", "💼", "🏺", "💎", "👑", "🌟", "🎊")
private val REGULAR_GRADIENT = listOf(
    listOf(Color(0xFF1E3A5F), Color(0xFF2D6A9F)),  // Level 1 — deep blue
    listOf(Color(0xFF1E4D3A), Color(0xFF2D8A5F)),  // Level 2 — forest green
    listOf(Color(0xFF4A2D6A), Color(0xFF7B3FA0)),  // Level 3 — purple
    listOf(Color(0xFF6A3D1E), Color(0xFFA05A2D)),  // Level 4 — amber
    listOf(Color(0xFF1A4A4A), Color(0xFF2D7A7A)),  // Level 5 — teal
    listOf(Color(0xFF5A1E3A), Color(0xFF9A2D5A)),  // Level 6 — ruby
    listOf(Color(0xFF3A3A1E), Color(0xFF7A7A2D)),  // Level 7 — olive gold
    listOf(Color(0xFF1E2A5A), Color(0xFF2D4A9A)),  // Level 8 — midnight blue
    listOf(Color(0xFF4A1E1E), Color(0xFF9A2D2D)),  // Level 9 — deep red
    listOf(Color(0xFF3A2A1A), Color(0xFF8A6030))   // Level 10 — legendary bronze
)
private val VIP_GRADIENT = listOf(
    Color(0xFF4A3500), Color(0xFF8A6500)      // VIP gold tone
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarRewardsScreen(
    onBack: () -> Unit,
    viewModel: StarRewardsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⭐ Star Rewards", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1A0533), Color(0xFF0D1B2A)))
                )
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Primary
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // ── Stars Summary Banner ──
                    item {
                        StarsHeaderBanner(
                            availableStars = uiState.availableStars,
                            totalStarsEarned = uiState.totalStarsEarned
                        )
                    }

                    // ── How It Works hint ──
                    item {
                        Text(
                            "Earn ⭐ stars by completing levels. Stars roll over — " +
                            "opened chests reset each month on the 1st.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ── Monthly Chests Header ──
                    item {
                        Text(
                            "🗓️ ${viewModel.currentMonthDisplayName()} Chests",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // ── Regular Chests ──
                    items(uiState.regularChests, key = { it.id }) { chest ->
                        val opened = chest.id in uiState.openedChestIds
                        ChestCard(
                            chest = chest,
                            opened = opened,
                            canAfford = uiState.availableStars >= chest.starCost,
                            gradientColors = REGULAR_GRADIENT.getOrElse(chest.level - 1) {
                                listOf(Color(0xFF1E3A5F), Color(0xFF2D6A9F))
                            },
                            chestEmoji = CHEST_EMOJIS.getOrElse(chest.level - 1) { "🎁" },
                            onOpen = { viewModel.openChest(chest) }
                        )
                    }

                    // ── VIP Section ──
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "👑 VIP Bonus Chests",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (!uiState.isVip) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFD700).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "🔒 VIP Only",
                                        color = Color(0xFFFFD700),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    items(uiState.vipChests, key = { it.id }) { chest ->
                        val opened = chest.id in uiState.openedChestIds
                        Box {
                            ChestCard(
                                chest = chest,
                                opened = opened,
                                canAfford = uiState.isVip && uiState.availableStars >= chest.starCost,
                                gradientColors = VIP_GRADIENT,
                                chestEmoji = "👑",
                                onOpen = { viewModel.openChest(chest) }
                            )
                            // Overlay for non-VIP
                            if (!uiState.isVip) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🔒 VIP Required", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }

            // ── Error snackbar ──
            uiState.errorMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) { Text(msg) }
            }
        }
    }

    // ── Reward Dialog ──
    uiState.openingResult?.let { result ->
        ChestRewardDialog(
            chest = result.chest,
            reward = result.reward,
            onDismiss = { viewModel.dismissReward() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StarsHeaderBanner(
    availableStars: Int,
    totalStarsEarned: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2A1F45),
        border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarStat("⭐", "Available", availableStars.toString())
            VerticalDivider(Modifier.height(40.dp), color = Color.White.copy(alpha = 0.15f))
            StarStat("🌟", "All Time", totalStarsEarned.toString())
        }
    }
}

@Composable
private fun StarStat(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 26.sp)
        Text(
            value,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700),
            fontSize = 22.sp
        )
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

@Composable
private fun ChestCard(
    chest: StarChest,
    opened: Boolean,
    canAfford: Boolean,
    gradientColors: List<Color>,
    chestEmoji: String,
    onOpen: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (!opened && canAfford) 1f else 0.98f,
        animationSpec = tween(300),
        label = "chestScale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = if (!opened && canAfford) 1.5.dp else 1.dp,
            color = if (opened) Color.White.copy(alpha = 0.2f)
                    else if (canAfford) Color.White.copy(alpha = 0.6f)
                    else Color.White.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        if (opened) listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A))
                        else gradientColors
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: emoji + info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Chest emoji with level badge
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Text(
                            if (opened) "✅" else chestEmoji,
                            fontSize = 40.sp
                        )
                        if (!opened) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    "Lv${chest.level}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Column {
                        Text(
                            if (opened) "Chest ${chest.level} — Opened ✓"
                            else "Chest ${chest.level}",
                            fontWeight = FontWeight.Bold,
                            color = if (opened) Color.White.copy(alpha = 0.4f) else Color.White,
                            fontSize = 16.sp
                        )
                        // Rewards preview
                        RewardPreviewText(chest = chest, opened = opened)
                    }
                }

                // Right: star cost + button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Star cost badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (canAfford && !opened) Color(0xFFFFD700).copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            "⭐ ${chest.starCost}",
                            color = if (canAfford && !opened) Color(0xFFFFD700)
                                    else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    // Open button
                    if (!opened) {
                        Button(
                            onClick = onOpen,
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) Color(0xFF7C3AED) else Color(0xFF3A3A3A),
                                disabledContainerColor = Color(0xFF2A2A2A)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                if (canAfford) "Open" else "🔒",
                                color = if (canAfford) Color.White else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardPreviewText(chest: StarChest, opened: Boolean) {
    val parts = buildList {
        if (chest.coins > 0) add("🪙 ${chest.coins}")
        if (chest.lives > 0) add("❤️ ${chest.lives}")
        if (chest.addGuessItems > 0) add("➕ ${chest.addGuessItems}")
        if (chest.removeLetterItems > 0) add("✂️ ${chest.removeLetterItems}")
        if (chest.gems > 0) add("💎 ${chest.gems}")
    }.joinToString("  ")

    Text(
        parts,
        color = if (opened) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.8f),
        fontSize = 13.sp
    )
}

@Composable
private fun ChestRewardDialog(
    chest: StarChest,
    reward: ChestReward,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "sparkleAlpha"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A1F45),
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(CHEST_EMOJIS.getOrElse(chest.level - 1) { "🎁" }, fontSize = 56.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Chest ${chest.level} Opened!",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "You earned:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                if (reward.coins > 0)     RewardRow("🪙", "${reward.coins} Coins",       sparkleAlpha)
                if (reward.lives > 0)     RewardRow("❤️", "${reward.lives} Lives",        sparkleAlpha)
                if (reward.addGuessItems > 0) RewardRow("➕", "${reward.addGuessItems} Add Guess", sparkleAlpha)
                if (reward.removeLetterItems > 0) RewardRow("✂️", "${reward.removeLetterItems} Remove Letter", sparkleAlpha)
                if (reward.gems > 0)      RewardRow("💎", "${reward.gems} Gems",          sparkleAlpha)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text("🎉 Claim!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    )
}

@Composable
private fun RewardRow(emoji: String, label: String, alpha: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = Color.White.copy(alpha = alpha),
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        )
    }
}
