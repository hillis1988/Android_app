package com.starfleet.idle.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class GameRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("starfleet_idle_save", Context.MODE_PRIVATE)

    fun save(state: GameState) {
        val json = JSONObject().apply {
            put("credits", state.credits)
            put("totalCreditsEarned", state.totalCreditsEarned)
            put("lastTickTime", state.lastTickTime)
            put("gameStartTime", state.gameStartTime)

            val shipsJson = JSONObject()
            state.ships.forEach { (tierId, shipState) ->
                val shipJson = JSONObject().apply {
                    put("count", shipState.count)
                    val upgradesJson = JSONObject()
                    shipState.upgradeLevels.forEach { (upgradeId, level) ->
                        upgradesJson.put(upgradeId, level)
                    }
                    put("upgrades", upgradesJson)
                }
                shipsJson.put(tierId, shipJson)
            }
            put("ships", shipsJson)

            val shopJson = JSONObject()
            state.shopLevels.forEach { (bonusId, level) ->
                shopJson.put(bonusId, level)
            }
            put("shop", shopJson)
        }

        prefs.edit().putString("game_state", json.toString()).apply()
    }

    fun load(): GameState? {
        val raw = prefs.getString("game_state", null) ?: return null
        return try {
            val json = JSONObject(raw)
            val shipsJson = json.getJSONObject("ships")
            val ships = mutableMapOf<String, ShipState>()

            SHIP_TIERS.forEach { tier ->
                if (shipsJson.has(tier.id)) {
                    val shipJson = shipsJson.getJSONObject(tier.id)
                    val upgradesJson = shipJson.getJSONObject("upgrades")
                    val upgradeLevels = mutableMapOf<String, Int>()
                    UPGRADE_TYPES.forEach { upgrade ->
                        upgradeLevels[upgrade.id] = upgradesJson.optInt(upgrade.id, 0)
                    }
                    ships[tier.id] = ShipState(
                        count = shipJson.getInt("count"),
                        upgradeLevels = upgradeLevels
                    )
                } else {
                    ships[tier.id] = ShipState()
                }
            }

            val shopLevels = mutableMapOf<String, Int>()
            val shopJson = json.optJSONObject("shop")
            SHOP_BONUSES.forEach { bonus ->
                shopLevels[bonus.id] = shopJson?.optInt(bonus.id, 0) ?: 0
            }

            GameState(
                credits = json.getDouble("credits"),
                totalCreditsEarned = json.getDouble("totalCreditsEarned"),
                ships = ships,
                shopLevels = shopLevels,
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
