package com.starfleet.idle.data

data class ShipState(
    val count: Int = 0,
    val upgradeLevels: Map<String, Int> = UPGRADE_TYPES.associate { it.id to 0 }
)

data class GameState(
    val credits: Double = 25.0,
    val totalCreditsEarned: Double = 0.0,
    val ships: Map<String, ShipState> = SHIP_TIERS.associate { it.id to ShipState() },
    val shopLevels: Map<String, Int> = SHOP_BONUSES.associate { it.id to 0 },
    val starCoins: Int = 0,
    val totalPrestigeResets: Int = 0,
    val buyAmount: BuyAmount = BuyAmount.X1,
    val lastTickTime: Long = System.currentTimeMillis(),
    val gameStartTime: Long = System.currentTimeMillis()
) {
    // --- Prestige ---
    val prestigeMultiplier: Double
        get() = getPrestigeMultiplier(starCoins)

    val coinsOnReset: Int
        get() = calculatePrestigeCoins(fleetPower)

    // --- Shop bonus helpers ---
    val globalIncomeMultiplier: Double
        get() {
            val level = shopLevels["warp_drive"] ?: 0
            return 1.0 + (level * 0.20)
        }

    val fleetPowerMultiplier: Double
        get() {
            val level = shopLevels["shield_array"] ?: 0
            return 1.0 + (level * 0.15)
        }

    val costReductionFactor: Double
        get() {
            val level = shopLevels["trade_routes"] ?: 0
            return Math.pow(0.97, level.toDouble())
        }

    val offlineEfficiency: Double
        get() {
            val level = shopLevels["auto_pilot"] ?: 0
            return 0.4 + (level * 0.08)
        }

    val tapCredits: Double
        get() {
            val level = shopLevels["command_bridge"] ?: 0
            if (level == 0) return 0.0
            return maxOf(1.0, creditsPerSecond * 0.3 * level)
        }

    // --- Fleet power (no prestige multiplier — prestige only boosts income) ---
    val fleetPower: Double
        get() = SHIP_TIERS.sumOf { tier ->
            val state = ships[tier.id] ?: ShipState()
            val milestoneMulti = getMilestoneMultiplier(state.count)
            state.count * tier.basePower * milestoneMulti
        } * fleetPowerMultiplier

    // --- Income with milestone + upgrade + shop + prestige multipliers ---
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
            return rawIncome * globalIncomeMultiplier * prestigeMultiplier
        }

    fun getShipCostAt(tierId: String, index: Int): Double {
        val tier = SHIP_TIERS.first { it.id == tierId }
        val state = ships[tierId] ?: ShipState()
        return tier.baseCost * Math.pow(tier.costMultiplier, (state.count + index).toDouble()) * costReductionFactor
    }

    fun getShipCost(tierId: String): Double = getShipCostAt(tierId, 0)

    fun getBulkShipCost(tierId: String, count: Int): Double {
        var total = 0.0
        for (i in 0 until count) {
            total += getShipCostAt(tierId, i)
        }
        return total
    }

    fun getAffordableCount(tierId: String, requested: Int): Int {
        var total = 0.0
        for (i in 0 until requested) {
            total += getShipCostAt(tierId, i)
            if (total > credits) return i
        }
        return requested
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
            val dysonCount = ships["dyson"]?.count ?: 0
            return dysonCount >= 5 && fleetPower >= 10_000_000_000.0
        }
}
