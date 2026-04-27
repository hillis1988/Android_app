package com.starfleet.idle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.starfleet.idle.data.BuyAmount
import com.starfleet.idle.data.SHIP_TIERS
import com.starfleet.idle.data.ShipState
import com.starfleet.idle.data.calculatePrestigeCoins
import com.starfleet.idle.data.getPrestigeMultiplier
import com.starfleet.idle.ui.theme.*

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsState()
    val offlineEarnings by viewModel.offlineEarnings.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPrestigeDialog by remember { mutableStateOf(false) }
    var showHardResetDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatsBar(state = state)

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepSpace,
                contentColor = CreditGold
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("🚀 Fleet", fontSize = 14.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🏪 Shop", fontSize = 14.sp) }
                )
            }

            when (selectedTab) {
                0 -> FleetTab(
                    viewModel = viewModel,
                    state = state,
                    onPrestige = { showPrestigeDialog = true },
                    onHardReset = { showHardResetDialog = true }
                )
                1 -> ShopScreen(
                    gameState = state,
                    onBuyBonus = { viewModel.buyShopBonus(it) },
                    onTap = { viewModel.tap() }
                )
            }
        }

        // Offline earnings dialog
        offlineEarnings?.let { earned ->
            Dialog(onDismissRequest = { viewModel.dismissOfflineEarnings() }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🌙 Welcome Back, Commander!",
                            color = CreditGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your fleet earned while you were away:",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "+${formatNumber(earned)} credits",
                            color = ShieldGreen,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.dismissOfflineEarnings() },
                            colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)
                        ) {
                            Text("Collect")
                        }
                    }
                }
            }
        }

        // Prestige confirmation dialog
        if (showPrestigeDialog) {
            val coinsToEarn = state.coinsOnReset
            val newTotal = state.starCoins + coinsToEarn
            val newMultiplier = getPrestigeMultiplier(newTotal)

            Dialog(onDismissRequest = { showPrestigeDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🪙 PRESTIGE RESET",
                            color = CreditGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (coinsToEarn > 0) {
                            Text(
                                text = "Reset your fleet and earn:",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "+$coinsToEarn Star Coins",
                                color = CreditGold,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total: $newTotal coins · ${String.format("%.1f", newMultiplier)}x income",
                                color = ShieldGreen,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All ships, upgrades, and shop bonuses will be reset.\nStar Coins are permanent.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showPrestigeDialog = false }
                                ) {
                                    Text("Cancel", color = TextSecondary)
                                }
                                Button(
                                    onClick = {
                                        viewModel.prestige()
                                        showPrestigeDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)
                                ) {
                                    Text("Prestige!")
                                }
                            }
                        } else {
                            Text(
                                text = "You need more Fleet Power to earn Star Coins.",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Reach 1,000+ Fleet Power to earn your first coin.",
                                color = AlertRed,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showPrestigeDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)
                            ) {
                                Text("Got it")
                            }
                        }
                    }
                }
            }
        }

        // Hard reset confirmation dialog
        if (showHardResetDialog) {
            Dialog(onDismissRequest = { showHardResetDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ HARD RESET",
                            color = AlertRed,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "This will erase EVERYTHING including Star Coins. This cannot be undone.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showHardResetDialog = false }
                            ) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Button(
                                onClick = {
                                    viewModel.hardReset()
                                    showHardResetDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                            ) {
                                Text("Erase All")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FleetTab(
    viewModel: GameViewModel,
    state: com.starfleet.idle.data.GameState,
    onPrestige: () -> Unit,
    onHardReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BuyAmountSelector(
            selected = state.buyAmount,
            onSelect = { viewModel.setBuyAmount(it) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(SHIP_TIERS) { tier ->
                val shipState = state.ships[tier.id] ?: ShipState()
                ShipPanel(
                    tier = tier,
                    shipState = shipState,
                    gameState = state,
                    onBuyShip = { viewModel.buyShip(tier.id) },
                    onBuyUpgrade = { upgradeId -> viewModel.buyUpgrade(tier.id, upgradeId) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Prestige button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🪙 PRESTIGE",
                            color = CreditGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reset for +${state.coinsOnReset} Star Coins (each coin = +5% income forever)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onPrestige,
                            colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Prestige Reset")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onHardReset) {
                        Text("Hard Reset (erase all)", color = AlertRed, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun BuyAmountSelector(
    selected: BuyAmount,
    onSelect: (BuyAmount) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSpace)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Buy:",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 4.dp)
        )
        BuyAmount.entries.forEach { amount ->
            val isSelected = amount == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(amount) },
                label = {
                    Text(
                        text = amount.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NebulaPurple,
                    selectedLabelColor = TextPrimary,
                    containerColor = CardBackground,
                    labelColor = TextSecondary
                ),
                modifier = Modifier
                    .height(30.dp)
                    .padding(horizontal = 2.dp)
            )
        }
    }
}
