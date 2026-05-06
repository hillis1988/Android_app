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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starfleet.idle.data.*
import com.starfleet.idle.ui.theme.*

@Composable
fun ResearchScreen(gameState: GameState, onBuyResearch: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SpaceBlack),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔬 RESEARCH LAB", color = CreditGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🧪 ${gameState.researchPoints} Research Points",
                        color = StarBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+${String.format("%.2f", gameState.researchPointGenRate * 3600)} RP/hour",
                        color = TextSecondary, fontSize = 11.sp
                    )
                }
            }
        }

        ResearchBranch.entries.forEach { branch ->
            val nodes = RESEARCH_NODES.filter { it.branch == branch }
            item {
                Text(
                    text = "${branch.emoji} ${branch.label}",
                    color = CreditGold, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(nodes) { node ->
                ResearchNodeCard(node = node, gameState = gameState, onBuy = { onBuyResearch(node.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ResearchNodeCard(node: ResearchNode, gameState: GameState, onBuy: () -> Unit) {
    val level = gameState.researchLevels[node.id] ?: 0
    val isMaxed = level >= node.maxLevel
    val cost = gameState.getResearchCost(node.id)
    val canAfford = !isMaxed && gameState.researchPoints >= cost

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = node.emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = node.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = node.description, color = TextSecondary, fontSize = 10.sp)
                Text(text = "Level $level / ${node.maxLevel}", color = if (isMaxed) ShieldGreen else StarBlue, fontSize = 10.sp)
            }
            if (isMaxed) {
                Text(text = "MAX", color = ShieldGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
            } else {
                Button(
                    onClick = onBuy, enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) StarBlue else DeepSpace),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp)
                ) { Text(text = "$cost RP", fontSize = 11.sp) }
            }
        }
    }
}
