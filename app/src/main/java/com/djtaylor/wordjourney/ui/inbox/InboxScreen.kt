package com.djtaylor.wordjourney.ui.inbox

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.data.db.InboxItemEntity
import com.djtaylor.wordjourney.ui.theme.TileCorrect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Inbox", fontWeight = FontWeight.Bold)
                        if (uiState.unclaimedCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Badge { Text(uiState.unclaimedCount.toString()) }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val unclaimed = uiState.items.filter { !it.claimed }
        val claimed   = uiState.items.filter { it.claimed }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Claim-All button
            if (unclaimed.size > 1) {
                item {
                    Button(
                        onClick = { viewModel.claimAll() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Claim All (${unclaimed.size} rewards)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Unclaimed items
            if (unclaimed.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📬", fontSize = 56.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "All caught up!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "No unclaimed rewards right now.\nCheck back tomorrow for VIP daily rewards.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(unclaimed, key = { it.id }) { item ->
                    InboxItemCard(
                        item = item,
                        onClaim = { viewModel.claimItem(item.id) }
                    )
                }
            }

            // Claimed items section header
            if (claimed.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Claimed",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    HorizontalDivider()
                }
                items(claimed.take(20), key = { "claimed_${it.id}" }) { item ->
                    InboxItemCard(item = item, onClaim = null)
                }
            }
        }
    }
}

@Composable
private fun InboxItemCard(
    item: InboxItemEntity,
    onClaim: (() -> Unit)?
) {
    val isClaimed = item.claimed
    val cardAlpha = if (isClaimed) 0.55f else 1f

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isClaimed)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isClaimed) 0.dp else 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on type
            Text(
                typeEmoji(item.type),
                fontSize = 36.sp,
                modifier = Modifier.padding(end = 14.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    item.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha)
                )
                // Reward chips
                val chips = buildRewardChips(item)
                if (chips.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chips.forEach { chip ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TileCorrect.copy(alpha = if (isClaimed) 0.15f else 0.22f)
                            ) {
                                Text(
                                    chip,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 12.sp,
                                    color = TileCorrect.copy(alpha = cardAlpha),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            if (onClaim != null && !isClaimed) {
                Button(
                    onClick = onClaim,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Claim", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else if (isClaimed) {
                Text(
                    "✔ Claimed",
                    color = TileCorrect.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun typeEmoji(type: String): String = when (type) {
    "vip_daily"   -> "👑"
    "seasonal"    -> "🎄"
    "promo"       -> "🎁"
    "admin"       -> "📦"
    else          -> "📬"
}

private fun buildRewardChips(item: InboxItemEntity): List<String> {
    val chips = mutableListOf<String>()
    if (item.coinsGranted > 0) chips += "+${item.coinsGranted} 🪙"
    if (item.diamondsGranted > 0) chips += "+${item.diamondsGranted} 💎"
    if (item.livesGranted > 0) chips += "+${item.livesGranted} ❤️"
    if (item.addGuessItemsGranted > 0) chips += "+${item.addGuessItemsGranted} ➕"
    if (item.removeLetterItemsGranted > 0) chips += "+${item.removeLetterItemsGranted} 🚫"
    if (item.definitionItemsGranted > 0) chips += "+${item.definitionItemsGranted} 📖"
    if (item.showLetterItemsGranted > 0) chips += "+${item.showLetterItemsGranted} 💡"
    return chips
}
