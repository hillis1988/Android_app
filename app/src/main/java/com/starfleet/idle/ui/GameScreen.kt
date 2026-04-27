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
import com.starfleet.idle.data.SHIP_TIERS
import com.starfleet.idle.data.ShipState
import com.starfleet.idle.ui.theme.*

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsState()
    val offlineEarnings by viewModel.offlineEarnings.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatsBar(state = state)

            // Tab row
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
                0 -> FleetTab(viewModel = viewModel, state = state)
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
    }
}

@Composable
private fun FleetTab(viewModel: GameViewModel, state: com.starfleet.idle.data.GameState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
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
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { viewModel.resetGame() }) {
                    Text("Reset Game", color = AlertRed, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
