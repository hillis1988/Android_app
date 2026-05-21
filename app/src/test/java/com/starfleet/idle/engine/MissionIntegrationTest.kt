package com.starfleet.idle.engine

import com.starfleet.idle.data.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Integration-level unit tests for ViewModel-equivalent flows at the engine level.
 * Tests the same flows that GameViewModel calls: deploy, offline completion, and prestige.
 *
 * **Validates: Requirements 3.4, 4.2, 10.5**
 */
class MissionIntegrationTest : FunSpec({

    // Helper: create a GameState with ships in all sectors, high fleet power for missions
    fun gameStateWithFleet(shipCount: Int = 10): GameState {
        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = shipCount)
            }
        )
        return GameState(
            credits = 100_000.0,
            gems = 50,
            researchPoints = 10,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeSectorId = "solar",
            lastTickTime = 1_000_000_000_000L
        )
    }

    // =========================================================================
    // Test: Deploying a mission updates state correctly (Requirement 3.4)
    // =========================================================================

    test("deploy mission - activeMissions has 1 entry after deployment") {
        val state = gameStateWithFleet()
        val template = MISSION_TEMPLATES.first { it.id == "combat_easy_solar_1" }
        val selectedTiers = listOf("probe", "shuttle")

        // Validate deployment first
        val validation = MissionEngine.validateDeployment(template, selectedTiers, state)
        validation.isValid shouldBe true

        // Deploy
        val now = 1_000_000_000_000L
        val result = MissionEngine.deployMission(state, template, selectedTiers, now)

        result.activeMissions shouldHaveSize 1
    }

    test("deploy mission - ship counts decreased by 1 per deployed tier") {
        val state = gameStateWithFleet(shipCount = 5)
        val template = MISSION_TEMPLATES.first { it.id == "combat_easy_solar_1" }
        val selectedTiers = listOf("probe", "shuttle", "corvette")

        val now = 1_000_000_000_000L
        val result = MissionEngine.deployMission(state, template, selectedTiers, now)

        // probe, shuttle, corvette are all in "solar" sector
        result.sectors["solar"]!!.ships["probe"]!!.count shouldBe 4
        result.sectors["solar"]!!.ships["shuttle"]!!.count shouldBe 4
        result.sectors["solar"]!!.ships["corvette"]!!.count shouldBe 4
    }

    test("deploy mission - mission status is IN_PROGRESS") {
        val state = gameStateWithFleet()
        val template = MISSION_TEMPLATES.first { it.id == "combat_easy_solar_1" }
        val selectedTiers = listOf("probe")

        val now = 1_000_000_000_000L
        val result = MissionEngine.deployMission(state, template, selectedTiers, now)

        result.activeMissions[0].status shouldBe MissionStatus.IN_PROGRESS
    }

    test("deploy mission - deployed mission records correct template and tiers") {
        val state = gameStateWithFleet()
        val template = MISSION_TEMPLATES.first { it.id == "mining_easy_solar_1" }
        val selectedTiers = listOf("probe", "corvette")

        val now = 1_500_000_000_000L
        val result = MissionEngine.deployMission(state, template, selectedTiers, now)

        val mission = result.activeMissions[0]
        mission.templateId shouldBe "mining_easy_solar_1"
        mission.missionName shouldBe "Lunar Ore Collection"
        mission.missionType shouldBe MissionType.MINING
        mission.deployedShipTiers shouldBe listOf("probe", "corvette")
        mission.startTime shouldBe now
        mission.durationMs shouldBe template.durationMs
    }

    test("deploy mission - ships from different sectors are deducted from correct sectors") {
        val state = gameStateWithFleet(shipCount = 5)
        val template = MISSION_TEMPLATES.first { it.id == "combat_easy_solar_1" }
        // probe is in "solar", frigate is in "nebula"
        val selectedTiers = listOf("probe", "frigate")

        val now = 1_000_000_000_000L
        val result = MissionEngine.deployMission(state, template, selectedTiers, now)

        result.sectors["solar"]!!.ships["probe"]!!.count shouldBe 4
        result.sectors["nebula"]!!.ships["frigate"]!!.count shouldBe 4
    }

    // =========================================================================
    // Test: Offline load detects completed missions (Requirement 4.2)
    // =========================================================================

    test("offline load - mission whose startTime + durationMs < now transitions from IN_PROGRESS") {
        val startTime = 1_000_000_000_000L
        val durationMs = 30 * 60 * 1000L // 30 minutes

        val mission = ActiveMission(
            id = "m_offline_1",
            templateId = "combat_easy_solar_1",
            missionName = "Patrol Route Alpha",
            missionType = MissionType.COMBAT,
            difficulty = MissionDifficulty.EASY,
            sectorId = "solar",
            requiredFleetPower = 50.0,
            durationMs = durationMs,
            deployedShipTiers = listOf("probe"),
            startTime = startTime,
            status = MissionStatus.IN_PROGRESS
        )

        val state = gameStateWithFleet().copy(
            activeMissions = listOf(mission),
            lastTickTime = startTime
        )

        // Simulate returning after 1 hour (mission only needs 30 min)
        val now = startTime + 60 * 60 * 1000L
        val result = MissionEngine.checkMissionCompletions(state, now)

        val completedMission = result.activeMissions[0]
        (completedMission.status == MissionStatus.COMPLETED_SUCCESS ||
            completedMission.status == MissionStatus.COMPLETED_FAILURE) shouldBe true
    }

    test("offline load - mission still in progress if time not elapsed") {
        val startTime = 1_000_000_000_000L
        val durationMs = 4 * 60 * 60 * 1000L // 4 hours

        val mission = ActiveMission(
            id = "m_offline_2",
            templateId = "combat_hard_solar_1",
            missionName = "Orbital Defense Drill",
            missionType = MissionType.COMBAT,
            difficulty = MissionDifficulty.HARD,
            sectorId = "solar",
            requiredFleetPower = 500.0,
            durationMs = durationMs,
            deployedShipTiers = listOf("probe", "shuttle"),
            startTime = startTime,
            status = MissionStatus.IN_PROGRESS
        )

        val state = gameStateWithFleet().copy(
            activeMissions = listOf(mission),
            lastTickTime = startTime
        )

        // Only 1 hour has passed, mission needs 4 hours
        val now = startTime + 60 * 60 * 1000L
        val result = MissionEngine.checkMissionCompletions(state, now)

        result.activeMissions[0].status shouldBe MissionStatus.IN_PROGRESS
    }

    test("offline load - multiple missions checked, only elapsed ones complete") {
        val startTime = 1_000_000_000_000L

        val shortMission = ActiveMission(
            id = "m_short",
            templateId = "combat_easy_solar_1",
            missionName = "Patrol Route Alpha",
            missionType = MissionType.COMBAT,
            difficulty = MissionDifficulty.EASY,
            sectorId = "solar",
            requiredFleetPower = 50.0,
            durationMs = 30 * 60 * 1000L, // 30 min
            deployedShipTiers = listOf("probe"),
            startTime = startTime,
            status = MissionStatus.IN_PROGRESS
        )

        val longMission = ActiveMission(
            id = "m_long",
            templateId = "combat_hard_solar_1",
            missionName = "Orbital Defense Drill",
            missionType = MissionType.COMBAT,
            difficulty = MissionDifficulty.HARD,
            sectorId = "solar",
            requiredFleetPower = 500.0,
            durationMs = 4 * 60 * 60 * 1000L, // 4 hours
            deployedShipTiers = listOf("shuttle"),
            startTime = startTime,
            status = MissionStatus.IN_PROGRESS
        )

        val state = gameStateWithFleet().copy(
            activeMissions = listOf(shortMission, longMission),
            lastTickTime = startTime
        )

        // 1 hour has passed: short mission done, long mission still going
        val now = startTime + 60 * 60 * 1000L
        val result = MissionEngine.checkMissionCompletions(state, now)

        val short = result.activeMissions.first { it.id == "m_short" }
        val long = result.activeMissions.first { it.id == "m_long" }

        (short.status == MissionStatus.COMPLETED_SUCCESS ||
            short.status == MissionStatus.COMPLETED_FAILURE) shouldBe true
        long.status shouldBe MissionStatus.IN_PROGRESS
    }

    // =========================================================================
    // Test: Prestige cancels active missions (Requirement 10.5)
    // =========================================================================

    test("prestige - activeMissions is empty after prestige") {
        val mission = ActiveMission(
            id = "m_prestige_1",
            templateId = "combat_easy_solar_1",
            missionName = "Patrol Route Alpha",
            missionType = MissionType.COMBAT,
            difficulty = MissionDifficulty.EASY,
            sectorId = "solar",
            requiredFleetPower = 50.0,
            durationMs = 30 * 60 * 1000L,
            deployedShipTiers = listOf("probe"),
            startTime = 1_000_000_000_000L,
            status = MissionStatus.IN_PROGRESS
        )

        // Need enough fleet power for prestige to grant coins (> 1000 power)
        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 50)
            }
        )
        val state = GameState(
            credits = 100_000.0,
            gems = 25,
            researchPoints = 10,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission),
            lastTickTime = 1_000_000_000_000L
        )

        val result = GameEngine.prestige(state)

        result.activeMissions.shouldBeEmpty()
    }

    test("prestige - completedMissions is empty after prestige") {
        val completedResult = MissionResult(
            missionId = "m_completed",
            missionName = "Old Mission",
            success = true,
            successChance = 0.7,
            creditsEarned = 500.0,
            gemsEarned = 2,
            researchPointsEarned = 0,
            bonusReward = null,
            completedAt = 1_000_000_000_000L
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 50)
            }
        )
        val state = GameState(
            credits = 100_000.0,
            gems = 25,
            researchPoints = 10,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            completedMissions = listOf(completedResult),
            lastTickTime = 1_000_000_000_000L
        )

        val result = GameEngine.prestige(state)

        result.completedMissions.shouldBeEmpty()
    }

    test("prestige - missionHistory is preserved") {
        val historyEntry = MissionHistoryEntry(
            missionName = "Past Mission",
            success = true,
            completedAt = 900_000_000_000L
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 50)
            }
        )
        val state = GameState(
            credits = 100_000.0,
            gems = 25,
            researchPoints = 10,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            missionHistory = listOf(historyEntry),
            lastTickTime = 1_000_000_000_000L
        )

        val result = GameEngine.prestige(state)

        result.missionHistory shouldHaveSize 1
        result.missionHistory[0].missionName shouldBe "Past Mission"
        result.missionHistory[0].success shouldBe true
    }

    test("prestige - gems are preserved through prestige") {
        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 50)
            }
        )
        val state = GameState(
            credits = 100_000.0,
            gems = 42,
            researchPoints = 10,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(
                ActiveMission(
                    id = "m_p",
                    templateId = "combat_easy_solar_1",
                    missionName = "Test",
                    missionType = MissionType.COMBAT,
                    difficulty = MissionDifficulty.EASY,
                    sectorId = "solar",
                    requiredFleetPower = 50.0,
                    durationMs = 30 * 60 * 1000L,
                    deployedShipTiers = listOf("probe"),
                    startTime = 1_000_000_000_000L,
                    status = MissionStatus.IN_PROGRESS
                )
            ),
            lastTickTime = 1_000_000_000_000L
        )

        val result = GameEngine.prestige(state)

        result.gems shouldBe 42
    }

    test("prestige - no mission rewards granted from cancelled missions") {
        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 50)
            }
        )
        val initialCredits = 100_000.0
        val initialGems = 25
        val initialRp = 10

        val state = GameState(
            credits = initialCredits,
            gems = initialGems,
            researchPoints = initialRp,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(
                ActiveMission(
                    id = "m_no_reward",
                    templateId = "combat_elite_deepspace_1",
                    missionName = "Leviathan Hunt",
                    missionType = MissionType.COMBAT,
                    difficulty = MissionDifficulty.ELITE,
                    sectorId = "deepspace",
                    requiredFleetPower = 5_000_000.0,
                    durationMs = 8 * 60 * 60 * 1000L,
                    deployedShipTiers = listOf("battlecruiser", "carrier"),
                    startTime = 1_000_000_000_000L,
                    status = MissionStatus.IN_PROGRESS
                )
            ),
            lastTickTime = 1_000_000_000_000L
        )

        val result = GameEngine.prestige(state)

        // Gems should be preserved (not increased by mission rewards)
        result.gems shouldBe initialGems
        // Research points should be preserved (not increased by mission rewards)
        result.researchPoints shouldBe initialRp
    }
})
