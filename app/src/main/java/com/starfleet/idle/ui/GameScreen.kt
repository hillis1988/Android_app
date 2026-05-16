package com.starfleet.idle.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.starfleet.idle.data.*
import com.starfleet.idle.ui.theme.*
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsState()
    val offlineEarnings by viewModel.offlineEarnings.collectAsState()
    val newAchievements by viewModel.newAchievements.collectAsState()
    val currentEncounter by viewModel.randomEncounter.collectAsState()
    val showDailyReward by viewModel.showDailyReward.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPrestigeDialog by remember { mutableStateOf(false) }
    var showHardResetDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }

    val tabs = listOf("🚀 Fleet", "💎 Premium", "🏪 Shop", "🔬 Research", "✨ Perks", "📊 Stats")

    Box(modifier = Modifier.fillMaxSize().background(SpaceBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatsBar(state = state, onGetGems = { selectedTab = 1 })

            // Sector selector
            SectorSelector(state = state, onSwitch = { viewModel.switchSector(it) })

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepSpace,
                contentColor = CreditGold,
                edgePadding = 4.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp) })
                }
            }

            // AnimatedContent(
            //    targetState = state.activeSectorId,
            //    transitionSpec = {
            //        (fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.8f))
            //            .togetherWith(fadeOut(animationSpec = tween(600)) + scaleOut(targetScale = 1.2f))
            //    },
            //    label = "SectorTransition"
            // ) { _ ->
                Column(modifier = Modifier.fillMaxSize()) {
                    // Specialty Banner
                    val currentSector = state.activeSector
                    if (currentSector.specialty != SectorSpecialty.NONE) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(NebulaPurple.copy(alpha = 0.1f)).padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "✨ ${currentSector.specialty.description}", color = StarBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    when (selectedTab) {
                        0 -> FleetTab(viewModel, state, { showPrestigeDialog = true }, { showHardResetDialog = true }, { showCreditsDialog = true })
                        1 -> PremiumScreen(state, { viewModel.activateAdBoost() }, { viewModel.activateSpeedBoost() }, { viewModel.buyGemItem(it) }, { viewModel.purchaseGemPack(it) })
                        2 -> ShopScreen(state, { viewModel.buyShopBonus(it) }, { viewModel.tap() })
                        3 -> ResearchScreen(state) { viewModel.buyResearch(it) }
                        4 -> PerkScreen(state) { viewModel.buyPerk(it) }
                        5 -> StatsScreen(gameState = state)
                    }
                }
            // }
        }

        // --- Dialogs ---

        // Offline earnings with double option
        /* offlineEarnings?.let { earned ->
            Dialog(onDismissRequest = { viewModel.dismissOfflineEarnings() }) {
                StarFleetIdleTheme {
                    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌙 Welcome Back, Commander!", color = CreditGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("Your fleet earned while you were away:", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text("+${formatNumber(earned)} credits", color = ShieldGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { viewModel.dismissOfflineEarnings() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepSpace)) {
                                    Text("Collect")
                                }
                                Button(onClick = { viewModel.doubleOfflineEarnings(earned) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)) {
                                    Text("📺 Double It!")
                                }
                            }
                            Text("Watch an ad to double your offline earnings", color = TextSecondary, fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        } */

        // Daily reward
        if (showDailyReward) {
            val rewardIndex = ((state.dailyLoginStreak - 1) % DAILY_REWARDS.size)
            val reward = if (rewardIndex >= 0) DAILY_REWARDS[rewardIndex] else null
            if (reward != null) {
                Dialog(onDismissRequest = { viewModel.claimDailyReward() }) {
                    StarFleetIdleTheme {
                        Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📅 DAILY REWARD", color = CreditGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("Day ${state.dailyLoginStreak} streak!", color = ShieldGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("${reward.emoji} ${reward.description}", color = TextPrimary, fontSize = 16.sp)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { viewModel.claimDailyReward() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)) {
                                    Text("Claim!")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Achievement popup
        if (newAchievements.isNotEmpty()) {
            val achievementNames = newAchievements.mapNotNull { id -> ACHIEVEMENTS.find { it.id == id } }
            Dialog(onDismissRequest = { viewModel.dismissNewAchievements() }) {
                StarFleetIdleTheme {
                    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆 ACHIEVEMENT UNLOCKED!", color = CreditGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            achievementNames.forEach { a ->
                                Text("${a.emoji} ${a.name}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("+${a.gemReward} 💎", color = NebulaPurple, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.dismissNewAchievements() },
                                colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)) {
                                Text("Nice!")
                            }
                        }
                    }
                }
            }
        }

        // Space Encounter popup
        currentEncounter?.let { data ->
            val ui = getEncounterUI(data.type)
            Dialog(onDismissRequest = { viewModel.dismissEncounter() }) {
                StarFleetIdleTheme {
                    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(ui.emoji, fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(ui.title, color = CreditGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text(ui.description, color = TextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(20.dp))
                            ui.options.forEachIndexed { index, option ->
                                Button(
                                    onClick = { viewModel.handleEncounter(index) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepSpace)
                                ) {
                                    Text(option)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            TextButton(onClick = { viewModel.dismissEncounter() }) {
                                Text("Ignore", color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Prestige dialog
        if (showPrestigeDialog) {
            val coinsToEarn = state.coinsOnReset
            val newTotal = state.starCoins + coinsToEarn
            val newMult = getPrestigeMultiplier(newTotal)
            Dialog(onDismissRequest = { showPrestigeDialog = false }) {
                StarFleetIdleTheme {
                    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🪙 PRESTIGE RESET", color = CreditGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            if (coinsToEarn > 0) {
                                Text("Reset your fleet and earn:", color = TextSecondary, fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("+$coinsToEarn Star Coins", color = CreditGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text("Total: $newTotal coins · ${String.format("%.1f", newMult)}x income", color = ShieldGreen, fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Ships, upgrades, and shop bonuses reset.\nResearch, Gems, and Star Coins are permanent.",
                                    color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(onClick = { showPrestigeDialog = false }) { Text("Cancel", color = TextSecondary) }
                                    Button(onClick = { viewModel.prestige(); showPrestigeDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)) { Text("Prestige!") }
                                }
                            } else {
                                Text("Reach 1,000+ total Fleet Power to earn coins.", color = AlertRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { showPrestigeDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)) { Text("Got it") }
                            }
                        }
                    }
                }
            }
        }

        // Hard reset dialog
        if (showHardResetDialog) {
            Dialog(onDismissRequest = { showHardResetDialog = false }) {
                StarFleetIdleTheme {
                    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠️ HARD RESET", color = AlertRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("Erase EVERYTHING. This cannot be undone.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { showHardResetDialog = false }) { Text("Cancel", color = TextSecondary) }
                                Button(onClick = { viewModel.hardReset(); showHardResetDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)) { Text("Erase All") }
                            }
                        }
                    }
                }
            }
        }

        // Credits dialog
        if (showCreditsDialog) {
            CreditsDialog(onDismiss = { showCreditsDialog = false })
        }

        // Random encounter dialog
        currentEncounter?.let { encounter ->
            EncounterDialog(
                encounter = encounter,
                onChoose = { idx -> viewModel.handleEncounter(idx) },
                onDismiss = { viewModel.dismissEncounter() }
            )
        }

        // First-time tutorial
        if (!state.seenTutorial) {
            TutorialDialog(onDismiss = { viewModel.completeTutorial() })
        }
    }
}

@Composable
private fun EncounterDialog(
    encounter: com.starfleet.idle.data.EncounterData,
    onChoose: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val ui = com.starfleet.idle.data.getEncounterUI(encounter.type)
    Dialog(onDismissRequest = onDismiss) {
        StarFleetIdleTheme {
            Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(ui.emoji, fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(ui.title, color = CreditGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(ui.description, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    ui.options.forEachIndexed { idx, opt ->
                        Button(
                            onClick = { onChoose(idx) },
                            colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(opt, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Skip", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialDialog(onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val steps = listOf(
        Triple("⭐ Welcome, Commander!",
            "You command a fleet of starships earning credits across the galaxy.",
            "Tap a ship to buy more — they earn passively, even when you're offline."),
        Triple("🌌 Explore Sectors",
            "Build 10 ships of the highest tier in a sector to unlock the next.",
            "Each new sector earns more — but costs more too. Choose your battles."),
        Triple("🪙 Prestige & Quests",
            "When progress slows, prestige to earn permanent Star Coins.",
            "Complete daily quests for gems. Build the strongest fleet in the galaxy!")
    )
    Dialog(onDismissRequest = {}) {
        StarFleetIdleTheme {
            Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(steps[step].first, color = CreditGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(steps[step].second, color = TextPrimary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(steps[step].third, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        steps.indices.forEach { i ->
                            Box(
                                modifier = Modifier
                                    .width(8.dp).height(8.dp)
                                    .background(
                                        if (i == step) CreditGold else TextSecondary.copy(alpha = 0.3f),
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (step < steps.size - 1) step++ else onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (step < steps.size - 1) "Next" else "Let's Go!")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditsDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        StarFleetIdleTheme {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⭐ CREDITS", color = CreditGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    Text("StarFleet Idle", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("v1.0", color = TextSecondary, fontSize = 12.sp)

                    Spacer(Modifier.height(16.dp))
                    Divider(color = DeepSpace, thickness = 1.dp)
                    Spacer(Modifier.height(16.dp))

                    Text("Created by", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))

                    Text("Roy Hillis", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("&", color = TextSecondary, fontSize = 14.sp)
                    Text("Abbie Hillis", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { /* In-app purchase logic here later */ },
                            colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("☕ Buy Abbie a Frappe (£5)", fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                        Button(
                            onClick = { /* In-app purchase logic here later */ },
                            colors = ButtonDefaults.buttonColors(containerColor = StarBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🍺 Buy Roy a Pint (£5)", fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://www.linkedin.com/in/roy-hillis-529146207")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StarBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("\uD83D\uDD17 Roy on LinkedIn", fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider(color = DeepSpace, thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    Text("Thank you for playing!", color = CreditGold, fontSize = 13.sp)

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectorSelector(state: GameState, onSwitch: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth().background(DeepSpace).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(SECTORS.size) { index ->
            val sector = SECTORS[index]
            val unlocked = state.isSectorUnlocked(sector.id)
            val active = sector.id == state.activeSectorId
            FilterChip(
                selected = active, onClick = { if (unlocked) onSwitch(sector.id) }, enabled = unlocked,
                label = {
                    val labelText = if (unlocked) {
                        "${sector.emoji} ${sector.name}"
                    } else {
                        val reqShip = SHIP_TIERS.find { it.id == sector.unlockShipId }
                        val haveCount = if (reqShip != null) {
                            state.sectors[reqShip.sectorId]?.ships?.get(reqShip.id)?.count ?: 0
                        } else 0
                        "🔒 $haveCount/${sector.unlockShipCount} ${reqShip?.name ?: "???"}"
                    }
                    Text(text = labelText, fontSize = 10.sp, maxLines = 1)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NebulaPurple, selectedLabelColor = TextPrimary,
                    containerColor = CardBackground, labelColor = TextSecondary,
                    disabledContainerColor = SpaceBlack, disabledLabelColor = TextSecondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.height(30.dp)
            )
        }
    }
}

@Composable
private fun FleetTab(viewModel: GameViewModel, state: GameState, onPrestige: () -> Unit, onHardReset: () -> Unit, onCredits: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        BuyAmountSelector(selected = state.buyAmount, onSelect = { viewModel.setBuyAmount(it) })

        // Ad boost indicator or prompt
        if (state.isAdBoostActive) {
            Row(
                modifier = Modifier.fillMaxWidth().background(NebulaPurple.copy(alpha = 0.2f)).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val min = state.adBoostRemainingMs / 60_000
                Text("⚡ 2x INCOME · ${min / 60}h ${min % 60}m left", color = ShieldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CreditGold.copy(alpha = 0.1f))
                    .clickable { viewModel.activateAdBoost() }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📺 Watch Ad for 2x Income (1hr) ", color = CreditGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("▶", color = NebulaPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
            // Daily quests panel at top
            if (state.activeQuests.isNotEmpty()) {
                item {
                    QuestPanel(state = state, onClaim = { idx -> viewModel.claimQuestReward(idx) })
                }
            }

            val activeShips = SHIP_TIERS.filter { it.sectorId == state.activeSectorId }
            items(activeShips) { tier ->
                val shipState = state.activeShips[tier.id] ?: ShipState()
                ShipPanel(tier, shipState, state, { viewModel.buyShip(tier.id) }, { uid -> viewModel.buyUpgrade(tier.id, uid) })
            }
            item {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🪙 PRESTIGE", color = CreditGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Reset for +${state.coinsOnReset} Star Coins (each = +5% income forever)",
                            color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onPrestige, colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                            modifier = Modifier.fillMaxWidth()) { Text("Prestige Reset") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { viewModel.showLeaderboards() }) {
                        Text("🏆 Leaderboards", color = CreditGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onHardReset) { Text("Hard Reset (erase all)", color = AlertRed, fontSize = 11.sp) }
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onCredits) { Text("⭐ Credits", color = TextSecondary, fontSize = 11.sp) }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuyAmountSelector(selected: BuyAmount, onSelect: (BuyAmount) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(DeepSpace).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Buy:", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
        BuyAmount.entries.forEach { amount ->
            val sel = amount == selected
            FilterChip(
                selected = sel, onClick = { onSelect(amount) },
                label = { Text(amount.label, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NebulaPurple, selectedLabelColor = TextPrimary,
                    containerColor = CardBackground, labelColor = TextSecondary
                ),
                modifier = Modifier.height(28.dp).padding(horizontal = 2.dp)
            )
        }
    }
}
