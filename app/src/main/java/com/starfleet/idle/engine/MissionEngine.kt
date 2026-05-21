package com.starfleet.idle.engine

import com.starfleet.idle.data.*
import kotlin.math.min
import kotlin.random.Random

object MissionEngine {

    private const val BOARD_REFRESH_INTERVAL_MS = 14_400_000L // 4 hours

    /** Check if fleet missions feature is unlocked (Orion Nebula sector). */
    fun isFeatureUnlocked(gameState: GameState): Boolean {
        return gameState.totalFleetPower >= 500_000.0
    }

    /** Get number of unlocked mission slots based on total fleet power. */
    fun getUnlockedSlotCount(totalFleetPower: Double): Int {
        return when {
            totalFleetPower >= 1_000_000_000.0 -> 3
            totalFleetPower >= 50_000_000.0 -> 2
            totalFleetPower >= 500_000.0 -> 1
            else -> 0
        }
    }

    /**
     * Generate a fresh mission board for a sector. Returns 3-6 missions.
     * Excludes missions that are currently active or on cooldown.
     * If fewer than 3 are available after filtering, returns whatever is available.
     *
     * @param cooldowns Map of templateId to cooldownUntil timestamp. Templates whose
     *   cooldown hasn't expired (cooldowns[id] > seed) are excluded from the board.
     */
    fun generateMissionBoard(
        sectorId: String,
        activeMissions: List<ActiveMission>,
        currentBoardMissions: List<MissionTemplate>,
        seed: Long,
        cooldowns: Map<String, Long> = emptyMap()
    ): List<MissionTemplate> {
        val random = Random(seed)

        // Filter templates by sector
        val sectorMissions = MISSION_TEMPLATES.filter { it.sectorId == sectorId }

        // Exclude missions whose templateId matches any active mission
        val activeTemplateIds = activeMissions.map { it.templateId }.toSet()

        // Exclude missions on cooldown:
        // 1. Check the cooldowns map (tracked in MissionBoardState)
        // 2. Also check the template's own cooldownUntil field (legacy/static cooldown)
        val available = sectorMissions.filter { template ->
            template.id !in activeTemplateIds &&
                template.cooldownUntil <= seed &&
                (cooldowns[template.id] ?: 0L) <= seed
        }

        // Shuffle using seed-based random
        val shuffled = available.shuffled(random)

        // Pick count between 3 and 6
        val count = if (shuffled.size >= 3) {
            random.nextInt(3, 7) // 3 to 6 inclusive
        } else {
            shuffled.size // Return whatever is available (per Req 1.5)
        }

        return shuffled.take(count.coerceAtMost(shuffled.size))
    }

    /** Check if a board needs refresh (elapsed ≥ 4 hours). */
    fun shouldRefreshBoard(lastRefreshTime: Long, now: Long): Boolean {
        return (now - lastRefreshTime) >= BOARD_REFRESH_INTERVAL_MS
    }

    /**
     * Calculate effective fleet power for a deployment against a mission type.
     * Sums each deployed ship's base power × its affinity multiplier for the mission type.
     */
    fun calculateEffectiveFleetPower(
        deployedShipTiers: List<String>,
        shipStates: Map<String, ShipState>,
        missionType: MissionType,
        gameState: GameState
    ): Double {
        return deployedShipTiers.sumOf { tierId ->
            val shipTier = SHIP_TIERS.find { it.id == tierId } ?: return@sumOf 0.0
            val affinity = SHIP_AFFINITIES.find { it.shipTierId == tierId } ?: return@sumOf 0.0
            val affinityMultiplier = when (missionType) {
                MissionType.COMBAT -> affinity.combat
                MissionType.MINING -> affinity.mining
                MissionType.EXPLORATION -> affinity.exploration
                MissionType.DIPLOMACY -> affinity.diplomacy
            }
            shipTier.basePower * affinityMultiplier
        }
    }

    /**
     * Calculate success chance (30%-95%) based on power ratio.
     * Piecewise linear:
     * - ratio ≤ 0.75: 0.30
     * - 0.75 < ratio ≤ 1.0: linear 30% to 70%
     * - 1.0 < ratio < 1.5: linear 70% to 95%
     * - ratio ≥ 1.5: 0.95
     */
    fun calculateSuccessChance(effectivePower: Double, requiredPower: Double): Double {
        if (requiredPower <= 0.0) return 0.95
        val ratio = effectivePower / requiredPower
        return when {
            ratio <= 0.75 -> 0.30
            ratio >= 1.5 -> 0.95
            ratio <= 1.0 -> {
                // Linear from 0.30 at ratio=0.75 to 0.70 at ratio=1.0
                0.30 + (ratio - 0.75) * (0.40 / 0.25)
            }
            else -> {
                // Linear from 0.70 at ratio=1.0 to 0.95 at ratio=1.5
                0.70 + (ratio - 1.0) * (0.25 / 0.5)
            }
        }
    }

