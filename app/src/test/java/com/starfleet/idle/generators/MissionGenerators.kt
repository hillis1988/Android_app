package com.starfleet.idle.generators

import com.starfleet.idle.data.*
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*

/**
 * Custom Kotest generators for Fleet Missions property-based tests.
 * Provides reusable generators for mission-related domain objects.
 */
object MissionGenerators {

    // =========================================================================
    // Sector and Board Generation Generators
    // =========================================================================

    /** Generator for valid sector IDs from the SECTORS list. */
    val sectorIdArb: Arb<String> = Arb.element(SECTORS.map { it.id })

    /** Generator for sector IDs that have missions in MISSION_TEMPLATES. */
    val sectorIdWithMissionsArb: Arb<String> = Arb.element(
        MISSION_TEMPLATES.map { it.sectorId }.distinct()
    )

    /** Generator for sector IDs that do NOT exist in MISSION_TEMPLATES. */
    val sectorIdWithoutMissionsArb: Arb<String> = Arb.element(
        listOf("unknown_sector", "nonexistent", "fake_sector", "alpha_centauri", "andromeda")
    )

    /** Generator for random seeds (Long values). */
    val seedArb: Arb<Long> = Arb.long(min = 0L, max = Long.MAX_VALUE)

    /** Generator for timestamps representing "now" values. */
    val timestampArb: Arb<Long> = Arb.long(min = 1_000_000_000_000L, max = 2_000_000_000_000L)

    /** Generator for ActiveMission instances. */
    val activeMissionArb: Arb<ActiveMission> = arbitrary {
        val template = Arb.element(MISSION_TEMPLATES).bind()
        ActiveMission(
            id = "m_${Arb.long(1L, 999999L).bind()}",
            templateId = template.id,
            missionName = template.name,
            missionType = template.type,
            difficulty = template.difficulty,
            sectorId = template.sectorId,
            requiredFleetPower = template.requiredFleetPower,
            durationMs = template.durationMs,
            deployedShipTiers = listOf(Arb.element(SHIP_TIERS.map { it.id }).bind()),
            startTime = Arb.long(0L, System.currentTimeMillis()).bind(),
            status = MissionStatus.IN_PROGRESS
        )
    }

    /** Generator for a list of active missions (0-3). */
    val activeMissionListArb: Arb<List<ActiveMission>> = arbitrary {
        val count = Arb.int(0, 3).bind()
        List(count) { activeMissionArb.bind() }
    }

    // =========================================================================
    // Fleet Power and Success Chance Generators
    // =========================================================================

    /** Generate a random MissionType. */
    val missionTypeArb: Arb<MissionType> = Arb.enum<MissionType>()

    /** Generate a random subset of 1-5 distinct ship tier IDs. */
    val deployedShipTiersArb: Arb<List<String>> = Arb.int(1..5).flatMap { count ->
        Arb.shuffle(SHIP_TIERS.map { it.id }).map { shuffled ->
            shuffled.take(count)
        }
    }

    /** Generate a positive double suitable for fleet power values. */
    val positivePowerArb: Arb<Double> = Arb.double(min = 0.01, max = 1_000_000_000.0)
        .filter { it.isFinite() && it > 0.0 }

    /** Generate a ratio value for success chance testing. */
    val ratioArb: Arb<Double> = Arb.double(min = 0.01, max = 5.0)
        .filter { it.isFinite() && it > 0.0 }

    // =========================================================================
    // GameState Generators
    // =========================================================================

    /** Generate a basic GameState with ships owned (at least 1 of each tier). */
    val gameStateWithShipsArb: Arb<GameState> = Arb.int(1..100).map { shipCount ->
        val sectors = SECTORS.associate { sector ->
            val ships = SHIP_TIERS.filter { it.sectorId == sector.id }.associate { tier ->
                tier.id to ShipState(count = shipCount)
            }
            sector.id to SectorState(ships = ships)
        }
        GameState(sectors = sectors)
    }

