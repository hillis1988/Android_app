package com.starfleet.idle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.starfleet.idle.data.*
import com.starfleet.idle.engine.MissionEngine
import com.starfleet.idle.ui.theme.*

@Composable
fun MissionDetailDialog(
    mission: MissionTemplate,
    gameState: GameState,
    onDeploy: (MissionTemplate, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTiers by remember { mutableStateOf<List<String>>(emptyList()) }

    // Calculate success chance based on selected ships
    val effectivePower = if (selectedTiers.isNotEmpty()) {
        MissionEngine.calculateEffectiveFleetPower(
            deployedShipTiers = selectedTiers,
            shipStates = gameState.activeSectorState.ships,
            missionType = mission.type,
            gameState = gameState
        )
    } else 0.0

    val successChance = if (selectedTiers.isNotEmpty()) {
        MissionEngine.calculateSuccessChance(effectivePower, mission.requiredFleetPower)
    } else 0.0

    // Validate deployment
    val validation = if (selectedTiers.isNotEmpty()) {
        MissionEngine.validateDeployment(mission, selectedTiers, gameState)
    } else {
        DeploymentValidation(false, "Select at least 1 ship type")
    }

    // Get available ship tiers (player owns at least 1, not already deployed)
    val deployedTiers = gameState.activeMissions
        .filter { it.status == MissionStatus.IN_PROGRESS }
        .flatMap { it.deployedShipTiers }
        .toSet()

    val availableShipTiers = SHIP_TIERS.filter { tier ->
        val totalOwned = gameState.sectors.values.sumOf { sectorState ->
            sectorState.ships[tier.id]?.count ?: 0
        }
        totalOwned > 0 && tier.id !in deployedTiers
    }

    Dialog(onDismissRequest = onDismiss) {
        StarFleetIdleTheme {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    // Mission header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = mission.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mission.name,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mission.description,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Mission details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("⚡ Required Power", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                formatFleetPower(mission.requiredFleetPower),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("⏱ Duration", color = TextSecondary, fontSize = 11.sp)
                            val minutes = mission.durationMs / (60 * 1000)
                            val durationText = if (minutes < 60) "${minutes}m" else "${minutes / 60}h"
                            Text(
                                durationText,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Rewards preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("💰 Credits + 💎 ${mission.difficulty.gemReward} gems", color = CreditGold, fontSize = 12.sp)
                        if (mission.difficulty.researchPointReward.last > 0) {
                            Text(
                                "🔬 ${mission.difficulty.researchPointReward.first}-${mission.difficulty.researchPointReward.last} RP",
                                color = StarBlue,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Divider(color = DeepSpace, thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    // Ship selection
                    Text(
                        "Select Ships (1-5 types)",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    // Ship list (scrollable)
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(availableShipTiers) { tier ->
                            val isSelected = tier.id in selectedTiers
                            val affinity = SHIP_AFFINITIES.find { it.shipTierId == tier.id }
                            val affinityValue = when (mission.type) {
                                MissionType.COMBAT -> affinity?.combat ?: 1.0
                                MissionType.MINING -> affinity?.mining ?: 1.0
                                MissionType.EXPLORATION -> affinity?.exploration ?: 1.0
                                MissionType.DIPLOMACY -> affinity?.diplomacy ?: 1.0
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NebulaPurple.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        selectedTiers = if (isSelected) {
                                            selectedTiers - tier.id
                                        } else if (selectedTiers.size < 5) {
                                            selectedTiers + tier.id
                                        } else {
                                            selectedTiers
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedTiers = if (checked && selectedTiers.size < 5) {
                                            selectedTiers + tier.id
                                        } else {
                                            selectedTiers - tier.id
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = NebulaPurple,
                                        uncheckedColor = TextSecondary
                                    )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${tier.emoji} ${tier.name}",
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                                // Affinity indicator
                                val affinityColor = when {
                                    affinityValue >= 1.5 -> ShieldGreen
                                    affinityValue >= 1.0 -> CreditGold
                                    else -> AlertRed
                                }
                                Text(
                                    "${affinityValue}x",
                                    color = affinityColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Success chance display
                    if (selectedTiers.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Success Chance:", color = TextSecondary, fontSize = 13.sp)
                            val chanceColor = when {
                                successChance >= 0.80 -> ShieldGreen
                                successChance >= 0.50 -> CreditGold
                                else -> AlertRed
                            }
                            Text(
                                "${(successChance * 100).toInt()}%",
                                color = chanceColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Validation error
                    if (!validation.isValid && selectedTiers.isNotEmpty()) {
                        Text(
                            validation.errorMessage ?: "",
                            color = AlertRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Button(
                            onClick = { onDeploy(mission, selectedTiers) },
                            enabled = validation.isValid,
                            colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Deploy!")
                        }
                    }
                }
            }
        }
    }
}