    /** Validate a deployment: checks ship ownership, slot availability, tier conflicts. */
    fun validateDeployment(
        missionTemplate: MissionTemplate,
        selectedTiers: List<String>,
        gameState: GameState
    ): DeploymentValidation {
        // 1. Check tier count: empty
        if (selectedTiers.isEmpty()) {
            return DeploymentValidation(false, "Select at least 1 ship type")
        }

        // 2. Check tier count: too many
        if (selectedTiers.size > 5) {
            return DeploymentValidation(false, "Maximum 5 ship types per mission")
        }

        // 3. Check slot availability
        if (gameState.activeMissionCount >= gameState.missionSlotCount) {
            return DeploymentValidation(false, "No mission slots available")
        }

        // 4. Check ownership: player must own ≥1 of each tier across ALL sectors
        for (tierId in selectedTiers) {
            val totalOwned = gameState.sectors.values.sumOf { sectorState ->
                sectorState.ships[tierId]?.count ?: 0
            }
            if (totalOwned < 1) {
                val shipName = SHIP_TIERS.firstOrNull { it.id == tierId }?.name ?: tierId
                return DeploymentValidation(false, "You don't own any $shipName")
            }
        }

        // 5. Check duplicate tiers: no tier already deployed in an active IN_PROGRESS mission
        val deployedTiers = gameState.activeMissions
            .filter { it.status == MissionStatus.IN_PROGRESS }
            .flatMap { it.deployedShipTiers }
            .toSet()

        for (tierId in selectedTiers) {
            if (tierId in deployedTiers) {
                val shipName = SHIP_TIERS.firstOrNull { it.id == tierId }?.name ?: tierId
                return DeploymentValidation(false, "$shipName is already deployed")
            }
        }

        // All checks pass
        return DeploymentValidation(true)
    }

    /** Deploy ships to a mission. Returns updated GameState. */
    fun deployMission(
        gameState: GameState,
        missionTemplate: MissionTemplate,
        selectedTiers: List<String>,
        now: Long = System.currentTimeMillis()
    ): GameState {
        // 1. Generate a unique mission ID
        val missionId = "m_${now}_${missionTemplate.id}"

        // 2. Create an ActiveMission
        val activeMission = ActiveMission(
            id = missionId,
            templateId = missionTemplate.id,
            missionName = missionTemplate.name,
            missionType = missionTemplate.type,
            difficulty = missionTemplate.difficulty,
            sectorId = missionTemplate.sectorId,
            requiredFleetPower = missionTemplate.requiredFleetPower,
            durationMs = missionTemplate.durationMs,
            deployedShipTiers = selectedTiers,
            startTime = now,
            status = MissionStatus.IN_PROGRESS
        )

        // 3. Deduct 1 ship per deployed tier from the correct sector
        // Each ship tier belongs to a specific sector (ShipTier.sectorId)
        val updatedSectors = gameState.sectors.toMutableMap()
        for (tierId in selectedTiers) {
            val shipTier = SHIP_TIERS.firstOrNull { it.id == tierId } ?: continue
            val sectorId = shipTier.sectorId
            val sectorState = updatedSectors[sectorId] ?: SectorState()
            val shipState = sectorState.ships[tierId] ?: ShipState()

            val updatedShipState = shipState.copy(count = maxOf(0, shipState.count - 1))
            val updatedShips = sectorState.ships.toMutableMap()
            updatedShips[tierId] = updatedShipState

            updatedSectors[sectorId] = sectorState.copy(ships = updatedShips)
        }

        // 4. Add the ActiveMission to activeMissions and return updated state
        return gameState.copy(
            sectors = updatedSectors,
            activeMissions = gameState.activeMissions + activeMission
        )
    }

