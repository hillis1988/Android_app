package com.starfleet.idle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starfleet.idle.data.GameState
import com.starfleet.idle.ui.theme.*

@Composable
fun StatsBar(state: GameState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSpace)
            .padding(16.dp)
    ) {
        Text(
            text = "⭐ STARFLEET COMMAND",
            color = CreditGold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatChip(label = "Credits", value = formatNumber(state.credits), color = CreditGold)
            StatChip(label = "Per Sec", value = formatNumber(state.creditsPerSecond), color = ShieldGreen)
            StatChip(label = "Fleet Power", value = formatNumber(state.fleetPower), color = StarBlue)
        }

        if (state.isGameComplete) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "🎉 GALAXY CONQUERED! YOU WIN! 🎉",
                color = CreditGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(CardBackground, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
    }
}

fun formatNumber(value: Double): String {
    return when {
        value >= 1_000_000_000_000 -> String.format("%.2fT", value / 1_000_000_000_000)
        value >= 1_000_000_000 -> String.format("%.2fB", value / 1_000_000_000)
        value >= 1_000_000 -> String.format("%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format("%.2fK", value / 1_000)
        value >= 1 -> String.format("%.1f", value)
        else -> String.format("%.2f", value)
    }
}