    /**
     * Generate a GameState where the player owns 0 of at least one specific tier
     * from the selectedTiers list. Returns a Pair of (GameState, selectedTiers).
     */
    fun gameStateWithZeroOwnershipArb(tierCount: Int = 1): Arb<Pair<GameState, List<String>>> = arbitrary {
        val count = Arb.int(1, minOf(tierCount, 5).coerceAtLeast(1)).bind()
        val selectedTiers = Arb.shuffle(SHIP_TIERS.map { it.id }).bind().take(count)
        // Pick one tier to have 0 count
        val zeroTierIndex = Arb.int(0, selectedTiers.size - 1).bind()
        val zeroTierId = selectedTiers[zeroTierIndex]

        val sectors = SECTORS.associate { sector ->
            val ships = SHIP_TIERS.filter { it.sectorId == sector.id }.associate { tier ->
                val shipCount = if (tier.id == zeroTierId) 0 else Arb.int(1, 50).bind()
                tier.id to ShipState(count = shipCount)
            }
            sector.id to SectorState(ships = ships)
        }
        Pair(GameState(sectors = sectors), selectedTiers)
    }

    /**
     * Generate a GameState where the player owns ≥1 of every selected tier,
     * has available mission slots, and no duplicate tier conflicts.
     * Returns a Pair of (GameState, selectedTiers).
     */
    fun validDeploymentStateArb(tierCount: Int = 3): Arb<Pair<GameState, List<String>>> = arbitrary {
        val count = Arb.int(1, minOf(tierCount, 5).coerceAtLeast(1)).bind()
        val selectedTiers = Arb.shuffle(SHIP_TIERS.map { it.id }).bind().take(count)

        val sectors = SECTORS.associate { sector ->
            val ships = SHIP_TIERS.filter { it.sectorId == sector.id }.associate { tier ->
                tier.id to ShipState(count = Arb.int(1, 50).bind())
            }
            sector.id to SectorState(ships = ships)
        }
        // Ensure enough fleet power for at least 1 slot (≥500,000) and no active missions
        Pair(GameState(sectors = sectors, activeMissions = emptyList()), selectedTiers)
    }

    /**
     * Generate a GameState with exactly N active IN_PROGRESS missions filling all slots.
     * Uses high fleet power (3 slots) and fills the specified number of slots.
     */
    fun gameStateWithFullSlotsArb(slotCount: Int = 3): Arb<GameState> = arbitrary {
        // Create sectors with many ships to ensure high fleet power (3 slots available)
        val sectors = SECTORS.associate { sector ->
            val ships = SHIP_TIERS.filter { it.sectorId == sector.id }.associate { tier ->
                tier.id to ShipState(count = Arb.int(10, 100).bind())
            }
            sector.id to SectorState(ships = ships)
        }

        // Create active missions to fill all slots, each with unique tiers
        val allTierIds = SHIP_TIERS.map { it.id }.shuffled()
        val activeMissions = (0 until slotCount).map { i ->
            val template = MISSION_TEMPLATES[i % MISSION_TEMPLATES.size]
            ActiveMission(
                id = "m_fill_$i",
                templateId = template.id,
                missionName = template.name,
                missionType = template.type,
                difficulty = template.difficulty,
                sectorId = template.sectorId,
                requiredFleetPower = template.requiredFleetPower,
                durationMs = template.durationMs,
                deployedShipTiers = listOf(allTierIds[i % allTierIds.size]),
                startTime = System.currentTimeMillis(),
                status = MissionStatus.IN_PROGRESS
            )
        }

        GameState(sectors = sectors, activeMissions = activeMissions)
    }

    /**
     * Generate a GameState with an active mission that has specific deployed tiers.
     * Returns a Pair of (GameState, deployedTiersInActiveMission).
     */
    fun gameStateWithDeployedTiersArb(): Arb<Pair<GameState, List<String>>> = arbitrary {
        val deployedCount = Arb.int(1, 3).bind()
        val allTierIds = SHIP_TIERS.map { it.id }
        val deployedTiers = Arb.shuffle(allTierIds).bind().take(deployedCount)

        val sectors = SECTORS.associate { sector ->
            val ships = SHIP_TIERS.filter { it.sectorId == sector.id }.associate { tier ->
                tier.id to ShipState(count = Arb.int(2, 50).bind())
            }
            sector.id to SectorState(ships = ships)
        }

        val template = MISSION_TEMPLATES.first()
        val activeMission = ActiveMission(
            id = "m_existing_1",
            templateId = template.id,
            missionName = template.name,
            missionType = template.type,
            difficulty = template.difficulty,
            sectorId = template.sectorId,
            requiredFleetPower = template.requiredFleetPower,
            durationMs = template.durationMs,
            deployedShipTiers = deployedTiers,
            startTime = System.currentTimeMillis(),
            status = MissionStatus.IN_PROGRESS
        )

        Pair(GameState(sectors = sectors, activeMissions = listOf(activeMission)), deployedTiers)
    }

