package com.starfleet.idle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.starfleet.idle.data.*
import com.starfleet.idle.engine.MissionEngine
import com.starfleet.idle.ui.theme.*

@Composable
fun MissionBoardScreen(
    gameState: GameState,
    onMissionSelected: (MissionTemplate) -> Unit,
    onCollectReward: (String) -> Unit,
    onBack: () -> Unit
) {
    val sectorId = gameState.activeSectorId
    val sector = gameState.activeSector
    val slotCount = gameState.missionSlotCount
    val activeCount = gameState.activeMissionCount
    val board = gameState.missionBoards[sectorId]
    val availableMissions = board?.missions ?: emptyList()

    // Active missions for this sector (IN_PROGRESS)
    val activeMissions = gameState.activeMissions.filter {
        it.sectorId == sectorId && it.status == MissionStatus.IN_PROGRESS
    }

    // Completed missions ready to collect (any sector)
    val completedMissions = gameState.completedMissions

    // Notification badge count
    val completedCount = completedMissions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // Header
        MissionBoardHeader(
            sectorName = sector.name,
            sectorEmoji = sector.emoji,
            activeCount = activeCount,
            slotCount = slotCount,
            completedCount = completedCount,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Completed missions section
            if (completedMissions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "✅ Completed",
                        subtitle = "$completedCount ready to collect"
                    )
                }
                items(completedMissions, key = { it.missionId }) { result ->
                    CompletedMissionCard(result = result, onCollect = onCollectReward)
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            // Active missions section
            if (activeMissions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "🚀 Active Missions",
                        subtitle = "$activeCount/$slotCount slots used"
                    )
                }
                items(activeMissions, key = { it.id }) { mission ->
                    ActiveMissionCard(mission = mission)
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            // Available missions section
            item {
                SectionHeader(
                    title = "📋 Available Missions",
                    subtitle = "${sector.emoji} ${sector.name}"
                )
            }
            if (availableMissions.isEmpty()) {
                item {
                    Text(
                        text = "No missions available. Check back later!",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(availableMissions, key = { it.id }) { mission ->
                    AvailableMissionCard(
                        mission = mission,
                        onClick = { onMissionSelected(mission) }
                    )
                }
            }

            // Locked slots section
            item { Spacer(Modifier.height(12.dp)) }
            item {
                LockedSlotsSection(totalFleetPower = gameState.totalFleetPower)
            }
        }
    }
}

@Composable
private fun MissionBoardHeader(
    sectorName: String,
    sectorEmoji: String,
    activeCount: Int,
    slotCount: Int,
    completedCount: Int,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            TextButton(onClick = onBack) {
                Text("← Back", color = StarBlue, fontSize = 13.sp)
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Fleet Missions",
                        color = CreditGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (completedCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        NotificationBadge(count = completedCount)
                    }
                }
                Text(
                    text = "$sectorEmoji $sectorName · $activeCount/$slotCount slots used",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Spacer to balance the back button
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun NotificationBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(AlertRed),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ActiveMissionCard(mission: ActiveMission) {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Update timer every second
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            now.longValue = System.currentTimeMillis()
        }
    }

    val elapsed = now.longValue - mission.startTime
    val remaining = (mission.durationMs - elapsed).coerceAtLeast(0L)
    val progress = (elapsed.toFloat() / mission.durationMs.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getMissionTypeEmoji(mission.missionType),
                    fontSize = 24.sp
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.missionName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DifficultyBadge(difficulty = mission.difficulty)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "⏱ ${formatDuration(remaining)}",
                            color = StarBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "🔄",
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = StarBlue,
                trackColor = DeepSpace
            )
        }
    }
}

@Composable
private fun CompletedMissionCard(result: MissionResult, onCollect: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (result.success) "✅" else "❌",
                fontSize = 24.sp
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.missionName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (result.success) "Mission Successful!" else "Mission Failed",
                    color = if (result.success) ShieldGreen else AlertRed,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { onCollect(result.missionId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (result.success) ShieldGreen else NebulaPurple
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Collect",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AvailableMissionCard(mission: MissionTemplate, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = mission.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DifficultyBadge(difficulty = mission.difficulty)
                        Text(
                            text = "⏱ ${formatMissionDuration(mission.durationMs)}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fleet power requirement
                Text(
                    text = "⚡ ${formatFleetPower(mission.requiredFleetPower)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                // Reward preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💎 ${mission.difficulty.gemReward}",
                        color = NebulaPurple,
                        fontSize = 11.sp
                    )
                    if (mission.difficulty.researchPointReward.last > 0) {
                        Text(
                            text = "🔬 ${mission.difficulty.researchPointReward.first}-${mission.difficulty.researchPointReward.last}",
                            color = StarBlue,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedSlotsSection(totalFleetPower: Double) {
    val slotThresholds = listOf(
        Triple(1, 500_000.0, "Orion Nebula"),
        Triple(2, 50_000_000.0, "Deep Space"),
        Triple(3, 1_000_000_000.0, "Galactic Core")
    )

    val unlockedSlots = MissionEngine.getUnlockedSlotCount(totalFleetPower)
    val lockedSlots = slotThresholds.filter { it.first > unlockedSlots }

    if (lockedSlots.isNotEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "🔒 Locked Slots",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            lockedSlots.forEach { (slot, threshold, sectorName) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSpace),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔒", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Slot $slot",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Requires ${formatFleetPower(threshold)} fleet power ($sectorName tier)",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: MissionDifficulty) {
    val (color, label) = when (difficulty) {
        MissionDifficulty.EASY -> ShieldGreen to "Easy"
        MissionDifficulty.MEDIUM -> CreditGold to "Medium"
        MissionDifficulty.HARD -> Color(0xFFF97316) to "Hard"
        MissionDifficulty.ELITE -> NebulaPurple to "Elite"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- Utility functions ---

private fun getMissionTypeEmoji(type: MissionType): String {
    return when (type) {
        MissionType.COMBAT -> "⚔️"
        MissionType.MINING -> "⛏️"
        MissionType.EXPLORATION -> "🔭"
        MissionType.DIPLOMACY -> "🤝"
    }
}

/** Format remaining time as countdown: "Xh Ym" or "Xm Ys" */
private fun formatDuration(remainingMs: Long): String {
    val totalSeconds = remainingMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

/** Format mission duration for display: "30m", "1h", "2h", "4h", "8h" */
private fun formatMissionDuration(durationMs: Long): String {
    val minutes = durationMs / (60 * 1000)
    return when {
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h"
    }
}

/** Format fleet power as: "500", "2K", "50K", "5M", "1B" etc. */
fun formatFleetPower(power: Double): String {
    return when {
        power >= 1_000_000_000.0 -> {
            val value = power / 1_000_000_000.0
            if (value == value.toLong().toDouble()) "${value.toLong()}B" else String.format("%.1fB", value)
        }
        power >= 1_000_000.0 -> {
            val value = power / 1_000_000.0
            if (value == value.toLong().toDouble()) "${value.toLong()}M" else String.format("%.1fM", value)
        }
        power >= 1_000.0 -> {
            val value = power / 1_000.0
            if (value == value.toLong().toDouble()) "${value.toLong()}K" else String.format("%.1fK", value)
        }
        else -> {
            if (power == power.toLong().toDouble()) "${power.toLong()}" else String.format("%.0f", power)
        }
    }
}
