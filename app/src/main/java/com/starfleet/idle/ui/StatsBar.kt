package com.starfleet.idle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starfleet.idle.data.GameState
import com.starfleet.idle.ui.theme.*

@Composable
fun StatsBar(state: GameState, onGetGems: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSpace)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text("⭐ STARFLEET COMMAND", color = CreditGold, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatChip("Credits", formatNumber(state.credits), CreditGold)
            StatChip("Per Sec", formatNumber(state.creditsPerSecond), ShieldGreen)
            StatChip("Fleet Power", formatNumber(state.totalFleetPower), StarBlue)
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (state.starCoins > 0) {
                Text("🪙 ${state.starCoins}", color = CreditGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(" · ", color = TextSecondary, fontSize = 11.sp)
            }
            // Tappable gem display that navigates to Premium tab
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NebulaPurple.copy(alpha = 0.2f))
                    .clickable { onGetGems() }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💎 ${state.gems}", color = NebulaPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(" +", color = ShieldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(" · ", color = TextSecondary, fontSize = 11.sp)
            Text("🧪 ${state.researchPoints} RP", color = StarBlue, fontSize = 11.sp)
            if (state.isAdBoostActive) {
                Text(" · ", color = TextSecondary, fontSize = 11.sp)
                Text("⚡2x", color = ShieldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (state.isSpeedBoostActive) {
                Text(" · ", color = TextSecondary, fontSize = 11.sp)
                Text("⏩2x", color = StarBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (state.isGameComplete) {
            Spacer(Modifier.height(6.dp))
            Text("🎉 GALAXY CONQUERED! YOU WIN! 🎉", color = CreditGold, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        // Ready-to-prestige indicator (shows when gain is meaningful)
        if (state.coinsOnReset >= 5 && state.coinsOnReset >= state.starCoins / 4) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CreditGold.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🪙 Ready to Prestige! +${state.coinsOnReset} Star Coins",
                    color = CreditGold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(CardBackground, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextSecondary, fontSize = 10.sp)
    }
}

fun formatNumber(value: Double): String {
    return when {
        value >= 1e33 -> String.format("%.2fDc", value / 1e33)
        value >= 1e30 -> String.format("%.2fNo", value / 1e30)
        value >= 1e27 -> String.format("%.2fOc", value / 1e27)
        value >= 1e24 -> String.format("%.2fSp", value / 1e24)
        value >= 1e21 -> String.format("%.2fSx", value / 1e21)
        value >= 1e18 -> String.format("%.2fQi", value / 1e18)
        value >= 1e15 -> String.format("%.2fQa", value / 1e15)
        value >= 1e12 -> String.format("%.2fT", value / 1e12)
        value >= 1e9 -> String.format("%.2fB", value / 1e9)
        value >= 1e6 -> String.format("%.2fM", value / 1e6)
        value >= 1e3 -> String.format("%.2fK", value / 1e3)
        value >= 1 -> String.format("%.1f", value)
        else -> String.format("%.2f", value)
    }
}