    /** Generator for a random MissionTemplate from the predefined list. */
    val missionTemplateArb: Arb<MissionTemplate> = Arb.element(MISSION_TEMPLATES)

    /** Generator for tier count selections of 0 (empty list). */
    val emptyTierSelectionArb: Arb<List<String>> = Arb.constant(emptyList())

    /** Generator for tier count selections of >5 (6-12 tiers). */
    val tooManyTiersArb: Arb<List<String>> = Arb.int(6, SHIP_TIERS.size).map { count ->
        SHIP_TIERS.map { it.id }.shuffled().take(count)
    }

    // =========================================================================
    // Mission Completion and Reward Generators
    // =========================================================================

    /** Generator for MissionDifficulty. */
    val missionDifficultyArb: Arb<MissionDifficulty> = Arb.enum<MissionDifficulty>()

    /** Generator for CPS values (positive, finite). */
    val cpsArb: Arb<Double> = Arb.double(min = 1.0, max = 1_000_000.0)
        .filter { it.isFinite() && it > 0.0 }

    /** Generator for mission duration in milliseconds (valid durations from difficulty options). */
    val durationMsArb: Arb<Long> = Arb.element(
        MissionDifficulty.entries.flatMap { it.durationOptions }.distinct()
    )

    /**
     * Generate a GameState with a completed (SUCCESS) mission ready for collection.
     * The mission has elapsed its full duration. Returns (GameState, missionId).
     */
    fun completedSuccessMissionStateArb(): Arb<Pair<GameState, String>> = arbitrary {
        val template = Arb.element(MISSION_TEMPLATES).bind()
        val deployedCount = Arb.int(1, 3).bind()
        val deployedTiers = Arb.shuffle(SHIP_TIERS.map { it.id }).bind().take(deployedCount)

        val sectors = SECTORS.associate { sector ->
            val ships = SHIP_TIERS.filter { it.sectorId == sector.id }.associate { tier ->
                tier.id to ShipState(count = Arb.int(5, 50).bind())
            }
            sector.id to SectorState(ships = ships)
        }

        val missionId = "m_test_${Arb.long(1L, 999999L).bind()}"
        val startTime = System.currentTimeMillis() - template.durationMs - 60_000L

        val activeMission = ActiveMission(
            id = missionId,
            templateId = template.id,
            missionName = template.name,
            missionType = template.type,
            difficulty = template.difficulty,
            sectorId = template.sectorId,
            requiredFleetPower = template.requiredFleetPower,
            durationMs = template.durationMs,
            deployedShipTiers = deployedTiers,
            startTime = startTime,
            status = MissionStatus.COMPLETED_SUCCESS
        )

        val gameState = GameState(
            sectors = sectors,
            activeMissions = listOf(activeMission)
        )
        Pair(gameState, missionId)
    }

