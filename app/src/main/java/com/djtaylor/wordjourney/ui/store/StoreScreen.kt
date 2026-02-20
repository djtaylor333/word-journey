package com.djtaylor.wordjourney.ui.store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.billing.ProductIds
import com.djtaylor.wordjourney.ui.theme.*

@Composable
fun StoreScreen(
    onBack: () -> Unit,
    viewModel: StoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Spacer(Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(
                        "Store",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Primary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    // Currency display
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 12.dp)) {
                        Text("⬡ ${uiState.progress.coins}", color = CoinGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("◆ ${uiState.progress.diamonds}", color = DiamondCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("❤️ ${uiState.progress.lives}", color = HeartRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Tabs
                val tabs = listOf("Items", "Lives", "Coins", "Diamonds")
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Primary,
                    edgePadding = 8.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isPurchasing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            when (selectedTab) {
                0 -> ItemsTab(uiState, viewModel)
                1 -> LivesTab(uiState, viewModel)
                2 -> CoinsTab(uiState, viewModel)
                3 -> DiamondsTab(uiState, viewModel)
            }
        }
    }
}

@Composable
private fun ItemsTab(uiState: StoreUiState, viewModel: StoreViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Power-Ups", style = MaterialTheme.typography.titleLarge, color = Primary)
        Text("Use coins to buy in-game power-ups", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

        StoreCard(
            emoji = "➕",
            title = "Add a Guess",
            description = "Get one extra guess row for the current word without using a life",
            costLabel = "200 coins",
            costColor = CoinGold,
            enabled = uiState.progress.coins >= 200,
            onBuy = { /* items are bought in-game from the game screen */ }
        )
        StoreCard(
            emoji = "🚫",
            title = "Remove a Letter",
            description = "Eliminate one random letter guaranteed not in the current word",
            costLabel = "150 coins",
            costColor = CoinGold,
            enabled = uiState.progress.coins >= 150,
            onBuy = { /* items are bought in-game from the game screen */ }
        )
        StoreCard(
            emoji = "📖",
            title = "Definition",
            description = "Reveal the definition of the target word as a hint (once per level)",
            costLabel = "300 coins",
            costColor = CoinGold,
            enabled = uiState.progress.coins >= 300,
            onBuy = { /* items are bought in-game from the game screen */ }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "💡 Items are purchased directly from the game screen for immediate use.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun LivesTab(uiState: StoreUiState, viewModel: StoreViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lives", style = MaterialTheme.typography.titleLarge, color = HeartRed)
        StoreCard(
            emoji = "⬡",
            title = "Trade Coins for 1 Life",
            description = "Spend 1,000 coins to gain 1 extra life immediately",
            costLabel = "1000 coins",
            costColor = CoinGold,
            enabled = uiState.progress.coins >= 1000,
            onBuy = { viewModel.tradeCoinsForLife() }
        )
        StoreCard(
            emoji = "◆",
            title = "Trade Diamonds for 1 Life",
            description = "Spend 3 diamonds to gain 1 extra life immediately",
            costLabel = "3 diamonds",
            costColor = DiamondCyan,
            enabled = uiState.progress.diamonds >= 3,
            onBuy = { /* handled by StoreViewModel */ }
        )

        HorizontalDivider()
        Text("Purchase Packs", style = MaterialTheme.typography.titleMedium)

        StoreCard(
            emoji = "❤️",
            title = "5 Lives Pack",
            description = "4.99 — Instantly receive 5 extra lives",
            costLabel = viewModel.getPriceLabel(ProductIds.LIVES_PACK_5),
            costColor = HeartRed,
            enabled = true,
            onBuy = { viewModel.purchase(ProductIds.LIVES_PACK_5) }
        )
    }
}

@Composable
private fun CoinsTab(uiState: StoreUiState, viewModel: StoreViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Coins", style = MaterialTheme.typography.titleLarge, color = CoinGold)
        IapCard("⬡", "500 Coins", "Starter pack", ProductIds.COINS_500, viewModel)
        IapCard("⬡⬡", "1500 Coins", "Popular pack", ProductIds.COINS_1500, viewModel)
        IapCard("⬡⬡⬡", "5000 Coins", "Best value — save 30%", ProductIds.COINS_5000, viewModel)
    }
}

@Composable
private fun DiamondsTab(uiState: StoreUiState, viewModel: StoreViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Diamonds", style = MaterialTheme.typography.titleLarge, color = DiamondCyan)
        Text("Premium currency — used for lives, power-ups and special packs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        IapCard("◆", "10 Diamonds", "Starter pack", ProductIds.DIAMONDS_10, viewModel)
        IapCard("◆◆", "50 Diamonds", "Popular pack", ProductIds.DIAMONDS_50, viewModel)
        IapCard("◆◆◆", "200 Diamonds", "Best value", ProductIds.DIAMONDS_200, viewModel)
    }
}

@Composable
private fun IapCard(emoji: String, title: String, desc: String, productId: String, viewModel: StoreViewModel) {
    StoreCard(
        emoji = emoji,
        title = title,
        description = desc,
        costLabel = viewModel.getPriceLabel(productId),
        costColor = Primary,
        enabled = true,
        onBuy = { viewModel.purchase(productId) }
    )
}

@Composable
private fun StoreCard(
    emoji: String,
    title: String,
    description: String,
    costLabel: String,
    costColor: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onBuy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(emoji, fontSize = 32.sp)
                Column {
                    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            Button(
                onClick = onBuy,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = costColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(costLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnPrimary)
            }
        }
    }
}
