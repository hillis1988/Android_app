package com.starfleet.idle.data

data class ShipState(
    val count: Int = 0,
    val upgradeLevels: Map<String, Int> = UPGRADE_TYPES.associate { it.id to 0 }
)

data class GameState(
    val credits: Double = 50.0,
    val totalCreditsEarned: Double = 0.0,
    val ships: Map<String, ShipState> = SHIP_TIERS.associate { it.id to ShipState() },
    val shopLevels: Map<String, Int> = SHOP_BONUSES.associate { it.id to 0 },
    val lastTickTime: Long = System.currentTimeMillis(),
    val gameStartTime: Long = System.currentTimeMillis()
) {
    // --- Shop bonus helpers ---
    val globalIncomeMultiplier: Double
        get() {
            val level = shopLevels["warp_drive"] ?: 0
            return 1.0 + (level * 0.25)
        }

    val fleetPowerMultiplier: Double
        get() {
            val level = shopLevels["shield_array"] ?: 0
            return 1.0 + (level * 0.20)
        }

    val costReductionFactor: Double
        get() {
            val level = shopLevels["trade_routes"] ?: 0
            return Math.pow(0.95, level.toDouble()) // 5% cheaper per level, compounds
        }

    val offlineEfficiency: Double
        get() {
            val level = shopLevels["auto_pilot"] ?: 0
            return 0.5 + (level * 0.10) // base 50% + 10% per level
        }

    val tapCredits: Double
        get() {
            val level = shopLevels["command_bridge"] ?: 0
            if (level == 0) return 0.0
            // Tap gives 1 second worth of income * level, minimum 1 credit
            return maxOf(1.0, creditsPerSecond * 0.5 * level)
        }

    // --- Fleet power with milestone + shop multiplier ---
    val fleetPower: Double
        get() = SHIP_TIERS.sumOf { tier ->
            val state = ships[tier.id] ?: ShipState()
            val milestoneMulti = getMilestoneMultiplier(state.count)
            state.count * tier.basePower * milestoneMulti
        } * fleetPowerMultiplier

    // --- Income with milestone + upgrade + shop multipliers ---
    val creditsPerSecond: Double
        get() {
            val rawIncome = SHIP_TIERS.sumOf { tier ->
                val state = ships[tier.id] ?: ShipState()
                if (state.count == 0) return@sumOf 0.0
                val milestoneMulti = getMilestoneMultiplier(state.count)
                val baseIncome = tier.baseIncome * state.count * milestoneMulti
                val upgradeMultiplier = UPGRADE_TYPES.sumOf { upgrade ->
                    val level = state.upgradeLevels[upgrade.id] ?: 0
                    level * upgrade.incomeBoost
                }
                baseIncome * (1.0 + upgradeMultiplier)
            }
            return rawIncome * globalIncomeMultiplier
        }

    fun getShipCost(tierId: String): Double {
        val tier = SHIP_TIERS.first { it.id == tierId }
        val state = ships[tierId] ?: ShipState()
        return tier.baseCost * Math.pow(tier.costMultiplier, state.count.toDouble()) * costReductionFactor
    }

    fun getUpgradeCost(tierId: String, upgradeId: String): Double {
        val tier = SHIP_TIERS.first { it.id == tierId }
        val upgrade = UPGRADE_TYPES.first { it.id == upgradeId }
        val state = ships[tierId] ?: ShipState()
        val level = state.upgradeLevels[upgradeId] ?: 0
        return tier.baseCost * upgrade.baseCostMultiplier * Math.pow(upgrade.costScaling, level.toDouble())
    }

    fun getShopBonusCost(bonusId: String): Double {
        val bonus = SHOP_BONUSES.first { it.id == bonusId }
        val level = shopLevels[bonusId] ?: 0
        return bonus.baseCost * Math.pow(bonus.costScaling, level.toDouble())
    }

    fun isShipUnlocked(tierId: String): Boolean {
        val tier = SHIP_TIERS.first { it.id == tierId }
        return fleetPower >= tier.unlockPower
    }

    fun canAffordShip(tierId: String): Boolean = credits >= getShipCost(tierId)

    fun canAffordUpgrade(tierId: String, upgradeId: String): Boolean =
        credits >= getUpgradeCost(tierId, upgradeId)

    fun canAffordShopBonus(bonusId: String): Boolean {
        val bonus = SHOP_BONUSES.first { it.id == bonusId }
        val level = shopLevels[bonusId] ?: 0
        return level < bonus.maxLevel && credits >= getShopBonusCost(bonusId)
    }

    fun isShopBonusMaxed(bonusId: String): Boolean {
        val bonus = SHOP_BONUSES.first { it.id == bonusId }
        val level = shopLevels[bonusId] ?: 0
        return level >= bonus.maxLevel
    }

    val isGameComplete: Boolean
        get() {
            val dreadnoughts = ships["dreadnought"]?.count ?: 0
            return dreadnoughts >= 10 && fleetPower >= 200_000
        }
}