    /**
     * Generate a GameState with a completed (FAILURE) mission ready for collection.
     * Returns (GameState, missionId).
     */
    fun completedFailureMissionStateArb(): Arb<Pair<GameState, String>> = arbitrary {
        val template = Arb.element(MISSION_TEMPLATES).bind()
        val deployedCount = Arb.int(1, 3).bind()
        val deployedTiers = Arb.shuffle(SHIP_TIERS.map { it.id }).bind().take(deployedCount)

        val sectors = SECTORS.associate { sector ->
            val ships = SHIP_TIERS.filter { it.sectorId == sector.id }.associate { tier ->
                tier.id to ShipState(count = Arb.int(5, 50).bind())
            }
            sector.id to SectorState(ships = ships)
        }

        val missionId = "m_test_fail_${Arb.long(1L, 999999L).bind()}"
        val startTime = System.currentTimeMillis() - template.durationMs - 60_000L

        val activeMission = ActiveMission(
            id = missionId,
            templateId = template.id,
            missionName = template.name,
            missionType = template.type,
            difficulty = template.difficulty,
            sectorId = template.sectorId,
            requiredFleetPower = template.requiredFleetPower,
            durationMs = template.durationMs,
            deployedShipTiers = deployedTiers,
            startTime = startTime,
            status = MissionStatus.COMPLETED_FAILURE
        )

        val gameState = GameState(
            sectors = sectors,
            activeMissions = listOf(activeMission)
        )
        Pair(gameState, missionId)
    }

    /**
     * Generate a GameState with an IN_PROGRESS mission and a configurable start time.
     * Returns (GameState, missionId, startTime, durationMs).
     */
    fun inProgressMissionStateArb(): Arb<InProgressMissionData> = arbitrary {
        val template = Arb.element(MISSION_TEMPLATES).bind()
        val deployedCount = Arb.int(1, 3).bind()
        val deployedTiers = Arb.shuffle(SHIP_TIERS.map { it.id }).bind().take(deployedCount)

        val sectors = SECTORS.associate { sector ->
            val ships = SHIP_TIERS.filter { it.sectorId == sector.id }.associate { tier ->
                tier.id to ShipState(count = Arb.int(5, 50).bind())
            }
            sector.id to SectorState(ships = ships)
        }

        val missionId = "m_prog_${Arb.long(1L, 999999L).bind()}"
        val baseTime = 1_700_000_000_000L
        val startTime = baseTime

        val activeMission = ActiveMission(
            id = missionId,
            templateId = template.id,
            missionName = template.name,
            missionType = template.type,
            difficulty = template.difficulty,
            sectorId = template.sectorId,
            requiredFleetPower = template.requiredFleetPower,
            durationMs = template.durationMs,
            deployedShipTiers = deployedTiers,
            startTime = startTime,
            status = MissionStatus.IN_PROGRESS
        )

        val gameState = GameState(
            sectors = sectors,
            activeMissions = listOf(activeMission),
            lastTickTime = baseTime
        )
        InProgressMissionData(gameState, missionId, startTime, template.durationMs)
    }

    /**
     * Generate a GameState with a COMPLETE_MISSION quest active and a completed success mission.
     * Returns (GameState, missionId, questTemplateId).
     */
    fun stateWithQuestAndCompletedMissionArb(): Arb<Triple<GameState, String, String>> = arbitrary {
        val template = Arb.element(MISSION_TEMPLATES).bind()
        val deployedCount = Arb.int(1, 3).bind()
        val deployedTiers = Arb.shuffle(SHIP_TIERS.map { it.id }).bind().take(deployedCount)

        val sectors = SECTORS.associate { sector ->
            val ships = SHIP_TIERS.filter { it.sectorId == sector.id }.associate { tier ->
                tier.id to ShipState(count = Arb.int(5, 50).bind())
            }
            sector.id to SectorState(ships = ships)
        }

        val missionId = "m_quest_${Arb.long(1L, 999999L).bind()}"
        val startTime = System.currentTimeMillis() - template.durationMs - 60_000L

        val activeMission = ActiveMission(
            id = missionId,
            templateId = template.id,
            missionName = template.name,
            missionType = template.type,
            difficulty = template.difficulty,
            sectorId = template.sectorId,
            requiredFleetPower = template.requiredFleetPower,
            durationMs = template.durationMs,
            deployedShipTiers = deployedTiers,
            startTime = startTime,
            status = MissionStatus.COMPLETED_SUCCESS
        )

        // Pick a COMPLETE_MISSION quest template
        val questTemplate = Arb.element(
            QUEST_TEMPLATES.filter { it.type == QuestType.COMPLETE_MISSION }
        ).bind()
        val initialProgress = Arb.long(0L, questTemplate.targetValue - 1).bind()
        val activeQuest = ActiveQuest(
            templateId = questTemplate.id,
            progress = initialProgress,
            completed = false,
            claimed = false
        )

        val gameState = GameState(
            sectors = sectors,
            activeMissions = listOf(activeMission),
            activeQuests = listOf(activeQuest)
        )
        Triple(gameState, missionId, questTemplate.id)
    }