    /**
     * Check and complete any finished missions. Called on game load and periodically.
     * Implements time manipulation detection: if clock jump > 10 minutes forward
     * beyond expected max elapsed, cap at last known good timestamp.
     */
    fun checkMissionCompletions(
        gameState: GameState,
        now: Long = System.currentTimeMillis()
    ): GameState {
        val inProgressMissions = gameState.activeMissions.filter { it.status == MissionStatus.IN_PROGRESS }
        if (inProgressMissions.isEmpty()) return gameState

        // Time manipulation detection:
        // Calculate the longest remaining time among active missions + 10 min buffer
        val longestRemainingMs = inProgressMissions.maxOf { mission ->
            val elapsed = gameState.lastTickTime - mission.startTime
            val remaining = mission.durationMs - elapsed
            maxOf(0L, remaining)
        }
        val expectedMaxElapsed = longestRemainingMs + 10 * 60 * 1000L

        // If now jumps too far ahead of lastTickTime, cap it
        val effectiveNow = if (now - gameState.lastTickTime > expectedMaxElapsed) {
            gameState.lastTickTime + expectedMaxElapsed
        } else {
            now
        }

        // Process each active mission
        val updatedMissions = gameState.activeMissions.map { mission ->
            if (mission.status != MissionStatus.IN_PROGRESS) return@map mission

            val elapsed = effectiveNow - mission.startTime
            if (elapsed >= mission.durationMs) {
                // Mission duration has elapsed — determine success or failure
                val effectivePower = calculateEffectiveFleetPower(
                    deployedShipTiers = mission.deployedShipTiers,
                    shipStates = gameState.sectors.values.firstOrNull()?.ships ?: emptyMap(),
                    missionType = mission.missionType,
                    gameState = gameState
                )
                val successChance = calculateSuccessChance(effectivePower, mission.requiredFleetPower)

                // Deterministic roll using mission ID hashCode as seed
                val random = Random(mission.id.hashCode().toLong())
                val roll = random.nextDouble()

                if (roll < successChance) {
                    mission.copy(status = MissionStatus.COMPLETED_SUCCESS)
                } else {
                    mission.copy(status = MissionStatus.COMPLETED_FAILURE)
                }
            } else {
                mission // Still in progress
            }
        }

        return gameState.copy(activeMissions = updatedMissions)
    }

    /**
     * Collect rewards from a completed mission.
     * - COMPLETED_SUCCESS: grants credits (CPS × duration × multiplier, capped at 50% hourly),
     *   gems, research points, possible bonus reward (Elite only), returns ships, progresses quests.
     * - COMPLETED_FAILURE: returns ships only, no rewards.
     * Adds entry to missionHistory (capped at 20) and removes mission from activeMissions.
     */
    fun collectMissionReward(
        gameState: GameState,
        missionId: String
    ): GameState {
        val mission = gameState.activeMissions.find { it.id == missionId } ?: return gameState

        // Only collect from completed missions
        if (mission.status != MissionStatus.COMPLETED_SUCCESS &&
            mission.status != MissionStatus.COMPLETED_FAILURE
        ) {
            return gameState
        }

        val now = System.currentTimeMillis()
        var updatedState = gameState

        if (mission.status == MissionStatus.COMPLETED_SUCCESS) {
            // Calculate credit reward: CPS × duration_seconds × difficulty_multiplier
            // Capped at CPS × 1800 (50% of hourly income)
            val cps = updatedState.creditsPerSecond
            val durationSeconds = mission.durationMs / 1000.0
            val baseCredits = cps * durationSeconds * mission.difficulty.rewardMultiplier
            val cappedCredits = min(baseCredits, cps * 1800.0)

            // Gems from difficulty
            val gems = mission.difficulty.gemReward

            // Research points from difficulty range
            val rpRange = mission.difficulty.researchPointReward
            val researchPoints = if (rpRange.first == rpRange.last) {
                rpRange.first
            } else {
                Random(mission.id.hashCode().toLong() + 1).nextInt(rpRange.first, rpRange.last + 1)
            }

            // Bonus reward (Elite only, 25% chance)
            var bonusReward: BonusRewardType? = null
            if (mission.difficulty.hasBonusRewardChance) {
                val bonusRandom = Random(mission.id.hashCode().toLong() + 2)
                if (bonusRandom.nextDouble() < 0.25) {
                    bonusReward = BonusRewardType.entries[bonusRandom.nextInt(BonusRewardType.entries.size)]
                }
            }

            // Apply bonus reward end times
            var bonusIncomeEnd = updatedState.missionBonusIncomeEndTime
            var bonusFleetPowerEnd = updatedState.missionBonusFleetPowerEndTime
            var bonusResearchEnd = updatedState.missionBonusResearchEndTime
            if (bonusReward != null) {
                val endTime = now + 2 * 60 * 60 * 1000L // 2 hours
                when (bonusReward) {
                    BonusRewardType.INCOME_BOOST -> bonusIncomeEnd = endTime
                    BonusRewardType.FLEET_POWER_BOOST -> bonusFleetPowerEnd = endTime
                    BonusRewardType.RESEARCH_SPEED_BOOST -> bonusResearchEnd = endTime
                }
            }

            // Return ships: add 1 back to each deployed tier's sector
            val updatedSectors = returnShipsToSectors(updatedState.sectors, mission.deployedShipTiers)

            // Progress COMPLETE_MISSION quests
            val updatedQuests = updatedState.activeQuests.map { quest ->
                if (quest.template().type == QuestType.COMPLETE_MISSION && !quest.completed) {
                    val newProgress = quest.progress + 1
                    quest.copy(
                        progress = newProgress,
                        completed = newProgress >= quest.template().targetValue
                    )
                } else {
                    quest
                }
            }

            // Add to mission history (cap at 20)
            val historyEntry = MissionHistoryEntry(
                missionName = mission.missionName,
                success = true,
                completedAt = now
            )
            val updatedHistory = (updatedState.missionHistory + historyEntry).takeLast(20)

            // Apply all updates
            updatedState = updatedState.copy(
                credits = updatedState.credits + cappedCredits,
                gems = updatedState.gems + gems,
                researchPoints = updatedState.researchPoints + researchPoints,
                sectors = updatedSectors,
                activeQuests = updatedQuests,
                missionHistory = updatedHistory,
                missionBonusIncomeEndTime = bonusIncomeEnd,
                missionBonusFleetPowerEndTime = bonusFleetPowerEnd,
                missionBonusResearchEndTime = bonusResearchEnd
            )
        } else {
            // COMPLETED_FAILURE: return ships only, no rewards
            val updatedSectors = returnShipsToSectors(updatedState.sectors, mission.deployedShipTiers)

            // Add to mission history (cap at 20)
            val historyEntry = MissionHistoryEntry(
                missionName = mission.missionName,
                success = false,
                completedAt = now
            )
            val updatedHistory = (updatedState.missionHistory + historyEntry).takeLast(20)

            updatedState = updatedState.copy(
                sectors = updatedSectors,
                missionHistory = updatedHistory
            )
        }

        // Remove the mission from activeMissions
        updatedState = updatedState.copy(
            activeMissions = updatedState.activeMissions.filter { it.id != missionId }
        )

        // Apply cooldown: 30 minutes from now (Requirement 9.1)
        val cooldownDurationMs = 30 * 60 * 1000L
        val cooldownUntil = now + cooldownDurationMs
        val sectorId = mission.sectorId
        val currentBoard = updatedState.missionBoards[sectorId] ?: MissionBoardState()
        val updatedCooldowns = currentBoard.cooldowns + (mission.templateId to cooldownUntil)
        val updatedBoard = currentBoard.copy(cooldowns = updatedCooldowns)
        updatedState = updatedState.copy(
            missionBoards = updatedState.missionBoards + (sectorId to updatedBoard)
        )

        return updatedState
    }

