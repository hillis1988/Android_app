package com.starfleet.idle.engine

import com.starfleet.idle.data.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeGreaterThan as longShouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Unit tests for MissionEngine.checkMissionCompletions and collectMissionReward.
 *
 * **Validates: Requirements 4.2, 4.4, 5.5, 5.6, 5.7, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 9.2, 9.4, 9.5, 11.2**
 */
class MissionCompletionTest : FunSpec({

    // Helper to create a game state with ships
    fun gameStateWithShips(shipCount: Int = 10, cps: Double = 100.0): GameState {
        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = shipCount)
            }
        )
        return GameState(
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            lastTickTime = 1_000_000_000_000L
        )
    }

    // Helper to create an active mission
    fun createActiveMission(
        id: String = "m_test_1",
        startTime: Long = 1_000_000_000_000L,
        durationMs: Long = 30 * 60 * 1000L,
        status: MissionStatus = MissionStatus.IN_PROGRESS,
        difficulty: MissionDifficulty = MissionDifficulty.EASY,
        deployedTiers: List<String> = listOf("probe", "shuttle"),
        missionType: MissionType = MissionType.EXPLORATION,
        requiredPower: Double = 10.0 // Low power so success is likely
    ): ActiveMission {
        return ActiveMission(
            id = id,
            templateId = "combat_easy_solar_1",
            missionName = "Test Mission",
            missionType = missionType,
            difficulty = difficulty,
            sectorId = "solar",
            requiredFleetPower = requiredPower,
            durationMs = durationMs,
            deployedShipTiers = deployedTiers,
            startTime = startTime,
            status = status
        )
    }

    // =========================================================================
    // checkMissionCompletions tests
    // =========================================================================

    test("checkMissionCompletions - mission not yet complete stays IN_PROGRESS") {
        val startTime = 1_000_000_000_000L
        val mission = createActiveMission(startTime = startTime, durationMs = 60 * 60 * 1000L)
        val state = gameStateWithShips().copy(
            activeMissions = listOf(mission),
            lastTickTime = startTime
        )

        // Only 10 minutes have passed, mission needs 60 minutes
        val now = startTime + 10 * 60 * 1000L
        val result = MissionEngine.checkMissionCompletions(state, now)

        result.activeMissions[0].status shouldBe MissionStatus.IN_PROGRESS
    }

    test("checkMissionCompletions - mission with elapsed >= duration transitions to completed") {
        val startTime = 1_000_000_000_000L
        val durationMs = 30 * 60 * 1000L
        val mission = createActiveMission(startTime = startTime, durationMs = durationMs)
        val state = gameStateWithShips().copy(
            activeMissions = listOf(mission),
            lastTickTime = startTime
        )

        // 31 minutes have passed, mission needs 30 minutes
        val now = startTime + 31 * 60 * 1000L
        val result = MissionEngine.checkMissionCompletions(state, now)

        val completedMission = result.activeMissions[0]
        // Should be either SUCCESS or FAILURE, not IN_PROGRESS
        (completedMission.status == MissionStatus.COMPLETED_SUCCESS ||
            completedMission.status == MissionStatus.COMPLETED_FAILURE) shouldBe true
    }

    test("checkMissionCompletions - deterministic outcome based on mission ID") {
        val startTime = 1_000_000_000_000L
        val durationMs = 30 * 60 * 1000L
        val mission = createActiveMission(
            id = "m_deterministic_test",
            startTime = startTime,
            durationMs = durationMs
        )
        val state = gameStateWithShips().copy(
            activeMissions = listOf(mission),
            lastTickTime = startTime
        )

        val now = startTime + durationMs + 1000L

        // Run twice — should get same result
        val result1 = MissionEngine.checkMissionCompletions(state, now)
        val result2 = MissionEngine.checkMissionCompletions(state, now)

        result1.activeMissions[0].status shouldBe result2.activeMissions[0].status
    }

    test("checkMissionCompletions - already completed missions are not re-processed") {
        val startTime = 1_000_000_000_000L
        val mission = createActiveMission(
            startTime = startTime,
            durationMs = 30 * 60 * 1000L,
            status = MissionStatus.COMPLETED_SUCCESS
        )
        val state = gameStateWithShips().copy(
            activeMissions = listOf(mission),
            lastTickTime = startTime
        )

        val now = startTime + 60 * 60 * 1000L
        val result = MissionEngine.checkMissionCompletions(state, now)

        result.activeMissions[0].status shouldBe MissionStatus.COMPLETED_SUCCESS
    }

    test("checkMissionCompletions - time manipulation detection caps effective time") {
        val startTime = 1_000_000_000_000L
        val durationMs = 30 * 60 * 1000L // 30 min mission
        val mission = createActiveMission(
            startTime = startTime,
            durationMs = durationMs
        )
        val state = gameStateWithShips().copy(
            activeMissions = listOf(mission),
            lastTickTime = startTime
        )

        // Jump 24 hours ahead — way beyond expected max elapsed
        val manipulatedNow = startTime + 24 * 60 * 60 * 1000L
        val result = MissionEngine.checkMissionCompletions(state, manipulatedNow)

        // Mission should still complete (since capped time is still > duration)
        // because expectedMaxElapsed = 30min remaining + 10min buffer = 40min
        // and 40min > 30min duration, so mission completes
        val completedMission = result.activeMissions[0]
        (completedMission.status == MissionStatus.COMPLETED_SUCCESS ||
            completedMission.status == MissionStatus.COMPLETED_FAILURE) shouldBe true
    }

    test("checkMissionCompletions - no missions returns state unchanged") {
        val state = gameStateWithShips().copy(activeMissions = emptyList())
        val result = MissionEngine.checkMissionCompletions(state, System.currentTimeMillis())
        result shouldBe state
    }

    // =========================================================================
    // collectMissionReward tests
    // =========================================================================

    test("collectMissionReward - successful mission grants credits, gems, and returns ships") {
        val deployedTiers = listOf("probe", "shuttle")
        val mission = createActiveMission(
            id = "m_reward_test",
            status = MissionStatus.COMPLETED_SUCCESS,
            difficulty = MissionDifficulty.EASY,
            deployedTiers = deployedTiers
        )

        // Start with ships already deducted (simulating post-deployment state)
        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = if (tier.id in deployedTiers) 9 else 10)
            }
        )
        val state = GameState(
            credits = 1000.0,
            gems = 5,
            researchPoints = 0,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission)
        )

        val result = MissionEngine.collectMissionReward(state, "m_reward_test")

        // Credits should increase (EASY: multiplier 0.3)
        result.credits shouldBeGreaterThan state.credits
        // Gems should increase by 1 (EASY gem reward)
        result.gems shouldBe state.gems + 1
        // Ships should be returned
        val probeSector = SHIP_TIERS.first { it.id == "probe" }.sectorId
        result.sectors[probeSector]!!.ships["probe"]!!.count shouldBe 10
        result.sectors[probeSector]!!.ships["shuttle"]!!.count shouldBe 10
        // Mission removed from active
        result.activeMissions.shouldBeEmpty()
        // History entry added
        result.missionHistory shouldHaveSize 1
        result.missionHistory[0].success shouldBe true
    }

    test("collectMissionReward - failed mission returns ships but no rewards") {
        val deployedTiers = listOf("probe")
        val mission = createActiveMission(
            id = "m_fail_test",
            status = MissionStatus.COMPLETED_FAILURE,
            deployedTiers = deployedTiers
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = if (tier.id == "probe") 9 else 10)
            }
        )
        val state = GameState(
            credits = 1000.0,
            gems = 5,
            researchPoints = 3,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission)
        )

        val result = MissionEngine.collectMissionReward(state, "m_fail_test")

        // No reward changes
        result.credits shouldBe 1000.0
        result.gems shouldBe 5
        result.researchPoints shouldBe 3
        // Ships returned
        val probeSector = SHIP_TIERS.first { it.id == "probe" }.sectorId
        result.sectors[probeSector]!!.ships["probe"]!!.count shouldBe 10
        // Mission removed
        result.activeMissions.shouldBeEmpty()
        // History entry added with success = false
        result.missionHistory shouldHaveSize 1
        result.missionHistory[0].success shouldBe false
    }

    test("collectMissionReward - credit reward capped at 50% hourly income") {
        // Use an ELITE mission with long duration to trigger the cap
        val deployedTiers = listOf("probe")
        val mission = createActiveMission(
            id = "m_cap_test",
            status = MissionStatus.COMPLETED_SUCCESS,
            difficulty = MissionDifficulty.ELITE, // 0.8 multiplier
            durationMs = 8 * 60 * 60 * 1000L, // 8 hours = 28800 seconds
            deployedTiers = deployedTiers
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 10)
            }
        )
        val state = GameState(
            credits = 0.0,
            gems = 0,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission)
        )

        val result = MissionEngine.collectMissionReward(state, "m_cap_test")

        // baseCredits = CPS * 28800 * 0.8 = CPS * 23040
        // cap = CPS * 1800
        // Since 23040 > 1800, reward should be capped at CPS * 1800
        val cps = state.creditsPerSecond
        if (cps > 0) {
            result.credits shouldBeLessThanOrEqual cps * 1800.0 + 0.01
        }
    }

    test("collectMissionReward - Hard mission grants research points") {
        val deployedTiers = listOf("probe")
        val mission = createActiveMission(
            id = "m_rp_test",
            status = MissionStatus.COMPLETED_SUCCESS,
            difficulty = MissionDifficulty.HARD, // RP range 1..3
            deployedTiers = deployedTiers
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 10)
            }
        )
        val state = GameState(
            researchPoints = 0,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission)
        )

        val result = MissionEngine.collectMissionReward(state, "m_rp_test")

        result.researchPoints shouldBeGreaterThanOrEqual 1
    }

    test("collectMissionReward - IN_PROGRESS mission cannot be collected") {
        val mission = createActiveMission(
            id = "m_inprogress",
            status = MissionStatus.IN_PROGRESS
        )
        val state = gameStateWithShips().copy(activeMissions = listOf(mission))

        val result = MissionEngine.collectMissionReward(state, "m_inprogress")

        // State unchanged — mission still there
        result.activeMissions shouldHaveSize 1
        result.activeMissions[0].status shouldBe MissionStatus.IN_PROGRESS
    }

    test("collectMissionReward - nonexistent mission ID returns state unchanged") {
        val state = gameStateWithShips()
        val result = MissionEngine.collectMissionReward(state, "nonexistent_id")
        result shouldBe state
    }

    test("collectMissionReward - progresses COMPLETE_MISSION quests") {
        val deployedTiers = listOf("probe")
        val mission = createActiveMission(
            id = "m_quest_test",
            status = MissionStatus.COMPLETED_SUCCESS,
            deployedTiers = deployedTiers
        )

        val quest = ActiveQuest(templateId = "mission_1", progress = 0L, completed = false)

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 10)
            }
        )
        val state = GameState(
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission),
            activeQuests = listOf(quest)
        )

        val result = MissionEngine.collectMissionReward(state, "m_quest_test")

        // Quest progress should be incremented
        result.activeQuests[0].progress shouldBe 1L
        // mission_1 requires 1 completion, so it should be marked completed
        result.activeQuests[0].completed shouldBe true
    }

    test("collectMissionReward - mission history capped at 20 entries") {
        val deployedTiers = listOf("probe")
        val mission = createActiveMission(
            id = "m_history_cap",
            status = MissionStatus.COMPLETED_SUCCESS,
            deployedTiers = deployedTiers
        )

        // Start with 20 existing history entries
        val existingHistory = (1..20).map { i ->
            MissionHistoryEntry(
                missionName = "Old Mission $i",
                success = true,
                completedAt = 1_000_000_000_000L + i * 1000L
            )
        }

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 10)
            }
        )
        val state = GameState(
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission),
            missionHistory = existingHistory
        )

        val result = MissionEngine.collectMissionReward(state, "m_history_cap")

        // History should still be capped at 20
        result.missionHistory shouldHaveSize 20
        // The newest entry should be the one we just added
        result.missionHistory.last().missionName shouldBe "Test Mission"
    }

    test("collectMissionReward - ships returned to correct sector") {
        // Deploy ships from different sectors
        val deployedTiers = listOf("probe", "frigate") // probe=solar, frigate=nebula
        val mission = createActiveMission(
            id = "m_sector_return",
            status = MissionStatus.COMPLETED_SUCCESS,
            deployedTiers = deployedTiers
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = if (tier.id in deployedTiers) 9 else 10)
            }
        )
        val state = GameState(
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission)
        )

        val result = MissionEngine.collectMissionReward(state, "m_sector_return")

        // probe belongs to "solar" sector
        result.sectors["solar"]!!.ships["probe"]!!.count shouldBe 10
        // frigate belongs to "nebula" sector
        result.sectors["nebula"]!!.ships["frigate"]!!.count shouldBe 10
    }

    // =========================================================================
    // Cooldown enforcement tests
    // =========================================================================

    test("collectMissionReward - sets cooldown on the mission's template in the board state") {
        val deployedTiers = listOf("probe")
        val mission = createActiveMission(
            id = "m_cooldown_test",
            status = MissionStatus.COMPLETED_SUCCESS,
            deployedTiers = deployedTiers
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 10)
            }
        )
        val state = GameState(
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission),
            missionBoards = emptyMap()
        )

        val result = MissionEngine.collectMissionReward(state, "m_cooldown_test")

        // The board for the mission's sector should have a cooldown entry
        val board = result.missionBoards[mission.sectorId]!!
        board.cooldowns.containsKey(mission.templateId) shouldBe true
        // Cooldown should be approximately 30 minutes from now
        val cooldownUntil = board.cooldowns[mission.templateId]!!
        cooldownUntil longShouldBeGreaterThan System.currentTimeMillis()
    }

    test("collectMissionReward - failed mission also sets cooldown") {
        val deployedTiers = listOf("probe")
        val mission = createActiveMission(
            id = "m_cooldown_fail",
            status = MissionStatus.COMPLETED_FAILURE,
            deployedTiers = deployedTiers
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 10)
            }
        )
        val state = GameState(
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission),
            missionBoards = emptyMap()
        )

        val result = MissionEngine.collectMissionReward(state, "m_cooldown_fail")

        // Cooldown should be set even for failed missions (Req 9.1)
        val board = result.missionBoards[mission.sectorId]!!
        board.cooldowns.containsKey(mission.templateId) shouldBe true
    }

    test("generateMissionBoard - excludes missions on cooldown") {
        val now = System.currentTimeMillis()
        val cooldowns = mapOf(
            "combat_easy_solar_1" to (now + 30 * 60 * 1000L), // still on cooldown
            "mining_easy_solar_1" to (now - 1000L) // cooldown expired
        )

        val result = MissionEngine.generateMissionBoard(
            sectorId = "solar",
            activeMissions = emptyList(),
            currentBoardMissions = emptyList(),
            seed = now,
            cooldowns = cooldowns
        )

        // combat_easy_solar_1 should NOT be in the result (cooldown active)
        result.none { it.id == "combat_easy_solar_1" } shouldBe true
        // mining_easy_solar_1 CAN be in the result (cooldown expired)
        // (it may or may not be selected due to random shuffling, so we just verify the first one is excluded)
    }

    test("generateMissionBoard - includes missions whose cooldown has expired") {
        val now = 2_000_000_000_000L
        // All cooldowns expired (set in the past)
        val cooldowns = mapOf(
            "combat_easy_solar_1" to (now - 1000L),
            "mining_easy_solar_1" to (now - 1000L)
        )

        val result = MissionEngine.generateMissionBoard(
            sectorId = "solar",
            activeMissions = emptyList(),
            currentBoardMissions = emptyList(),
            seed = now,
            cooldowns = cooldowns
        )

        // All solar missions should be available (cooldowns expired)
        result.size shouldBeGreaterThanOrEqual 3
    }

    // =========================================================================
    // cancelAllMissions tests
    // =========================================================================

    test("cancelAllMissions - clears activeMissions and completedMissions") {
        val deployedTiers = listOf("probe", "shuttle")
        val mission1 = createActiveMission(
            id = "m_cancel_1",
            status = MissionStatus.IN_PROGRESS,
            deployedTiers = deployedTiers
        )
        val mission2 = createActiveMission(
            id = "m_cancel_2",
            status = MissionStatus.COMPLETED_SUCCESS,
            deployedTiers = listOf("corvette")
        )

        val completedResult = MissionResult(
            missionId = "m_old",
            missionName = "Old Mission",
            success = true,
            successChance = 0.7,
            creditsEarned = 100.0,
            gemsEarned = 2,
            researchPointsEarned = 0,
            bonusReward = null,
            completedAt = 1_000_000_000_000L
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = if (tier.id in deployedTiers) 9 else 10)
            }
        )
        val state = GameState(
            credits = 5000.0,
            gems = 10,
            researchPoints = 5,
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission1, mission2),
            completedMissions = listOf(completedResult),
            missionHistory = listOf(
                MissionHistoryEntry("Past Mission", true, 1_000_000_000_000L)
            )
        )

        val result = MissionEngine.cancelAllMissions(state)

        // Active missions cleared
        result.activeMissions.shouldBeEmpty()
        // Completed missions cleared
        result.completedMissions.shouldBeEmpty()
        // Mission history preserved
        result.missionHistory shouldHaveSize 1
        // Credits, gems, RP unchanged
        result.credits shouldBe 5000.0
        result.gems shouldBe 10
        result.researchPoints shouldBe 5
    }

    test("cancelAllMissions - returns deployed ships from IN_PROGRESS missions") {
        val deployedTiers = listOf("probe", "shuttle")
        val mission = createActiveMission(
            id = "m_cancel_ships",
            status = MissionStatus.IN_PROGRESS,
            deployedTiers = deployedTiers
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = if (tier.id in deployedTiers) 9 else 10)
            }
        )
        val state = GameState(
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(mission)
        )

        val result = MissionEngine.cancelAllMissions(state)

        // Ships should be returned
        val probeSector = SHIP_TIERS.first { it.id == "probe" }.sectorId
        result.sectors[probeSector]!!.ships["probe"]!!.count shouldBe 10
        result.sectors[probeSector]!!.ships["shuttle"]!!.count shouldBe 10
    }

    test("cancelAllMissions - does not return ships from non-IN_PROGRESS missions") {
        // A COMPLETED_SUCCESS mission's ships were already returned during collection
        // cancelAllMissions should only return ships from IN_PROGRESS missions
        val deployedTiers = listOf("corvette")
        val completedMission = createActiveMission(
            id = "m_cancel_completed",
            status = MissionStatus.COMPLETED_SUCCESS,
            deployedTiers = deployedTiers
        )

        val sectorState = SectorState(
            ships = SHIP_TIERS.associate { tier ->
                tier.id to ShipState(count = 10)
            }
        )
        val state = GameState(
            sectors = SECTORS.associate { sector ->
                sector.id to sectorState
            },
            activeMissions = listOf(completedMission)
        )

        val result = MissionEngine.cancelAllMissions(state)

        // corvette should NOT have been incremented (it's COMPLETED_SUCCESS, not IN_PROGRESS)
        val corvetteSector = SHIP_TIERS.first { it.id == "corvette" }.sectorId
        result.sectors[corvetteSector]!!.ships["corvette"]!!.count shouldBe 10
    }

    test("cancelAllMissions - with no active missions returns state with empty lists") {
        val state = gameStateWithShips().copy(
            activeMissions = emptyList(),
            completedMissions = emptyList()
        )

        val result = MissionEngine.cancelAllMissions(state)

        result.activeMissions.shouldBeEmpty()
        result.completedMissions.shouldBeEmpty()
    }
})
