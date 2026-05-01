package com.starfleet.idle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
fun PerkScreen(
    state: GameState,
    onBuyPerk: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SpaceBlack),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = NebulaPurple.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✨ STAR COIN PERKS", color = CreditGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Permanent bonuses that persist through Prestige.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Your Balance: ${state.starCoins} 🪙", color = CreditGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(STAR_COIN_PERKS) { perk ->
            PerkCard(perk, state, onBuyPerk)
        }
    }
}

@Composable
private fun PerkCard(perk: Perk, state: GameState, onBuyPerk: (String) -> Unit) {
    val level = state.perkLevels[perk.id] ?: 0
    val isMaxed = level >= perk.maxLevel
    val canAfford = state.starCoins >= perk.cost

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(perk.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(perk.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(perk.description, color = TextSecondary, fontSize = 12.sp)
                if (isMaxed) {
                    Text("UNLOCKED", color = ShieldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Cost: ${perk.cost} 🪙", color = CreditGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (!isMaxed) {
                Button(
                    onClick = { onBuyPerk(perk.id) },
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)
                ) {
                    Text("Buy")
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Owned",
                    tint = ShieldGreen
                )
            }
        }
    }
}