    /**
     * Cancel all active missions (used during prestige).
     * - Returns all deployed ships from IN_PROGRESS missions
     * - Clears activeMissions list
     * - Clears completedMissions list (pending MissionResult collection)
     * - Does NOT clear missionHistory (persists through prestige for cosmetic purposes)
     * - Does NOT change credits, gems, or research points
     */
    fun cancelAllMissions(gameState: GameState): GameState {
        // Return ships from all IN_PROGRESS active missions
        val inProgressMissions = gameState.activeMissions.filter { it.status == MissionStatus.IN_PROGRESS }
        val allDeployedTiers = inProgressMissions.flatMap { it.deployedShipTiers }
        val updatedSectors = returnShipsToSectors(gameState.sectors, allDeployedTiers)

        return gameState.copy(
            sectors = updatedSectors,
            activeMissions = emptyList(),
            completedMissions = emptyList()
        )
    }

    /**
     * Return ships to their home sectors.
     * For each tier in deployedShipTiers, find which sector it belongs to
     * (via SHIP_TIERS[tier].sectorId) and increment that sector's ship count by 1.
     */
    private fun returnShipsToSectors(
        sectors: Map<String, SectorState>,
        deployedShipTiers: List<String>
    ): Map<String, SectorState> {
        val updatedSectors = sectors.toMutableMap()
        for (tierId in deployedShipTiers) {
            val shipTier = SHIP_TIERS.firstOrNull { it.id == tierId } ?: continue
            val sectorId = shipTier.sectorId
            val sectorState = updatedSectors[sectorId] ?: SectorState()
            val shipState = sectorState.ships[tierId] ?: ShipState()

            val updatedShipState = shipState.copy(count = shipState.count + 1)
            val updatedShips = sectorState.ships.toMutableMap()
            updatedShips[tierId] = updatedShipState

            updatedSectors[sectorId] = sectorState.copy(ships = updatedShips)
        }
        return updatedSectors
    }
}
