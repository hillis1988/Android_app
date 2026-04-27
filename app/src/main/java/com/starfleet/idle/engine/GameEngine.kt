package com.starfleet.idle.engine

import com.starfleet.idle.data.*

object GameEngine {

    private const val MAX_OFFLINE_HOURS = 12.0

    fun tick(state: GameState, now: Long = System.currentTimeMillis()): GameState {
        val elapsed = (now - state.lastTickTime) / 1000.0
        if (elapsed <= 0) return state.copy(lastTickTime = now)

        val cps = state.creditsPerSecond
        val earned = cps * elapsed

        return state.copy(
            credits = state.credits + earned,
            totalCreditsEarned = state.totalCreditsEarned + earned,
            lastTickTime = now
        )
    }

    fun calculateOfflineEarnings(state: GameState): Pair<GameState, Double> {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - state.lastTickTime) / 1000.0
        val cappedSeconds = minOf(elapsedSeconds, MAX_OFFLINE_HOURS * 3600)
        val cps = state.creditsPerSecond
        val earned = cps * cappedSeconds * state.offlineEfficiency

        val newState = state.copy(
            credits = state.credits + earned,
            totalCreditsEarned = state.totalCreditsEarned + earned,
            lastTickTime = now
        )
        return Pair(newState, earned)
    }

    fun buyShip(state: GameState, tierId: String): GameState {
        if (!state.isShipUnlocked(tierId)) return state
        val cost = state.getShipCost(tierId)
        if (state.credits < cost) return state

        val currentShip = state.ships[tierId] ?: ShipState()
        val updatedShips = state.ships.toMutableMap().apply {
            this[tierId] = currentShip.copy(count = currentShip.count + 1)
        }

        return state.copy(
            credits = state.credits - cost,
            ships = updatedShips
        )
    }

    fun buyUpgrade(state: GameState, tierId: String, upgradeId: String): GameState {
        val currentShip = state.ships[tierId] ?: ShipState()
        if (currentShip.count == 0) return state

        val cost = state.getUpgradeCost(tierId, upgradeId)
        if (state.credits < cost) return state

        val currentLevel = currentShip.upgradeLevels[upgradeId] ?: 0
        val updatedUpgrades = currentShip.upgradeLevels.toMutableMap().apply {
            this[upgradeId] = currentLevel + 1
        }
        val updatedShips = state.ships.toMutableMap().apply {
            this[tierId] = currentShip.copy(upgradeLevels = updatedUpgrades)
        }

        return state.copy(
            credits = state.credits - cost,
            ships = updatedShips
        )
    }

    fun buyShopBonus(state: GameState, bonusId: String): GameState {
        val bonus = SHOP_BONUSES.first { it.id == bonusId }
        val level = state.shopLevels[bonusId] ?: 0
        if (level >= bonus.maxLevel) return state

        val cost = state.getShopBonusCost(bonusId)
        if (state.credits < cost) return state

        val updatedShop = state.shopLevels.toMutableMap().apply {
            this[bonusId] = level + 1
        }

        return state.copy(
            credits = state.credits - cost,
            shopLevels = updatedShop
        )
    }

    fun tap(state: GameState): GameState {
        val earned = state.tapCredits
        if (earned <= 0) return state
        return state.copy(
            credits = state.credits + earned,
            totalCreditsEarned = state.totalCreditsEarned + earned
        )
    }
}
