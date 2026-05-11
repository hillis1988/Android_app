package com.starfleet.idle.data

data class ShipState(
    val count: Int = 0,
    val upgradeLevels: Map<String, Int> = UPGRADE_TYPES.associate { it.id to 0 }
)

data class SectorState(
    val ships: Map<String, ShipState> = SHIP_TIERS.associate { it.id to ShipState() },
    val shopLevels: Map<String, Int> = SHOP_BONUSES.associate { it.id to 0 }
)

data class GameState(
    val credits: Double = 25.0,
    val totalCreditsEarned: Double = 0.0,
    val gems: Int = 0,
    val gemBonusIncome: Double = 0.0,
    val sectors: Map<String, SectorState> = SECTORS.associate { it.id to SectorState() },
    val activeSectorId: String = "solar",
    val researchLevels: Map<String, Int> = RESEARCH_NODES.associate { it.id to 0 },
    val perkLevels: Map<String, Int> = STAR_COIN_PERKS.associate { it.id to 0 },
    val researchPoints: Int = 0,
    val researchPointsFraction: Double = 0.0,
    val starCoins: Int = 0,
    val totalPrestigeResets: Int = 0,
    val unlockedAchievements: Set<String> = emptySet(),
    val dailyLoginStreak: Int = 0,
    val lastLoginDay: Long = 0L,
    val dailyRewardsClaimed: Int = 0,
    val adBoostEndTime: Long = 0L,
    val speedBoostEndTime: Long = 0L,
    val buyAmount: BuyAmount = BuyAmount.X1,
    val lastTickTime: Long = System.currentTimeMillis(),
    val lastTapTime: Long = 0L,
    val totalTaps: Long = 0L,
    val totalCreditsFromTaps: Double = 0.0,
    val gameStartTime: Long = System.currentTimeMillis()
) {
    // --- Active sector helpers ---
    val activeSector: Sector get() = SECTORS.first { it.id == activeSectorId }
    val activeSectorState: SectorState get() = sectors[activeSectorId] ?: SectorState()
    val activeShips: Map<String, ShipState> get() = activeSectorState.ships
    val activeShopLevels: Map<String, Int> get() = activeSectorState.shopLevels

    // --- Research helpers (no dependencies on fleet power) ---
    val researchIncomeMultiplier: Double
        get() {
            var mult = 1.0
            RESEARCH_NODES.filter { it.branch == ResearchBranch.PROPULSION }.forEach { node ->
                val level = researchLevels[node.id] ?: 0
                mult += level * node.effectPerLevel
            }
            return mult
        }

    val researchCostReduction: Double
        get() {
            var reduction = 0.0
            RESEARCH_NODES.filter { it.branch == ResearchBranch.ECONOMICS }.forEach { node ->
                val level = researchLevels[node.id] ?: 0
                reduction += level * node.effectPerLevel
            }
            return Math.pow(1.0 - 0.01, reduction * 100)
        }

    val researchFleetPowerMultiplier: Double
        get() {
            var mult = 1.0
            RESEARCH_NODES.filter { it.branch == ResearchBranch.MILITARY }.forEach { node ->
                val level = researchLevels[node.id] ?: 0
                mult += level * node.effectPerLevel
            }
            return mult
        }

    fun getResearchCost(nodeId: String): Int {
        val node = RESEARCH_NODES.first { it.id == nodeId }
        val level = researchLevels[nodeId] ?: 0
        return (node.baseCost * Math.pow(node.costScaling, level.toDouble())).toInt()
    }

    // --- Fleet power calculations ---
    fun getSectorPower(sectorId: String): Double {
        val sector = SECTORS.find { it.id == sectorId } ?: return 0.0
        val sectorState = sectors[sectorId] ?: SectorState()
        val rawSectorPower = SHIP_TIERS.sumOf { tier ->
            val state = sectorState.ships[tier.id] ?: ShipState()
            val milestoneMulti = getMilestoneMultiplier(state.count)
            state.count * tier.basePower * milestoneMulti
        }
        val shopPowerMult = 1.0 + ((sectorState.shopLevels["shield_array"] ?: 0) * 0.15)
        return rawSectorPower * shopPowerMult * sector.incomeMultiplier * researchFleetPowerMultiplier
    }

    val fleetPower: Double get() = getSectorPower(activeSectorId)

    val totalFleetPower: Double
        get() = SECTORS.sumOf { getSectorPower(it.id) }

    // --- Sector unlock ---
    fun isSectorUnlocked(sectorId: String): Boolean {
        val sector = SECTORS.first { it.id == sectorId }
        val targetShipId = sector.unlockShipId ?: return true
        
        // Find which sector contains targetShipId to check its count
        val targetShip = SHIP_TIERS.find { it.id == targetShipId } ?: return true
        val targetSectorState = sectors[targetShip.sectorId] ?: SectorState()
        val count = targetSectorState.ships[targetShipId]?.count ?: 0
        
        return count >= sector.unlockShipCount
    }

    val unlockedSectorCount: Int
        get() = SECTORS.count { isSectorUnlocked(it.id) }

    // --- Prestige ---
    val prestigeMultiplier: Double get() = getPrestigeMultiplier(starCoins)
    val coinsOnReset: Int get() {
        val baseCoins = calculatePrestigeCoins(totalFleetPower)
        return if (activeSector.specialty == SectorSpecialty.PRESTIGE_BONUS) (baseCoins * 1.15).toInt() else baseCoins
    }

    // --- Ad boost ---
    val isAdBoostActive: Boolean get() = System.currentTimeMillis() < adBoostEndTime
    val adBoostMultiplier: Double get() = if (isAdBoostActive) 2.0 else 1.0
    val adBoostRemainingMs: Long get() = maxOf(0, adBoostEndTime - System.currentTimeMillis())

    val isSpeedBoostActive: Boolean get() = System.currentTimeMillis() < speedBoostEndTime
    val speedMultiplier: Double get() = if (isSpeedBoostActive) 2.0 else 1.0
    val speedBoostRemainingMs: Long get() = maxOf(0, speedBoostEndTime - System.currentTimeMillis())

    // --- Shop bonus helpers (from active sector) ---
    val globalIncomeMultiplier: Double
        get() {
            val level = activeShopLevels["warp_drive"] ?: 0
            return 1.0 + (level * 0.08) // 8% per level (was 20%)
        }

    val costReductionFactor: Double
        get() {
            val level = activeShopLevels["trade_routes"] ?: 0
            val perkBonus = if ((perkLevels["speed_docking"] ?: 0) > 0) 0.90 else 1.0
            return Math.pow(0.98, level.toDouble()) * researchCostReduction * perkBonus // 2% per level (was 3%)
        }

    val offlineEfficiency: Double
        get() {
            val level = activeShopLevels["auto_pilot"] ?: 0
            return 0.4 + (level * 0.05) // 5% per level (was 8%)
        }

    // Tap gives a FLAT amount based on total credits earned (not CPS).
    // This prevents autoclickers from being useful — even at 100 taps/sec,
    // the total is capped and scales very slowly.
    val tapCredits: Double
        get() {
            val level = activeShopLevels["command_bridge"] ?: 0
            if (level == 0) return 0.0
            // Gives 0.1% of CPS per tap, with a hard cap of 2 seconds of income per tap
            // At max level 5: 0.5% of CPS per tap = 0.005 * CPS
            // Even at 20 taps/sec that's only 0.1 * CPS = 10% boost, not game-breaking
            val perTap = creditsPerSecond * 0.001 * level
            val cap = creditsPerSecond * 2.0 // max 2 seconds of income per tap
            return minOf(maxOf(1.0, perTap), cap)
        }

    // --- Research point gen rate (uses totalFleetPower, safe) ---
    val researchPointGenRate: Double
        get() {
            val darkMatterLevel = researchLevels["dark_matter"] ?: 0
            val baseMult = 1.0 + (darkMatterLevel * 0.15)
            val specialtyMult = if (activeSector.specialty == SectorSpecialty.RESEARCH_BOOST) 1.25 else 1.0
            return (totalFleetPower / 10_000.0) * baseMult * specialtyMult / 3600.0
        }

    // --- Income across all sectors (no unlock check — just skip empty sectors) ---
    val creditsPerSecond: Double
        get() {
            var totalIncome = 0.0

            SECTORS.forEach { sector ->
                val sectorState = sectors[sector.id] ?: SectorState()

                val hasShips = sectorState.ships.values.any { it.count > 0 }
                if (!hasShips) return@forEach

                val shopIncomeMult = 1.0 + ((sectorState.shopLevels["warp_drive"] ?: 0) * 0.08)

                val sectorIncome = SHIP_TIERS.sumOf { tier ->
                    val state = sectorState.ships[tier.id] ?: ShipState()
                    if (state.count == 0) return@sumOf 0.0
                    val milestoneMulti = getMilestoneMultiplier(state.count)
                    val baseIncome = tier.baseIncome * state.count * milestoneMulti * sector.incomeMultiplier

                    val upgradeSpecialtyMult = if (sector.specialty == SectorSpecialty.UPGRADE_EFFICIENCY) 1.25 else 1.0
                    val upgradeMultiplier = UPGRADE_TYPES.sumOf { upgrade ->
                        val level = state.upgradeLevels[upgrade.id] ?: 0
                        level * upgrade.incomeBoost * upgradeSpecialtyMult
                    }
                    baseIncome * (1.0 + upgradeMultiplier)
                }

                totalIncome += sectorIncome * shopIncomeMult
            }

            return totalIncome * prestigeMultiplier * researchIncomeMultiplier * adBoostMultiplier * speedMultiplier * (1.0 + gemBonusIncome)
        }

    // --- Ship costs (active sector) ---
    fun getShipCostAt(tierId: String, index: Int): Double {
        val tier = SHIP_TIERS.first { it.id == tierId }
        val state = activeShips[tierId] ?: ShipState()
        val specialtyCostMult = if (activeSector.specialty == SectorSpecialty.FAST_PROGRESS) 0.5 else 1.0
        return tier.baseCost * activeSector.costMultiplier *
            Math.pow(tier.costMultiplier, (state.count + index).toDouble()) * costReductionFactor * specialtyCostMult
    }

    fun getShipCost(tierId: String): Double = getShipCostAt(tierId, 0)

    fun getBulkShipCost(tierId: String, count: Int): Double {
        var total = 0.0
        for (i in 0 until count) { total += getShipCostAt(tierId, i) }
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
        val state = activeShips[tierId] ?: ShipState()
        val level = state.upgradeLevels[upgradeId] ?: 0
        return tier.baseCost * activeSector.costMultiplier *
            upgrade.baseCostMultiplier * Math.pow(upgrade.costScaling, level.toDouble())
    }

    fun getShopBonusCost(bonusId: String): Double {
        val bonus = SHOP_BONUSES.first { it.id == bonusId }
        val level = activeShopLevels[bonusId] ?: 0
        return bonus.baseCost * activeSector.costMultiplier *
            Math.pow(bonus.costScaling, level.toDouble())
    }

    fun isShipUnlocked(tierId: String): Boolean {
        val tier = SHIP_TIERS.first { it.id == tierId }
        if (tier.sectorId != activeSectorId) return false
        
        val targetShipId = tier.unlockShipsId ?: return true
        val count = activeShips[targetShipId]?.count ?: 0
        return count >= tier.unlockShipsCount
    }

    fun canAffordShip(tierId: String): Boolean = credits >= getShipCost(tierId)

    fun canAffordUpgrade(tierId: String, upgradeId: String): Boolean =
        credits >= getUpgradeCost(tierId, upgradeId)

    fun canAffordShopBonus(bonusId: String): Boolean {
        val bonus = SHOP_BONUSES.first { it.id == bonusId }
        val level = activeShopLevels[bonusId] ?: 0
        return level < bonus.maxLevel && credits >= getShopBonusCost(bonusId)
    }

    fun isShopBonusMaxed(bonusId: String): Boolean {
        val bonus = SHOP_BONUSES.first { it.id == bonusId }
        val level = activeShopLevels[bonusId] ?: 0
        return level >= bonus.maxLevel
    }

    val maxOfflineHours: Double
        get() = if ((perkLevels["autopilot_2"] ?: 0) > 0) 24.0 else 12.0

    val isGameComplete: Boolean
        get() = unlockedSectorCount >= SECTORS.size &&
            (sectors["void"]?.ships?.get("dyson")?.count ?: 0) >= 3
}
