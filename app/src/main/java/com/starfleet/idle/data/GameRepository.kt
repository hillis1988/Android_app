package com.starfleet.idle.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class GameRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("starfleet_idle_save_v4", Context.MODE_PRIVATE)

    fun save(state: GameState) {
        val json = JSONObject().apply {
            put("credits", state.credits)
            put("totalCreditsEarned", state.totalCreditsEarned)
            put("totalTaps", state.totalTaps)
            put("totalCreditsFromTaps", state.totalCreditsFromTaps)
            put("gems", state.gems)
            put("gemBonusIncome", state.gemBonusIncome)
            put("activeSectorId", state.activeSectorId)
            put("researchPoints", state.researchPoints)
            put("researchPointsFraction", state.researchPointsFraction)
            put("starCoins", state.starCoins)
            put("totalPrestigeResets", state.totalPrestigeResets)
            put("dailyLoginStreak", state.dailyLoginStreak)
            put("lastLoginDay", state.lastLoginDay)
            put("dailyRewardsClaimed", state.dailyRewardsClaimed)
            put("adBoostEndTime", state.adBoostEndTime)
            put("speedBoostEndTime", state.speedBoostEndTime)
            put("buyAmount", state.buyAmount.name)
            put("lastTickTime", state.lastTickTime)
            put("lastTapTime", state.lastTapTime)
            put("gameStartTime", state.gameStartTime)

            // Sectors
            val sectorsJson = JSONObject()
            state.sectors.forEach { (sectorId, sectorState) ->
                val sectorJson = JSONObject()
                val shipsJson = JSONObject()
                sectorState.ships.forEach { (tierId, shipState) ->
                    val shipJson = JSONObject().apply {
                        put("count", shipState.count)
                        val upgradesJson = JSONObject()
                        shipState.upgradeLevels.forEach { (uid, lvl) -> upgradesJson.put(uid, lvl) }
                        put("upgrades", upgradesJson)
                    }
                    shipsJson.put(tierId, shipJson)
                }
                sectorJson.put("ships", shipsJson)

                val shopJson = JSONObject()
                sectorState.shopLevels.forEach { (bid, lvl) -> shopJson.put(bid, lvl) }
                sectorJson.put("shop", shopJson)

                sectorsJson.put(sectorId, sectorJson)
            }
            put("sectors", sectorsJson)

            // Perks
            val perksJson = JSONObject()
            state.perkLevels.forEach { (pid, lvl) -> perksJson.put(pid, lvl) }
            put("perks", perksJson)

            // Research
            val researchJson = JSONObject()
            state.researchLevels.forEach { (nid, lvl) -> researchJson.put(nid, lvl) }
            put("research", researchJson)

            // Achievements
            val achieveArray = JSONArray()
            state.unlockedAchievements.forEach { achieveArray.put(it) }
            put("achievements", achieveArray)

            // Quests
            put("questsRefreshedAt", state.questsRefreshedAt)
            put("seenTutorial", state.seenTutorial)
            put("hideWelcomeMessage", state.hideWelcomeMessage)
            val questsArray = JSONArray()
            state.activeQuests.forEach { q ->
                questsArray.put(JSONObject().apply {
                    put("templateId", q.templateId)
                    put("progress", q.progress)
                    put("completed", q.completed)
                    put("claimed", q.claimed)
                })
            }
            put("activeQuests", questsArray)

            // Fleet Missions
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
                    boardState.cooldowns.forEach { (templateId, until) -> cooldownsJson.put(templateId, until) }
                    put("cooldowns", cooldownsJson)
                }
                missionBoardsJson.put(sectorId, boardJson)
            }
            put("missionBoards", missionBoardsJson)

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

            val missionHistoryArray = JSONArray()
            state.missionHistory.forEach { entry ->
                missionHistoryArray.put(JSONObject().apply {
                    put("missionName", entry.missionName)
                    put("success", entry.success)
                    put("completedAt", entry.completedAt)
                })
            }
            put("missionHistory", missionHistoryArray)

            put("missionBonusIncomeEndTime", state.missionBonusIncomeEndTime)
            put("missionBonusFleetPowerEndTime", state.missionBonusFleetPowerEndTime)
            put("missionBonusResearchEndTime", state.missionBonusResearchEndTime)
            put("seenMissionTutorial", state.seenMissionTutorial)
        }

        prefs.edit().putString("game_state", json.toString()).apply()
    }

    fun load(): GameState? {
        val raw = prefs.getString("game_state", null) ?: return null
        return try {
            val json = JSONObject(raw)

            // Sectors
            val sectorsJson = json.optJSONObject("sectors")
            val sectors = mutableMapOf<String, SectorState>()
            SECTORS.forEach { sector ->
                val sectorJson = sectorsJson?.optJSONObject(sector.id)
                if (sectorJson != null) {
                    val shipsJson = sectorJson.getJSONObject("ships")
                    val ships = mutableMapOf<String, ShipState>()
                    SHIP_TIERS.forEach { tier ->
                        if (shipsJson.has(tier.id)) {
                            val shipJson = shipsJson.getJSONObject(tier.id)
                            val upgradesJson = shipJson.getJSONObject("upgrades")
                            val upgradeLevels = mutableMapOf<String, Int>()
                            UPGRADE_TYPES.forEach { u -> upgradeLevels[u.id] = upgradesJson.optInt(u.id, 0) }
                            ships[tier.id] = ShipState(count = shipJson.getInt("count"), upgradeLevels = upgradeLevels)
                        } else {
                            ships[tier.id] = ShipState()
                        }
                    }
                    val shopJson = sectorJson.optJSONObject("shop")
                    val shopLevels = mutableMapOf<String, Int>()
                    SHOP_BONUSES.forEach { b -> shopLevels[b.id] = shopJson?.optInt(b.id, 0) ?: 0 }
                    sectors[sector.id] = SectorState(ships = ships, shopLevels = shopLevels)
                } else {
                    sectors[sector.id] = SectorState()
                }
            }

            // Research
            val researchJson = json.optJSONObject("research")
            val researchLevels = mutableMapOf<String, Int>()
            RESEARCH_NODES.forEach { n -> researchLevels[n.id] = researchJson?.optInt(n.id, 0) ?: 0 }

            // Perks
            val perksJson = json.optJSONObject("perks")
            val perkLevels = mutableMapOf<String, Int>()
            STAR_COIN_PERKS.forEach { p -> perkLevels[p.id] = perksJson?.optInt(p.id, 0) ?: 0 }

            // Achievements
            val achieveArray = json.optJSONArray("achievements")
            val achievements = mutableSetOf<String>()
            if (achieveArray != null) {
                for (i in 0 until achieveArray.length()) {
                    achievements.add(achieveArray.getString(i))
                }
            }

            val buyAmountStr = json.optString("buyAmount", "X1")
            val buyAmount = try { BuyAmount.valueOf(buyAmountStr) } catch (e: Exception) { BuyAmount.X1 }

            // Active quests
            val questsArray = json.optJSONArray("activeQuests")
            val activeQuests = mutableListOf<ActiveQuest>()
            if (questsArray != null) {
                for (i in 0 until questsArray.length()) {
                    val q = questsArray.getJSONObject(i)
                    activeQuests.add(
                        ActiveQuest(
                            templateId = q.getString("templateId"),
                            progress = q.optLong("progress", 0L),
                            completed = q.optBoolean("completed", false),
                            claimed = q.optBoolean("claimed", false)
                        )
                    )
                }
            }

            // Fleet Missions — parse with safe defaults for backward compatibility
            val missionBoards = try {
                val boardsJson = json.optJSONObject("missionBoards")
                val boards = mutableMapOf<String, MissionBoardState>()
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
                        boards[sectorId] = MissionBoardState(
                            missions = missions,
                            lastRefreshTime = boardJson.optLong("lastRefreshTime", 0L),
                            cooldowns = cooldowns
                        )
                    }
                }
                boards
            } catch (e: Exception) {
                emptyMap()
            }

            val activeMissions = try {
                val arr = json.optJSONArray("activeMissions")
                val list = mutableListOf<ActiveMission>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        val tiersArray = m.optJSONArray("deployedShipTiers")
                        val tiers = mutableListOf<String>()
                        if (tiersArray != null) {
                            for (j in 0 until tiersArray.length()) {
                                tiers.add(tiersArray.getString(j))
                            }
                        }
                        list.add(
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
                list
            } catch (e: Exception) {
                emptyList()
            }

            val completedMissions = try {
                val arr = json.optJSONArray("completedMissions")
                val list = mutableListOf<MissionResult>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        val bonusRewardStr = if (m.isNull("bonusReward")) null else m.optString("bonusReward", null)
                        val bonusReward = bonusRewardStr?.let {
                            try { BonusRewardType.valueOf(it) } catch (e: Exception) { null }
                        }
                        list.add(
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
                list
            } catch (e: Exception) {
                emptyList()
            }

            val missionHistory = try {
                val arr = json.optJSONArray("missionHistory")
                val list = mutableListOf<MissionHistoryEntry>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        list.add(
                            MissionHistoryEntry(
                                missionName = m.getString("missionName"),
                                success = m.getBoolean("success"),
                                completedAt = m.getLong("completedAt")
                            )
                        )
                    }
                }
                list
            } catch (e: Exception) {
                emptyList()
            }

            val missionBonusIncomeEndTime = json.optLong("missionBonusIncomeEndTime", 0L)
            val missionBonusFleetPowerEndTime = json.optLong("missionBonusFleetPowerEndTime", 0L)
            val missionBonusResearchEndTime = json.optLong("missionBonusResearchEndTime", 0L)
            val seenMissionTutorial = json.optBoolean("seenMissionTutorial", false)

            GameState(
                credits = json.getDouble("credits"),
                totalCreditsEarned = json.getDouble("totalCreditsEarned"),
                totalTaps = json.optLong("totalTaps", 0L),
                totalCreditsFromTaps = json.optDouble("totalCreditsFromTaps", 0.0),
                gems = json.optInt("gems", 0),
                gemBonusIncome = json.optDouble("gemBonusIncome", 0.0),
                sectors = sectors,
                activeSectorId = json.optString("activeSectorId", "solar"),
                researchLevels = researchLevels,
                perkLevels = perkLevels,
                researchPoints = json.optInt("researchPoints", 0),
                researchPointsFraction = json.optDouble("researchPointsFraction", 0.0),
                starCoins = json.optInt("starCoins", 0),
                totalPrestigeResets = json.optInt("totalPrestigeResets", 0),
                unlockedAchievements = achievements,
                dailyLoginStreak = json.optInt("dailyLoginStreak", 0),
                lastLoginDay = json.optLong("lastLoginDay", 0L),
                dailyRewardsClaimed = json.optInt("dailyRewardsClaimed", 0),
                adBoostEndTime = json.optLong("adBoostEndTime", 0L),
                speedBoostEndTime = json.optLong("speedBoostEndTime", 0L),
                buyAmount = buyAmount,
                activeQuests = activeQuests,
                questsRefreshedAt = json.optLong("questsRefreshedAt", 0L),
                seenTutorial = json.optBoolean("seenTutorial", false),
                hideWelcomeMessage = json.optBoolean("hideWelcomeMessage", false),
                lastTickTime = json.getLong("lastTickTime"),
                lastTapTime = json.optLong("lastTapTime", 0L),
                gameStartTime = json.getLong("gameStartTime"),
                // Fleet Missions
                missionBoards = missionBoards,
                activeMissions = activeMissions,
                completedMissions = completedMissions,
                missionHistory = missionHistory,
                missionBonusIncomeEndTime = missionBonusIncomeEndTime,
                missionBonusFleetPowerEndTime = missionBonusFleetPowerEndTime,
                missionBonusResearchEndTime = missionBonusResearchEndTime,
                seenMissionTutorial = seenMissionTutorial
            )
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
