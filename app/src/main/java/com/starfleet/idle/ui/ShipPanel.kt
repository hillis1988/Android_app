package com.starfleet.idle.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starfleet.idle.data.*
import com.starfleet.idle.ui.theme.*

@Composable
fun ShipPanel(
    tier: ShipTier,
    shipState: ShipState,
    gameState: GameState,
    onBuyShip: () -> Unit,
    onBuyUpgrade: (String) -> Unit
) {
    val isUnlocked = gameState.isShipUnlocked(tier.id)
    var expanded by remember { mutableStateOf(false) }
    val milestoneMulti = getMilestoneMultiplier(shipState.count)
    val nextMilestone = getNextMilestone(shipState.count)

    val buyCount = getBuyCount(gameState.buyAmount, shipState.count)
    val bulkCost = gameState.getBulkShipCost(tier.id, buyCount)
    val canAfford = isUnlocked && gameState.credits >= gameState.getShipCost(tier.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .alpha(if (isUnlocked) 1f else 0.4f),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isUnlocked) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = tier.emoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tier.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (!isUnlocked) {
                        Text(
                            text = "Requires ${formatNumber(tier.unlockPower)} Fleet Power",
                            color = AlertRed,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "Owned: ${shipState.count} · Income: ${formatNumber(getShipIncome(tier, shipState, gameState))}/s",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        if (milestoneMulti > 1.0) {
                            Text(
                                text = "⚡ ${milestoneMulti.toInt()}x milestone bonus",
                                color = CreditGold,
                                fontSize = 10.sp
                            )
                        }
                        if (nextMilestone != null) {
                            Text(
                                text = "Next 2x at $nextMilestone ships",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (isUnlocked) {
                    Button(
                        onClick = onBuyShip,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) NebulaPurple else DeepSpace
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (buyCount > 1) "Buy $buyCount" else "Buy",
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatNumber(bulkCost),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = expanded && isUnlocked && shipState.count > 0) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = DeepSpace, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    UPGRADE_TYPES.forEach { upgrade ->
                        val level = shipState.upgradeLevels[upgrade.id] ?: 0
                        val cost = gameState.getUpgradeCost(tier.id, upgrade.id)
                        val canAffordUpgrade = gameState.credits >= cost

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = upgrade.emoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${upgrade.name} Lv.$level",
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "+${(upgrade.incomeBoost * 100).toInt()}% income per level",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Button(
                                onClick = { onBuyUpgrade(upgrade.id) },
                                enabled = canAffordUpgrade,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canAffordUpgrade) StarBlue else DeepSpace
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = formatNumber(cost), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getShipIncome(tier: ShipTier, state: ShipState, gameState: GameState): Double {
    if (state.count == 0) return 0.0
    val milestoneMulti = getMilestoneMultiplier(state.count)
    val baseIncome = tier.baseIncome * state.count * milestoneMulti
    val upgradeMultiplier = UPGRADE_TYPES.sumOf { upgrade ->
        val level = state.upgradeLevels[upgrade.id] ?: 0
        level * upgrade.incomeBoost
    }
    return baseIncome * (1.0 + upgradeMultiplier) * gameState.globalIncomeMultiplier
}
