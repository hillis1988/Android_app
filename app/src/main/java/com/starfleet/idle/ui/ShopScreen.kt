package com.starfleet.idle.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starfleet.idle.data.GameState
import com.starfleet.idle.data.SHOP_BONUSES
import com.starfleet.idle.data.ShopBonus
import com.starfleet.idle.ui.theme.*
import kotlinx.coroutines.delay

data class TapParticle(val id: Long, val text: String, val creationTime: Long)

@Composable
fun ShopScreen(
    gameState: GameState,
    onBuyBonus: (String) -> Unit,
    onTap: () -> Unit
) {
    var particles by remember { mutableStateOf(listOf<TapParticle>()) }

    // Cleanup particles
    LaunchedEffect(particles) {
        if (particles.isNotEmpty()) {
            delay(1000)
            particles = particles.filter { System.currentTimeMillis() - it.creationTime < 1000 }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SpaceBlack),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (gameState.tapCredits > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎯 TAP TO EARN", color = CreditGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(contentAlignment = Alignment.Center) {
                            Button(
                                onClick = {
                                    onTap()
                                    particles = particles + TapParticle(
                                        id = System.nanoTime(),
                                        text = "+${formatNumber(gameState.tapCredits)}",
                                        creationTime = System.currentTimeMillis()
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "+${formatNumber(gameState.tapCredits)} credits", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            // Render particles
                            particles.forEach { particle ->
                                ParticleEffect(particle)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "🏪 ${gameState.activeSector.name} SHOP",
                color = CreditGold, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(SHOP_BONUSES) { bonus ->
            ShopBonusCard(bonus = bonus, gameState = gameState, onBuy = { onBuyBonus(bonus.id) })
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ParticleEffect(particle: TapParticle) {
    val alpha = remember { Animatable(1f) }
    val yOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(0f, animationSpec = tween(1000))
    }
    LaunchedEffect(Unit) {
        yOffset.animateTo(-100f, animationSpec = tween(1000, easing = LinearOutSlowInEasing))
    }

    Text(
        text = particle.text,
        color = CreditGold,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier
            .graphicsLayer(
                alpha = alpha.value,
                translationY = yOffset.value
            )
    )
}

@Composable
private fun ShopBonusCard(bonus: ShopBonus, gameState: GameState, onBuy: () -> Unit) {
    val level = gameState.activeShopLevels[bonus.id] ?: 0
    val isMaxed = gameState.isShopBonusMaxed(bonus.id)
    val canAfford = gameState.canAffordShopBonus(bonus.id)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = bonus.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = bonus.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = bonus.description, color = TextSecondary, fontSize = 11.sp)
                Text(text = "Level $level / ${bonus.maxLevel}", color = if (isMaxed) ShieldGreen else StarBlue, fontSize = 11.sp)
            }
            if (isMaxed) {
                Text(text = "MAX", color = ShieldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
            } else {
                Button(
                    onClick = onBuy, enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) NebulaPurple else DeepSpace),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) { Text(text = formatNumber(gameState.getShopBonusCost(bonus.id)), fontSize = 12.sp) }
            }
        }
    }
}
