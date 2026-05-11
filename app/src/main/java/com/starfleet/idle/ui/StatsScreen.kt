package com.starfleet.idle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun StatsScreen(gameState: GameState) {
    val totalShips = gameState.sectors.values.sumOf { sector ->
        sector.ships.values.sumOf { it.count }
    }

    val highestEarner = findHighestEarningShip(gameState)
    val playTimeDays = (System.currentTimeMillis() - gameState.gameStartTime) / 86_400_000.0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SpaceBlack),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Text(
                text = "📊 STATISTICS",
                color = CreditGold, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // Income stats
        item { SectionHeader("💰 Income") }
        item { StatRow("Credits per second", formatNumber(gameState.creditsPerSecond)) }
        item { StatRow("Total credits earned", formatNumber(gameState.totalCreditsEarned)) }
        item { StatRow("Current credits", formatNumber(gameState.credits)) }
        item { StatRow("Income multiplier (prestige)", "%.1fx".format(gameState.prestigeMultiplier)) }
        item { StatRow("Income multiplier (shop)", "%.2fx".format(gameState.globalIncomeMultiplier)) }
        if (gameState.isAdBoostActive) {
            item { StatRow("Ad boost", "2x ACTIVE") }
        }

        // Fleet stats
        item { SectionHeader("🚀 Fleet") }
        item { StatRow("Total fleet power", formatNumber(gameState.totalFleetPower)) }
        item { StatRow("Active sector power", formatNumber(gameState.fleetPower)) }
        item { StatRow("Total ships owned", "$totalShips") }
        item { StatRow("Active sector", gameState.activeSector.name) }
        item { StatRow("Sectors unlocked", "${gameState.unlockedSectorCount} / ${SECTORS.size}") }
        if (highestEarner != null) {
            item { StatRow("Highest earner", "${highestEarner.first.emoji} ${highestEarner.first.name}") }
            item { StatRow("  └ earning", "${formatNumber(highestEarner.second)}/s") }
        }

        // Tap stats
        item { SectionHeader("🎯 Tapping") }
        item { StatRow("Total taps", "${gameState.totalTaps}") }
        item { StatRow("Credits from taps", formatNumber(gameState.totalCreditsFromTaps)) }
        item { StatRow("Credits per tap", formatNumber(gameState.tapCredits)) }

        // Prestige stats
        item { SectionHeader("🪙 Prestige") }
        item { StatRow("Star Coins", "${gameState.starCoins}") }
        item { StatRow("Total prestiges", "${gameState.totalPrestigeResets}") }
        item { StatRow("Coins on next reset", "${gameState.coinsOnReset}") }
        item { StatRow("Prestige multiplier", "%.1fx".format(gameState.prestigeMultiplier)) }

        // Research stats
        item { SectionHeader("🔬 Research") }
        item { StatRow("Research points", "${gameState.researchPoints}") }
        item { StatRow("RP generation", "${String.format(java.util.Locale.US, "%.2f", gameState.researchPointGenRate * 3600)} /hour") }
        item {
            val totalResearchLevels = gameState.researchLevels.values.sum()
            StatRow("Total research levels", "$totalResearchLevels")
        }

        // Premium stats
        item { SectionHeader("💎 Premium") }
        item { StatRow("Gems", "${gameState.gems}") }
        item { StatRow("Achievements unlocked", "${gameState.unlockedAchievements.size} / ${ACHIEVEMENTS.size}") }
        item { StatRow("Daily login streak", "${gameState.dailyLoginStreak} days") }
        if (gameState.gemBonusIncome > 0) {
            item { StatRow("Permanent gem income bonus", "+${String.format("%.0f", gameState.gemBonusIncome * 100)}%") }
        }

        // Time stats
        item { SectionHeader("⏱️ Time") }
        item { StatRow("Play time", "${String.format("%.1f", playTimeDays)} days") }
        item { StatRow("Offline efficiency", "${String.format("%.0f", gameState.offlineEfficiency * 100)}%") }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = StarBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = TextSecondary, fontSize = 13.sp)
            Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun findHighestEarningShip(state: GameState): Pair<ShipTier, Double>? {
    var best: Pair<ShipTier, Double>? = null
    val sector = state.activeSector
    val sectorState = state.activeSectorState

    val globalMult = state.prestigeMultiplier * state.researchIncomeMultiplier *
            state.adBoostMultiplier * state.speedMultiplier * (1.0 + state.gemBonusIncome)
    val shopIncomeMult = 1.0 + ((sectorState.shopLevels["warp_drive"] ?: 0) * 0.08)

    for (tier in SHIP_TIERS) {
        val shipState = sectorState.ships[tier.id] ?: ShipState()
        if (shipState.count == 0) continue

        val milestoneMulti = getMilestoneMultiplier(shipState.count)
        val baseIncome = tier.baseIncome * shipState.count * milestoneMulti * sector.incomeMultiplier

        val upgradeSpecialtyMult = if (sector.specialty == SectorSpecialty.UPGRADE_EFFICIENCY) 1.25 else 1.0
        val upgradeMultiplier = UPGRADE_TYPES.sumOf { upgrade ->
            val level = shipState.upgradeLevels[upgrade.id] ?: 0
            level * upgrade.incomeBoost * upgradeSpecialtyMult
        }
        val income = baseIncome * (1.0 + upgradeMultiplier) * shopIncomeMult * globalMult

        if (best == null || income > best.second) {
            best = Pair(tier, income)
        }
    }
    return best
}
