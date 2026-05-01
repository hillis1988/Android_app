package com.starfleet.idle.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class GameRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("starfleet_idle_save_v2", Context.MODE_PRIVATE)

    fun save(state: GameState) {
        val json = JSONObject().apply {
            put("credits", state.credits)
            put("totalCreditsEarned", state.totalCreditsEarned)
            put("gems", state.gems)
            put("gemBonusIncome", state.gemBonusIncome)
            put("activeSectorId", state.activeSectorId)
            put("researchPoints", state.researchPoints)
            put("starCoins", state.starCoins)
            put("totalPrestigeResets", state.totalPrestigeResets)
            put("dailyLoginStreak", state.dailyLoginStreak)
            put("lastLoginDay", state.lastLoginDay)
            put("dailyRewardsClaimed", state.dailyRewardsClaimed)
            put("adBoostEndTime", state.adBoostEndTime)
            put("speedBoostEndTime", state.speedBoostEndTime)
            put("buyAmount", state.buyAmount.name)
            put("lastTickTime", state.lastTickTime)
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

            GameState(
                credits = json.getDouble("credits"),
                totalCreditsEarned = json.getDouble("totalCreditsEarned"),
                gems = json.optInt("gems", 0),
                gemBonusIncome = json.optDouble("gemBonusIncome", 0.0),
                sectors = sectors,
                activeSectorId = json.optString("activeSectorId", "solar"),
                researchLevels = researchLevels,
                perkLevels = perkLevels,
                researchPoints = json.optInt("researchPoints", 0),
                starCoins = json.optInt("starCoins", 0),
                totalPrestigeResets = json.optInt("totalPrestigeResets", 0),
                unlockedAchievements = achievements,
                dailyLoginStreak = json.optInt("dailyLoginStreak", 0),
                lastLoginDay = json.optLong("lastLoginDay", 0L),
                dailyRewardsClaimed = json.optInt("dailyRewardsClaimed", 0),
                adBoostEndTime = json.optLong("adBoostEndTime", 0L),
                speedBoostEndTime = json.optLong("speedBoostEndTime", 0L),
                buyAmount = buyAmount,
                lastTickTime = json.getLong("lastTickTime"),
                gameStartTime = json.getLong("gameStartTime")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
