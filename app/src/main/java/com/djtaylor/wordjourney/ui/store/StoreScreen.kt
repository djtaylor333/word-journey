package com.djtaylor.wordjourney.ui.store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.djtaylor.wordjourney.billing.ProductIds
import com.djtaylor.wordjourney.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun StoreScreen(
    onBack: () -> Unit,
    viewModel: StoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val isLight = !isSystemInDarkTheme()
    val coinColor = adaptiveCoinColor(isLight)
    val diamondColor = adaptiveDiamondColor(isLight)

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
                        Text("⬡ ${uiState.progress.coins}", color = coinColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("◆ ${uiState.progress.diamonds}", color = diamondColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("❤️ ${uiState.progress.lives}", color = HeartRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Tabs
                val tabs = listOf("Items", "Bundles", "Lives", "Coins", "Diamonds", "VIP")
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    0 -> ItemsTab(uiState, viewModel)
                    1 -> BundlesTab(uiState, viewModel)
                    2 -> LivesTab(uiState, viewModel)
                    3 -> CoinsTab(uiState, viewModel)
                    4 -> DiamondsTab(uiState, viewModel)
                    5 -> VipTab(uiState, viewModel)
                }
            }
        }
    }
}

@Composable
private fun ItemsTab(uiState: StoreUiState, viewModel: StoreViewModel) {
    val isLt = !isSystemInDarkTheme()
    val cColor = adaptiveCoinColor(isLt)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Power-Ups", style = MaterialTheme.typography.titleLarge, color = Primary)
        Text(
            "Buy items to stock up — use them during gameplay!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        // Current inventory
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Text("📦 Your Inventory", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InventoryChip("➕", "Add Guess", uiState.progress.addGuessItems)
                    InventoryChip("🚫", "Remove Letter", uiState.progress.removeLetterItems)
                    InventoryChip("📖", "Definition", uiState.progress.definitionItems)
                    InventoryChip("💡", "Show Letter", uiState.progress.showLetterItems)
                }
            }
        }

        HorizontalDivider()

        StoreCard(
            emoji = "➕",
            title = "Add a Guess",
            description = "Get one extra guess row during a level",
            costLabel = "200 coins",
            costColor = cColor,
            ownedCount = uiState.progress.addGuessItems,
            enabled = uiState.progress.coins >= 200,
            onBuy = { viewModel.buyAddGuessItem() }
        )
        StoreCard(
            emoji = "🚫",
            title = "Remove a Letter",
            description = "Eliminate one letter guaranteed not in the word",
            costLabel = "150 coins",
            costColor = cColor,
            ownedCount = uiState.progress.removeLetterItems,
            enabled = uiState.progress.coins >= 150,
            onBuy = { viewModel.buyRemoveLetterItem() }
        )
        StoreCard(
            emoji = "📖",
            title = "Definition Hint",
            description = "Reveal the word's definition as a clue (once per level)",
            costLabel = "300 coins",
            costColor = cColor,
            ownedCount = uiState.progress.definitionItems,
            enabled = uiState.progress.coins >= 300,
            onBuy = { viewModel.buyDefinitionItem() }
        )
        StoreCard(
            emoji = "💡",
            title = "Show Letter",
            description = "Reveal one correct letter position in the word",
            costLabel = "250 coins",
            costColor = cColor,
            ownedCount = uiState.progress.showLetterItems,
            enabled = uiState.progress.coins >= 250,
            onBuy = { viewModel.buyShowLetterItem() }
        )

        HorizontalDivider()

        // ── Ad Rewards ──────────────────────────────────────────────────
        Text("Free Rewards", style = MaterialTheme.typography.titleLarge, color = Primary)
        Text(
            "Watch a short video ad to earn free rewards!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        StoreCard(
            emoji = "🎬",
            title = "Watch Ad → 100 Coins",
            description = "Watch a short video to earn 100 free coins",
            costLabel = if (uiState.isAdReady) "Watch" else "Loading…",
            costColor = AccentEasy,
            enabled = uiState.isAdReady && !uiState.isWatchingAd,
            onBuy = { viewModel.watchAdForCoins() }
        )
        StoreCard(
            emoji = "🎬",
            title = "Watch Ad → 1 Life",
            description = "Watch a short video to earn 1 free life",
            costLabel = if (uiState.isAdReady) "Watch" else "Loading…",
            costColor = HeartRed,
            enabled = uiState.isAdReady && !uiState.isWatchingAd,
            onBuy = { viewModel.watchAdForLife() }
        )
        StoreCard(
            emoji = "🎬",
            title = "Watch Ad → Random Item",
            description = "Watch a short video to earn 1 random power-up",
            costLabel = if (uiState.isAdReady) "Watch" else "Loading…",
            costColor = Primary,
            enabled = uiState.isAdReady && !uiState.isWatchingAd,
            onBuy = { viewModel.watchAdForItem() }
        )
    }
}

