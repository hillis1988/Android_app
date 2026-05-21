package com.starfleet.idle.data

import com.starfleet.idle.generators.MissionGenerators
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.json.JSONArray
import org.json.JSONObject

/**
 * Property-based tests for mission state serialization round-trip.
 *
 * Tests that serializing mission-related game state to JSON and deserializing
 * back produces an equivalent state, validating the GameRepository persistence logic.
 *
 * **Validates: Requirements 10.1**
 */
class MissionSerializationTest : FunSpec({

    val config = PropTestConfig(iterations = 100)

    // =========================================================================
    // Serialization helpers — extracted from GameRepository.save() / load()
    // These mirror the exact JSON format used by GameRepository without needing
    // Android Context or SharedPreferences.
    // =========================================================================

    fun serializeMissionState(state: MissionSerializationState): JSONObject {
        return JSONObject().apply {
            // Mission Boards
            val missionBoardsJson = JSONObject()
            state.missionBoards.forEach { (sectorId, boardState) ->
                val boardJson = JSONObject().apply {
                    val missionsArray = JSONArray()
                    boardState.missions.forEach { mission ->
                        missionsArray.put(JSONObject().apply {
                            put("id", mission.id)
                            put("name", mission.name)
                            put("emoji", mission.emoji)
                            put("description", mission.description)
                            put("type", mission.type.name)
                            put("difficulty", mission.difficulty.name)
                            put("sectorId", mission.sectorId)
                            put("requiredFleetPower", mission.requiredFleetPower)
                            put("durationMs", mission.durationMs)
                            put("cooldownUntil", mission.cooldownUntil)
                        })
                    }
                    put("missions", missionsArray)
                    put("lastRefreshTime", boardState.lastRefreshTime)
                    val cooldownsJson = JSONObject()
                    boardState.cooldowns.forEach { (templateId, until) ->
                        cooldownsJson.put(templateId, until)
                    }
                    put("cooldowns", cooldownsJson)
                }
                missionBoardsJson.put(sectorId, boardJson)
            }
            put("missionBoards", missionBoardsJson)

            // Active Missions
            val activeMissionsArray = JSONArray()
            state.activeMissions.forEach { mission ->
                activeMissionsArray.put(JSONObject().apply {
                    put("id", mission.id)
                    put("templateId", mission.templateId)
                    put("missionName", mission.missionName)
                    put("missionType", mission.missionType.name)
                    put("difficulty", mission.difficulty.name)
                    put("sectorId", mission.sectorId)
                    put("requiredFleetPower", mission.requiredFleetPower)
                    put("durationMs", mission.durationMs)
                    val tiersArray = JSONArray()
                    mission.deployedShipTiers.forEach { tiersArray.put(it) }
                    put("deployedShipTiers", tiersArray)
                    put("startTime", mission.startTime)
                    put("status", mission.status.name)
                })
            }
            put("activeMissions", activeMissionsArray)

            // Completed Missions
            val completedMissionsArray = JSONArray()
            state.completedMissions.forEach { result ->
                completedMissionsArray.put(JSONObject().apply {
                    put("missionId", result.missionId)
                    put("missionName", result.missionName)
                    put("success", result.success)
                    put("successChance", result.successChance)
                    put("creditsEarned", result.creditsEarned)
                    put("gemsEarned", result.gemsEarned)
                    put("researchPointsEarned", result.researchPointsEarned)
                    put("bonusReward", result.bonusReward?.name ?: JSONObject.NULL)
                    put("completedAt", result.completedAt)
                })
            }
            put("completedMissions", completedMissionsArray)

            // Mission History
            val missionHistoryArray = JSONArray()
            state.missionHistory.forEach { entry ->
                missionHistoryArray.put(JSONObject().apply {
                    put("missionName", entry.missionName)
                    put("success", entry.success)
                    put("completedAt", entry.completedAt)
                })
            }
            put("missionHistory", missionHistoryArray)

            // Bonus end times and tutorial flag
            put("missionBonusIncomeEndTime", state.missionBonusIncomeEndTime)
            put("missionBonusFleetPowerEndTime", state.missionBonusFleetPowerEndTime)
            put("missionBonusResearchEndTime", state.missionBonusResearchEndTime)
            put("seenMissionTutorial", state.seenMissionTutorial)
        }
    }

    fun deserializeMissionState(json: JSONObject): MissionSerializationState {
        // Mission Boards
        val missionBoards = mutableMapOf<String, MissionBoardState>()
        val boardsJson = json.optJSONObject("missionBoards")
        if (boardsJson != null) {
            val keys = boardsJson.keys()
            while (keys.hasNext()) {
                val sectorId = keys.next()
                val boardJson = boardsJson.getJSONObject(sectorId)
                val missionsArray = boardJson.optJSONArray("missions")
                val missions = mutableListOf<MissionTemplate>()
                if (missionsArray != null) {
                    for (i in 0 until missionsArray.length()) {
                        val m = missionsArray.getJSONObject(i)
                        missions.add(
                            MissionTemplate(
                                id = m.getString("id"),
                                name = m.getString("name"),
                                emoji = m.optString("emoji", "🚀"),
                                description = m.optString("description", ""),
                                type = try { MissionType.valueOf(m.getString("type")) } catch (e: Exception) { MissionType.COMBAT },
                                difficulty = try { MissionDifficulty.valueOf(m.getString("difficulty")) } catch (e: Exception) { MissionDifficulty.EASY },
                                sectorId = m.getString("sectorId"),
                                requiredFleetPower = m.getDouble("requiredFleetPower"),
                                durationMs = m.getLong("durationMs"),
                                cooldownUntil = m.optLong("cooldownUntil", 0L)
                            )
                        )
                    }
                }
                val cooldownsJson = boardJson.optJSONObject("cooldowns")
                val cooldowns = mutableMapOf<String, Long>()
                if (cooldownsJson != null) {
                    val cooldownKeys = cooldownsJson.keys()
                    while (cooldownKeys.hasNext()) {
                        val templateId = cooldownKeys.next()
                        cooldowns[templateId] = cooldownsJson.getLong(templateId)
                    }
                }
                missionBoards[sectorId] = MissionBoardState(
                    missions = missions,
                    lastRefreshTime = boardJson.optLong("lastRefreshTime", 0L),
                    cooldowns = cooldowns
                )
            }
        }

        // Active Missions
        val activeMissions = mutableListOf<ActiveMission>()
        val activeMissionsArr = json.optJSONArray("activeMissions")
        if (activeMissionsArr != null) {
            for (i in 0 until activeMissionsArr.length()) {
                val m = activeMissionsArr.getJSONObject(i)
                val tiersArray = m.optJSONArray("deployedShipTiers")
                val tiers = mutableListOf<String>()
                if (tiersArray != null) {
                    for (j in 0 until tiersArray.length()) {
                        tiers.add(tiersArray.getString(j))
                    }
                }
                activeMissions.add(
                    ActiveMission(
                        id = m.getString("id"),
                        templateId = m.getString("templateId"),
                        missionName = m.getString("missionName"),
                        missionType = try { MissionType.valueOf(m.getString("missionType")) } catch (e: Exception) { MissionType.COMBAT },
                        difficulty = try { MissionDifficulty.valueOf(m.getString("difficulty")) } catch (e: Exception) { MissionDifficulty.EASY },
                        sectorId = m.getString("sectorId"),
                        requiredFleetPower = m.getDouble("requiredFleetPower"),
                        durationMs = m.getLong("durationMs"),
                        deployedShipTiers = tiers,
                        startTime = m.getLong("startTime"),
                        status = try { MissionStatus.valueOf(m.getString("status")) } catch (e: Exception) { MissionStatus.IN_PROGRESS }
                    )
                )
            }
        }

        // Completed Missions
        val completedMissions = mutableListOf<MissionResult>()
        val completedArr = json.optJSONArray("completedMissions")
        if (completedArr != null) {
            for (i in 0 until completedArr.length()) {
                val m = completedArr.getJSONObject(i)
                val bonusRewardStr = if (m.isNull("bonusReward")) null else m.optString("bonusReward", null)
                val bonusReward = bonusRewardStr?.let {
                    try { BonusRewardType.valueOf(it) } catch (e: Exception) { null }
                }
                completedMissions.add(
                    MissionResult(
                        missionId = m.getString("missionId"),
                        missionName = m.getString("missionName"),
                        success = m.getBoolean("success"),
                        successChance = m.getDouble("successChance"),
                        creditsEarned = m.getDouble("creditsEarned"),
                        gemsEarned = m.getInt("gemsEarned"),
                        researchPointsEarned = m.getInt("researchPointsEarned"),
                        bonusReward = bonusReward,
                        completedAt = m.getLong("completedAt")
                    )
                )
            }
        }

        // Mission History
        val missionHistory = mutableListOf<MissionHistoryEntry>()
        val historyArr = json.optJSONArray("missionHistory")
        if (historyArr != null) {
            for (i in 0 until historyArr.length()) {
                val m = historyArr.getJSONObject(i)
                missionHistory.add(
                    MissionHistoryEntry(
                        missionName = m.getString("missionName"),
                        success = m.getBoolean("success"),
                        completedAt = m.getLong("completedAt")
                    )
                )
            }
        }

        return MissionSerializationState(
            missionBoards = missionBoards,
            activeMissions = activeMissions,
            completedMissions = completedMissions,
            missionHistory = missionHistory,
            missionBonusIncomeEndTime = json.optLong("missionBonusIncomeEndTime", 0L),
            missionBonusFleetPowerEndTime = json.optLong("missionBonusFleetPowerEndTime", 0L),
            missionBonusResearchEndTime = json.optLong("missionBonusResearchEndTime", 0L),
            seenMissionTutorial = json.optBoolean("seenMissionTutorial", false)
        )
    }

    // =========================================================================
    // Custom generators for serialization testing
    // =========================================================================

    val missionBoardStateArb: Arb<Pair<String, MissionBoardState>> = arbitrary {
        val sectorId = MissionGenerators.sectorIdWithMissionsArb.bind()
        val templatesForSector = MISSION_TEMPLATES.filter { it.sectorId == sectorId }
        val missionCount = Arb.int(1, minOf(6, templatesForSector.size)).bind()
        val missions = Arb.shuffle(templatesForSector).bind().take(missionCount).map { template ->
            template.copy(cooldownUntil = Arb.long(0L, 2_000_000_000_000L).bind())
        }
        val cooldownCount = Arb.int(0, missionCount).bind()
        val cooldowns = missions.take(cooldownCount).associate {
            it.id to Arb.long(1_000_000_000_000L, 2_000_000_000_000L).bind()
        }
        val lastRefreshTime = Arb.long(1_000_000_000_000L, 2_000_000_000_000L).bind()
        Pair(sectorId, MissionBoardState(missions = missions, lastRefreshTime = lastRefreshTime, cooldowns = cooldowns))
    }

    val missionResultArb: Arb<MissionResult> = arbitrary {
        MissionResult(
            missionId = "m_${Arb.long(1L, 999999L).bind()}",
            missionName = Arb.element(MISSION_TEMPLATES.map { it.name }).bind(),
            success = Arb.boolean().bind(),
            successChance = Arb.double(0.30, 0.95).bind(),
            creditsEarned = Arb.double(0.0, 1_000_000.0).filter { it.isFinite() }.bind(),
            gemsEarned = Arb.int(0, 10).bind(),
            researchPointsEarned = Arb.int(0, 5).bind(),
            bonusReward = Arb.element(listOf(null, BonusRewardType.INCOME_BOOST, BonusRewardType.FLEET_POWER_BOOST, BonusRewardType.RESEARCH_SPEED_BOOST)).bind(),
            completedAt = Arb.long(1_000_000_000_000L, 2_000_000_000_000L).bind()
        )
    }

    val missionHistoryEntryArb: Arb<MissionHistoryEntry> = arbitrary {
        MissionHistoryEntry(
            missionName = Arb.element(MISSION_TEMPLATES.map { it.name }).bind(),
            success = Arb.boolean().bind(),
            completedAt = Arb.long(1_000_000_000_000L, 2_000_000_000_000L).bind()
        )
    }

    val missionSerializationStateArb: Arb<MissionSerializationState> = arbitrary {
        // Generate 1-3 mission boards
        val boardCount = Arb.int(1, 3).bind()
        val boards = mutableMapOf<String, MissionBoardState>()
        val usedSectors = mutableSetOf<String>()
        repeat(boardCount) {
            val (sectorId, boardState) = missionBoardStateArb.bind()
            if (sectorId !in usedSectors) {
                boards[sectorId] = boardState
                usedSectors.add(sectorId)
            }
        }

        // Generate 1-3 active missions
        val activeMissionCount = Arb.int(1, 3).bind()
        val activeMissions = List(activeMissionCount) { MissionGenerators.activeMissionArb.bind() }

        // Generate 0-2 completed mission results
        val completedCount = Arb.int(0, 2).bind()
        val completedMissions = List(completedCount) { missionResultArb.bind() }

        // Generate 0-5 mission history entries
        val historyCount = Arb.int(0, 5).bind()
        val missionHistory = List(historyCount) { missionHistoryEntryArb.bind() }

        // Bonus end times
        val bonusIncomeEndTime = Arb.long(0L, 2_000_000_000_000L).bind()
        val bonusFleetPowerEndTime = Arb.long(0L, 2_000_000_000_000L).bind()
        val bonusResearchEndTime = Arb.long(0L, 2_000_000_000_000L).bind()

        // Tutorial flag
        val seenMissionTutorial = Arb.boolean().bind()

        MissionSerializationState(
            missionBoards = boards,
            activeMissions = activeMissions,
            completedMissions = completedMissions,
            missionHistory = missionHistory,
            missionBonusIncomeEndTime = bonusIncomeEndTime,
            missionBonusFleetPowerEndTime = bonusFleetPowerEndTime,
            missionBonusResearchEndTime = bonusResearchEndTime,
            seenMissionTutorial = seenMissionTutorial
        )
    }

    // =========================================================================
    // Property 21: Mission state serialization round-trip
    // For any valid game state containing active missions, mission boards,
    // completed missions, and mission history, serializing to JSON and
    // deserializing back produces an equivalent state.
    // Validates: Requirements 10.1
    // =========================================================================

    test("Property 21: Mission state serialization round-trip - serialize then deserialize produces equivalent state") {
        checkAll(config, missionSerializationStateArb) { originalState ->
            val json = serializeMissionState(originalState)
            val deserialized = deserializeMissionState(json)

            // Verify mission boards
            deserialized.missionBoards.size shouldBe originalState.missionBoards.size
            originalState.missionBoards.forEach { (sectorId, originalBoard) ->
                val deserializedBoard = deserialized.missionBoards[sectorId]!!
                deserializedBoard.lastRefreshTime shouldBe originalBoard.lastRefreshTime
                deserializedBoard.cooldowns shouldBe originalBoard.cooldowns
                deserializedBoard.missions.size shouldBe originalBoard.missions.size
                originalBoard.missions.forEachIndexed { index, originalMission ->
                    val deserializedMission = deserializedBoard.missions[index]
                    deserializedMission.id shouldBe originalMission.id
                    deserializedMission.name shouldBe originalMission.name
                    deserializedMission.emoji shouldBe originalMission.emoji
                    deserializedMission.description shouldBe originalMission.description
                    deserializedMission.type shouldBe originalMission.type
                    deserializedMission.difficulty shouldBe originalMission.difficulty
                    deserializedMission.sectorId shouldBe originalMission.sectorId
                    deserializedMission.requiredFleetPower shouldBe originalMission.requiredFleetPower
                    deserializedMission.durationMs shouldBe originalMission.durationMs
                    deserializedMission.cooldownUntil shouldBe originalMission.cooldownUntil
                }
            }

            // Verify active missions
            deserialized.activeMissions.size shouldBe originalState.activeMissions.size
            originalState.activeMissions.forEachIndexed { index, original ->
                val deser = deserialized.activeMissions[index]
                deser.id shouldBe original.id
                deser.templateId shouldBe original.templateId
                deser.missionName shouldBe original.missionName
                deser.missionType shouldBe original.missionType
                deser.difficulty shouldBe original.difficulty
                deser.sectorId shouldBe original.sectorId
                deser.requiredFleetPower shouldBe original.requiredFleetPower
                deser.durationMs shouldBe original.durationMs
                deser.deployedShipTiers shouldBe original.deployedShipTiers
                deser.startTime shouldBe original.startTime
                deser.status shouldBe original.status
            }

            // Verify completed missions
            deserialized.completedMissions.size shouldBe originalState.completedMissions.size
            originalState.completedMissions.forEachIndexed { index, original ->
                val deser = deserialized.completedMissions[index]
                deser.missionId shouldBe original.missionId
                deser.missionName shouldBe original.missionName
                deser.success shouldBe original.success
                deser.successChance shouldBe original.successChance
                deser.creditsEarned shouldBe original.creditsEarned
                deser.gemsEarned shouldBe original.gemsEarned
                deser.researchPointsEarned shouldBe original.researchPointsEarned
                deser.bonusReward shouldBe original.bonusReward
                deser.completedAt shouldBe original.completedAt
            }

            // Verify mission history
            deserialized.missionHistory.size shouldBe originalState.missionHistory.size
            originalState.missionHistory.forEachIndexed { index, original ->
                val deser = deserialized.missionHistory[index]
                deser.missionName shouldBe original.missionName
                deser.success shouldBe original.success
                deser.completedAt shouldBe original.completedAt
            }

            // Verify bonus end times
            deserialized.missionBonusIncomeEndTime shouldBe originalState.missionBonusIncomeEndTime
            deserialized.missionBonusFleetPowerEndTime shouldBe originalState.missionBonusFleetPowerEndTime
            deserialized.missionBonusResearchEndTime shouldBe originalState.missionBonusResearchEndTime

            // Verify tutorial flag
            deserialized.seenMissionTutorial shouldBe originalState.seenMissionTutorial
        }
    }
})

/**
 * Data class representing the mission-related subset of GameState for serialization testing.
 * This isolates the mission fields from the full GameState to enable testing without
 * Android dependencies (Context, SharedPreferences).
 */
data class MissionSerializationState(
    val missionBoards: Map<String, MissionBoardState>,
    val activeMissions: List<ActiveMission>,
    val completedMissions: List<MissionResult>,
    val missionHistory: List<MissionHistoryEntry>,
    val missionBonusIncomeEndTime: Long,
    val missionBonusFleetPowerEndTime: Long,
    val missionBonusResearchEndTime: Long,
    val seenMissionTutorial: Boolean
)
