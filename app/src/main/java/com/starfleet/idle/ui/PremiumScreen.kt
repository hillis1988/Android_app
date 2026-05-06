package com.starfleet.idle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starfleet.idle.data.*
import com.starfleet.idle.ui.theme.*

@Composable
fun PremiumScreen(
    gameState: GameState,
    onWatchAd: () -> Unit,
    onWatchSpeedAd: () -> Unit,
    onBuyGemItem: (String) -> Unit,
    onPurchaseGemPack: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SpaceBlack),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Gem balance
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "💎 ${gameState.gems} Gems", color = NebulaPurple, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (gameState.gemBonusIncome > 0) {
                        Text(
                            text = "Permanent income bonus: +${String.format("%.0f", gameState.gemBonusIncome * 100)}%",
                            color = ShieldGreen, fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Ad boost sections
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                // Income Boost
                Card(
                    modifier = Modifier.weight(1f).padding(end = 4.dp, bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "💰 INCOME BOOST", color = CreditGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        if (gameState.isAdBoostActive) {
                            val remainingMin = gameState.adBoostRemainingMs / 60_000
                            Text("⚡ 2x ACTIVE", color = ShieldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${remainingMin / 60}h ${remainingMin % 60}m left", color = TextSecondary, fontSize = 9.sp)
                        } else {
                            Text("Watch ad for 1h", color = TextSecondary, fontSize = 10.sp)
                            Text("2x Income", color = TextSecondary, fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onWatchAd,
                            colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Watch Ad", fontSize = 11.sp)
                        }
                    }
                }

                // Speed Boost
                Card(
                    modifier = Modifier.weight(1f).padding(start = 4.dp, bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "⏩ SPEED BOOST", color = StarBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        if (gameState.isSpeedBoostActive) {
                            val remainingMin = gameState.speedBoostRemainingMs / 60_000
                            Text("⚡ 2x SPEED", color = ShieldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${remainingMin / 60}h ${remainingMin % 60}m left", color = TextSecondary, fontSize = 9.sp)
                        } else {
                            Text("Watch ad for 1h", color = TextSecondary, fontSize = 10.sp)
                            Text("2x Speed", color = TextSecondary, fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onWatchSpeedAd,
                            colors = ButtonDefaults.buttonColors(containerColor = StarBlue),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Watch Ad", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Gem store
        item {
            Text(
                text = "💎 GEM STORE",
                color = CreditGold, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(GEM_STORE_ITEMS) { item ->
            GemStoreCard(item = item, gameState = gameState, onBuy = { onBuyGemItem(item.id) })
        }

        // Gem IAP Packs
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "💰 BUY GEMS",
                color = CreditGold, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (DEV_MODE_FREE_GEMS) {
                Text(
                    text = "🛠️ Dev mode: all packs are free",
                    color = ShieldGreen, fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        items(GEM_PACKS) { pack ->
            GemPackCard(pack = pack, onBuy = { onPurchaseGemPack(pack.id) })
        }

        // Achievements summary
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "🏆 ACHIEVEMENTS (${gameState.unlockedAchievements.size}/${ACHIEVEMENTS.size})",
                color = CreditGold, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(ACHIEVEMENTS) { achievement ->
            val unlocked = achievement.id in gameState.unlockedAchievements
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (unlocked) CardBackground else DeepSpace
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (unlocked) achievement.emoji else "🔒", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = achievement.name,
                            color = if (unlocked) TextPrimary else TextSecondary,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                        Text(text = achievement.description, color = TextSecondary, fontSize = 10.sp)
                    }
                    Text(
                        text = if (unlocked) "✅" else "+${achievement.gemReward}💎",
                        color = if (unlocked) ShieldGreen else NebulaPurple,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun GemStoreCard(item: GemStoreItem, gameState: GameState, onBuy: () -> Unit) {
    val canAfford = gameState.gems >= item.gemCost

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = item.emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = item.description, color = TextSecondary, fontSize = 10.sp)
            }
            Button(
                onClick = onBuy, enabled = canAfford,
                colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) NebulaPurple else DeepSpace),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp)
            ) { Text(text = "${item.gemCost} 💎", fontSize = 11.sp) }
        }
    }
}

@Composable
private fun GemPackCard(pack: GemPack, onBuy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (pack.isBestValue) NebulaPurple.copy(alpha = 0.15f) else CardBackground
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = pack.emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = pack.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (pack.isBestValue) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "BEST VALUE", color = CreditGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val totalGems = pack.gems + pack.bonusGems
                val bonusText = if (pack.bonusGems > 0) " (+${pack.bonusGems} bonus)" else ""
                Text(text = "$totalGems gems$bonusText", color = TextSecondary, fontSize = 11.sp)
            }
            Button(
                onClick = onBuy,
                colors = ButtonDefaults.buttonColors(containerColor = ShieldGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (DEV_MODE_FREE_GEMS) "FREE" else pack.priceDisplay,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