@Composable
private fun InventoryChip(emoji: String, label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Text("$count", fontWeight = FontWeight.Bold, fontSize = 18.sp,
            color = if (count > 0) Primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun LivesTab(uiState: StoreUiState, viewModel: StoreViewModel) {
    val isLt = !isSystemInDarkTheme()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lives", style = MaterialTheme.typography.titleLarge, color = HeartRed)
        Text(
            "Current: ❤️ ${uiState.progress.lives} lives",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )

        StoreCard(
            emoji = "⬡→❤️",
            title = "Trade Coins for 1 Life",
            description = "Spend 1,000 coins to gain 1 extra life",
            costLabel = "1000 coins",
            costColor = adaptiveCoinColor(isLt),
            enabled = uiState.progress.coins >= 1000,
            onBuy = { viewModel.tradeCoinsForLife() }
        )
        StoreCard(
            emoji = "◆→❤️",
            title = "Trade Diamonds for 1 Life",
            description = "Spend 3 diamonds to gain 1 extra life",
            costLabel = "3 diamonds",
            costColor = adaptiveDiamondColor(isLt),
            enabled = uiState.progress.diamonds >= 3,
            onBuy = { viewModel.tradeDiamondsForLife() }
        )

        HorizontalDivider()
        Text("Purchase Packs", style = MaterialTheme.typography.titleMedium)

        StoreCard(
            emoji = "❤️",
            title = "5 Lives Pack",
            description = "Instantly receive 5 extra lives",
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
        val isLt = !isSystemInDarkTheme()
        Text("Coins", style = MaterialTheme.typography.titleLarge, color = adaptiveCoinColor(isLt))
        Text("Current: ⬡ ${uiState.progress.coins} coins", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
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
        val isLt = !isSystemInDarkTheme()
        Text("Diamonds", style = MaterialTheme.typography.titleLarge, color = adaptiveDiamondColor(isLt))
        Text("Current: ◆ ${uiState.progress.diamonds} diamonds", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text("Premium currency — used for lives and special items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        IapCard("◆", "10 Diamonds", "Starter pack", ProductIds.DIAMONDS_10, viewModel)
        IapCard("◆◆", "50 Diamonds", "Popular pack", ProductIds.DIAMONDS_50, viewModel)
        IapCard("◆◆◆", "200 Diamonds", "Best value", ProductIds.DIAMONDS_200, viewModel)
    }
}

@Composable
private fun BundlesTab(uiState: StoreUiState, viewModel: StoreViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Value Bundles", style = MaterialTheme.typography.titleLarge, color = Primary)
        Text(
            "Save big with curated bundles!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        // Starter
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎒", fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Starter Bundle", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("Perfect for getting started",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = { viewModel.purchase(ProductIds.STARTER_BUNDLE) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEasy)
                    ) {
                        Text(viewModel.getPriceLabel(ProductIds.STARTER_BUNDLE),
                            fontWeight = FontWeight.Bold, color = OnPrimary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("⬡ 1,000 coins • ◆ 5 diamonds • 5× each item",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }

        // Adventurer
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, AccentRegular.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚔️", fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Adventurer Bundle", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = AccentRegular.copy(alpha = 0.2f)) {
                                Text("Popular", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentRegular)
                            }
                        }
                        Text("Best for regular players",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = { viewModel.purchase(ProductIds.ADVENTURER_BUNDLE) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRegular)
                    ) {
                        Text(viewModel.getPriceLabel(ProductIds.ADVENTURER_BUNDLE),
                            fontWeight = FontWeight.Bold, color = OnPrimary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("⬡ 3,000 coins • ◆ 20 diamonds • ❤️ 10 lives • 10× each item",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }

        // Champion
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, Primary.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆", fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Champion Bundle", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(alpha = 0.2f)) {
                                Text("Best Value", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                        Text("Ultimate power-up package",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = { viewModel.purchase(ProductIds.CHAMPION_BUNDLE) },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(viewModel.getPriceLabel(ProductIds.CHAMPION_BUNDLE),
                            fontWeight = FontWeight.Bold, color = OnPrimary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("⬡ 10,000 coins • ◆ 100 diamonds • ❤️ 25 lives • 25× each item",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun VipTab(uiState: StoreUiState, viewModel: StoreViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("VIP Pass", style = MaterialTheme.typography.titleLarge, color = Primary)

        if (uiState.progress.isVip) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Primary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(2.dp, Primary)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👑", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("You're a VIP!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Primary)
                    Text("Your subscription is active", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        } else {
            Text(
                "Unlock premium perks with a VIP subscription!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("VIP Benefits:", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                val benefits = listOf(
                    "👑 No ads — ever",
                    "❤️ +5 bonus lives per month",
                    "⬡ 500 bonus coins per month",
                    "⭐ 2× star rewards on all levels",
                    "🎁 Exclusive seasonal themes",
                    "📊 Advanced statistics"
                )
                benefits.forEach { benefit ->
                    Text(
                        benefit,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        if (!uiState.progress.isVip) {
            StoreCard(
                emoji = "👑",
                title = "VIP Monthly",
                description = "All VIP perks, billed monthly",
                costLabel = viewModel.getPriceLabel(ProductIds.VIP_MONTHLY),
                costColor = Primary,
                enabled = true,
                onBuy = { viewModel.purchase(ProductIds.VIP_MONTHLY) }
            )
            StoreCard(
                emoji = "👑",
                title = "VIP Yearly",
                description = "Save 33% — best VIP value",
                costLabel = viewModel.getPriceLabel(ProductIds.VIP_YEARLY),
                costColor = AccentEasy,
                enabled = true,
                onBuy = { viewModel.purchase(ProductIds.VIP_YEARLY) }
            )
        }
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
    costColor: Color,
    enabled: Boolean,
    ownedCount: Int? = null,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (ownedCount != null) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (ownedCount > 0) Primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "×$ownedCount",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ownedCount > 0) Primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
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