    /**
     * Generate a GameState with multiple active missions (mix of IN_PROGRESS and COMPLETED)
     * plus some mission history and completed missions, suitable for testing prestige/cancelAll.
     */
    fun gameStateWithActiveMissionsForPrestigeArb(): Arb<GameState> = arbitrary {
        val sectors = SECTORS.associate { sector ->
            val ships = SHIP_TIERS.filter { it.sectorId == sector.id }.associate { tier ->
                tier.id to ShipState(count = Arb.int(5, 50).bind())
            }
            sector.id to SectorState(ships = ships)
        }

        // Create 1-3 IN_PROGRESS missions with unique deployed tiers
        val inProgressCount = Arb.int(1, 3).bind()
        val allTierIds = SHIP_TIERS.map { it.id }.shuffled()
        var tierIndex = 0

        val inProgressMissions = (0 until inProgressCount).map { i ->
            val template = MISSION_TEMPLATES[i % MISSION_TEMPLATES.size]
            val deployCount = Arb.int(1, 2).bind()
            val deployedTiers = allTierIds.subList(tierIndex, (tierIndex + deployCount).coerceAtMost(allTierIds.size))
            tierIndex += deployCount

            ActiveMission(
                id = "m_prestige_ip_$i",
                templateId = template.id,
                missionName = template.name,
                missionType = template.type,
                difficulty = template.difficulty,
                sectorId = template.sectorId,
                requiredFleetPower = template.requiredFleetPower,
                durationMs = template.durationMs,
                deployedShipTiers = deployedTiers,
                startTime = 1_700_000_000_000L,
                status = MissionStatus.IN_PROGRESS
            )
        }

        // Optionally add a COMPLETED_SUCCESS mission
        val hasCompleted = Arb.boolean().bind()
        val completedMissions = if (hasCompleted) {
            val template = MISSION_TEMPLATES.last()
            listOf(
                ActiveMission(
                    id = "m_prestige_done_0",
                    templateId = template.id,
                    missionName = template.name,
                    missionType = template.type,
                    difficulty = template.difficulty,
                    sectorId = template.sectorId,
                    requiredFleetPower = template.requiredFleetPower,
                    durationMs = template.durationMs,
                    deployedShipTiers = listOf(allTierIds.last()),
                    startTime = 1_700_000_000_000L - template.durationMs - 60_000L,
                    status = MissionStatus.COMPLETED_SUCCESS
                )
            )
        } else emptyList()

        // Some mission history
        val historyCount = Arb.int(0, 5).bind()
        val history = (0 until historyCount).map { i ->
            MissionHistoryEntry(
                missionName = "Past Mission $i",
                success = Arb.boolean().bind(),
                completedAt = 1_700_000_000_000L - (i * 3_600_000L)
            )
        }

        // Some completed mission results (pending collection)
        val completedResultCount = Arb.int(0, 2).bind()
        val completedResults = (0 until completedResultCount).map { i ->
            MissionResult(
                missionId = "m_result_$i",
                missionName = "Result Mission $i",
                success = true,
                successChance = 0.7,
                creditsEarned = 1000.0,
                gemsEarned = 2,
                researchPointsEarned = 1,
                bonusReward = null,
                completedAt = 1_700_000_000_000L - (i * 1_800_000L)
            )
        }

        GameState(
            sectors = sectors,
            credits = Arb.double(100.0, 1_000_000.0).filter { it.isFinite() }.bind(),
            gems = Arb.int(0, 500).bind(),
            researchPoints = Arb.int(0, 100).bind(),
            activeMissions = inProgressMissions + completedMissions,
            completedMissions = completedResults,
            missionHistory = history
        )
    }

    /** Data class for in-progress mission test data. */
    data class InProgressMissionData(
        val gameState: GameState,
        val missionId: String,
        val startTime: Long,
        val durationMs: Long
    )
}
