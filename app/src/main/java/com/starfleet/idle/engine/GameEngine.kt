package com.starfleet.idle.engine

import com.starfleet.idle.data.*
import java.util.Calendar

object GameEngine {

    private const val AD_BOOST_DURATION_MS = 3_600_000L // 1 hour

    fun tick(state: GameState, now: Long = System.currentTimeMillis()): GameState {
        val elapsed = (now - state.lastTickTime) / 1000.0
        if (elapsed <= 0) return state.copy(lastTickTime = now)

        val cps = state.creditsPerSecond
        val earned = cps * elapsed

        // Research point accumulation
        val rpEarned = state.researchPointGenRate * elapsed
        val newRp = state.researchPoints + rpEarned.toInt()

        return state.copy(
            credits = state.credits + earned,
            totalCreditsEarned = state.totalCreditsEarned + earned,
            researchPoints = if (rpEarned >= 1.0) newRp else state.researchPoints,
            lastTickTime = now
        )
    }

    fun calculateOfflineEarnings(state: GameState): Pair<GameState, Double> {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - state.lastTickTime) / 1000.0
        val cappedSeconds = minOf(elapsedSeconds, state.maxOfflineHours * 3600)
        val cps = state.creditsPerSecond
        val earned = cps * cappedSeconds * state.offlineEfficiency

        val rpEarned = (state.researchPointGenRate * cappedSeconds).toInt()

        val newState = state.copy(
            credits = state.credits + earned,
            totalCreditsEarned = state.totalCreditsEarned + earned,
            researchPoints = state.researchPoints + rpEarned,
            lastTickTime = now
        )
        return Pair(newState, earned)
    }

    fun buyShips(state: GameState, tierId: String, amount: BuyAmount): GameState {
        if (!state.isShipUnlocked(tierId)) return state

        val currentShip = state.activeShips[tierId] ?: ShipState()
        val requested = getBuyCount(amount, currentShip.count)
        val affordable = state.getAffordableCount(tierId, requested)
        if (affordable == 0) return state

        val totalCost = state.getBulkShipCost(tierId, affordable)
        val updatedShips = state.activeShips.toMutableMap().apply {
            this[tierId] = currentShip.copy(count = currentShip.count + affordable)
        }
        val updatedSector = state.activeSectorState.copy(ships = updatedShips)
        val updatedSectors = state.sectors.toMutableMap().apply {
            this[state.activeSectorId] = updatedSector
        }

        return state.copy(credits = state.credits - totalCost, sectors = updatedSectors)
    }

    fun buyUpgrade(state: GameState, tierId: String, upgradeId: String): GameState {
        val currentShip = state.activeShips[tierId] ?: ShipState()
        if (currentShip.count == 0) return state

        val cost = state.getUpgradeCost(tierId, upgradeId)
        if (state.credits < cost) return state

        val currentLevel = currentShip.upgradeLevels[upgradeId] ?: 0
        val updatedUpgrades = currentShip.upgradeLevels.toMutableMap().apply {
            this[upgradeId] = currentLevel + 1
        }
        val updatedShips = state.activeShips.toMutableMap().apply {
            this[tierId] = currentShip.copy(upgradeLevels = updatedUpgrades)
        }
        val updatedSector = state.activeSectorState.copy(ships = updatedShips)
        val updatedSectors = state.sectors.toMutableMap().apply {
            this[state.activeSectorId] = updatedSector
        }

        return state.copy(credits = state.credits - cost, sectors = updatedSectors)
    }

    fun buyShopBonus(state: GameState, bonusId: String): GameState {
        val bonus = SHOP_BONUSES.first { it.id == bonusId }
        val level = state.activeShopLevels[bonusId] ?: 0
        if (level >= bonus.maxLevel) return state

        val cost = state.getShopBonusCost(bonusId)
        if (state.credits < cost) return state

        val updatedShop = state.activeShopLevels.toMutableMap().apply {
            this[bonusId] = level + 1
        }
        val updatedSector = state.activeSectorState.copy(shopLevels = updatedShop)
        val updatedSectors = state.sectors.toMutableMap().apply {
            this[state.activeSectorId] = updatedSector
        }

        return state.copy(credits = state.credits - cost, sectors = updatedSectors)
    }

    fun buyResearch(state: GameState, nodeId: String): GameState {
        val node = RESEARCH_NODES.first { it.id == nodeId }
        val level = state.researchLevels[nodeId] ?: 0
        if (level >= node.maxLevel) return state

        val cost = state.getResearchCost(nodeId)
        if (state.researchPoints < cost) return state

        val updatedResearch = state.researchLevels.toMutableMap().apply {
            this[nodeId] = level + 1
        }

        return state.copy(
            researchPoints = state.researchPoints - cost,
            researchLevels = updatedResearch
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

    fun setBuyAmount(state: GameState, amount: BuyAmount): GameState {
        return state.copy(buyAmount = amount)
    }

    fun switchSector(state: GameState, sectorId: String): GameState {
        if (!state.isSectorUnlocked(sectorId)) return state
        return state.copy(activeSectorId = sectorId)
    }

    fun prestige(state: GameState): GameState {
        val coinsEarned = calculatePrestigeCoins(state.totalFleetPower)
        val activeSector = SECTORS.find { it.id == state.activeSectorId }
        val finalCoins = if (activeSector?.specialty == SectorSpecialty.PRESTIGE_BONUS) (coinsEarned * 1.15).toInt() else coinsEarned
        
        if (finalCoins <= 0) return state

        val now = System.currentTimeMillis()
        return GameState(
            credits = 25.0,
            totalCreditsEarned = 0.0,
            gems = state.gems,
            gemBonusIncome = state.gemBonusIncome,
            sectors = SECTORS.associate { it.id to SectorState() },
            activeSectorId = "solar",
            researchLevels = state.researchLevels,  // persists
            perkLevels = state.perkLevels,          // persists
            researchPoints = state.researchPoints,    // persists
            starCoins = state.starCoins + finalCoins,
            totalPrestigeResets = state.totalPrestigeResets + 1,
            unlockedAchievements = state.unlockedAchievements,
            dailyLoginStreak = state.dailyLoginStreak,
            lastLoginDay = state.lastLoginDay,
            dailyRewardsClaimed = state.dailyRewardsClaimed,
            adBoostEndTime = state.adBoostEndTime,
            buyAmount = state.buyAmount,
            lastTickTime = now,
            gameStartTime = now
        )
    }

    fun buyPerk(state: GameState, perkId: String): GameState {
        val perk = STAR_COIN_PERKS.first { it.id == perkId }
        val level = state.perkLevels[perkId] ?: 0
        if (level >= perk.maxLevel) return state
        if (state.starCoins < perk.cost) return state

        val updatedPerks = state.perkLevels.toMutableMap().apply {
            this[perkId] = level + 1
        }
        return state.copy(
            starCoins = state.starCoins - perk.cost,
            perkLevels = updatedPerks
        )
    }

    // --- Ad boost ---
    fun activateAdBoost(state: GameState): GameState {
        val now = System.currentTimeMillis()
        val newEnd = if (state.isAdBoostActive) {
            // Stack: add 1 hour to existing, cap at 8 hours
            minOf(state.adBoostEndTime + AD_BOOST_DURATION_MS, now + 8 * AD_BOOST_DURATION_MS)
        } else {
            now + AD_BOOST_DURATION_MS
        }
        return state.copy(adBoostEndTime = newEnd)
    }

    fun activateSpeedBoost(state: GameState): GameState {
        val now = System.currentTimeMillis()
        val newEnd = if (state.isSpeedBoostActive) {
            minOf(state.speedBoostEndTime + AD_BOOST_DURATION_MS, now + 8 * AD_BOOST_DURATION_MS)
        } else {
            now + AD_BOOST_DURATION_MS
        }
        return state.copy(speedBoostEndTime = newEnd)
    }

    // --- Daily login ---
    fun checkDailyLogin(state: GameState): GameState {
        val today = getDayNumber()
        if (state.lastLoginDay == today) return state // already logged in today

        val isConsecutive = state.lastLoginDay == today - 1 || state.lastLoginDay == 0L
        val newStreak = if (isConsecutive) state.dailyLoginStreak + 1 else 1

        return state.copy(
            dailyLoginStreak = newStreak,
            lastLoginDay = today
        )
    }

    fun claimDailyReward(state: GameState): GameState {
        val rewardIndex = ((state.dailyLoginStreak - 1) % DAILY_REWARDS.size)
        if (rewardIndex < 0) return state
        val reward = DAILY_REWARDS[rewardIndex]

        if (state.dailyRewardsClaimed >= state.dailyLoginStreak) return state // already claimed

        return state.copy(
            credits = state.credits + reward.credits,
            gems = state.gems + reward.gems,
            researchPoints = state.researchPoints + reward.researchPoints,
            dailyRewardsClaimed = state.dailyRewardsClaimed + 1
        )
    }

    // --- Gem store ---
    fun buyGemItem(state: GameState, itemId: String): GameState {
        val item = GEM_STORE_ITEMS.first { it.id == itemId }
        if (state.gems < item.gemCost) return state

        var newState = state.copy(gems = state.gems - item.gemCost)

        when (item.type) {
            GemItemType.TIME_WARP_1H -> {
                val earned = newState.creditsPerSecond * 3600
                newState = newState.copy(
                    credits = newState.credits + earned,
                    totalCreditsEarned = newState.totalCreditsEarned + earned
                )
            }
            GemItemType.TIME_WARP_4H -> {
                val earned = newState.creditsPerSecond * 3600 * 4
                newState = newState.copy(
                    credits = newState.credits + earned,
                    totalCreditsEarned = newState.totalCreditsEarned + earned
                )
            }
            GemItemType.TIME_WARP_8H -> {
                val earned = newState.creditsPerSecond * 3600 * 8
                newState = newState.copy(
                    credits = newState.credits + earned,
                    totalCreditsEarned = newState.totalCreditsEarned + earned
                )
            }
            GemItemType.PERMANENT_INCOME_5 -> {
                newState = newState.copy(gemBonusIncome = newState.gemBonusIncome + 0.05)
            }
            GemItemType.PERMANENT_INCOME_15 -> {
                newState = newState.copy(gemBonusIncome = newState.gemBonusIncome + 0.15)
            }
            GemItemType.RESEARCH_POINTS_5 -> {
                newState = newState.copy(researchPoints = newState.researchPoints + 5)
            }
            GemItemType.RESEARCH_POINTS_20 -> {
                newState = newState.copy(researchPoints = newState.researchPoints + 20)
            }
        }

        return newState
    }

    // --- Gem IAP packs ---
    fun purchaseGemPack(state: GameState, packId: String): GameState {
        val pack = GEM_PACKS.first { it.id == packId }
        val totalGems = pack.gems + pack.bonusGems
        return state.copy(gems = state.gems + totalGems)
    }

    // --- Double offline earnings (ad reward) ---
    fun doubleOfflineEarnings(state: GameState, earnings: Double): GameState {
        return state.copy(
            credits = state.credits + earnings, // add the same amount again
            totalCreditsEarned = state.totalCreditsEarned + earnings
        )
    }

    // --- Achievements ---
    fun checkAchievements(state: GameState): GameState {
        val newUnlocked = mutableSetOf<String>()
        var gemsEarned = 0

        ACHIEVEMENTS.forEach { achievement ->
            if (achievement.id !in state.unlockedAchievements && achievement.condition(state)) {
                newUnlocked.add(achievement.id)
                var reward = achievement.gemReward
                if ((state.perkLevels["gem_finder"] ?: 0) > 0 && Math.random() < 0.10) {
                    reward = (reward * 1.5).toInt()
                }
                gemsEarned += reward
            }
        }

        if (newUnlocked.isEmpty()) return state

        return state.copy(
            unlockedAchievements = state.unlockedAchievements + newUnlocked,
            gems = state.gems + gemsEarned
        )
    }

    fun resolveEncounter(state: GameState, encounter: EncounterData, optionIndex: Int): GameState {
        return when (encounter.type) {
            "asteroid" -> {
                if (optionIndex == 0) {
                    val gain = state.creditsPerSecond * 300 // 5 mins of income
                    state.copy(credits = state.credits + gain, totalCreditsEarned = state.totalCreditsEarned + gain)
                } else {
                    state.copy(gems = state.gems + 15)
                }
            }
            "trader" -> {
                if (optionIndex == 0) {
                    val cost = state.creditsPerSecond * 600
                    if (state.credits >= cost) {
                        state.copy(credits = state.credits - cost, researchPoints = state.researchPoints + 10)
                    } else state
                } else {
                    if (state.gems >= 20) {
                        state.copy(gems = state.gems - 20, researchPoints = state.researchPoints + 25)
                    } else state
                }
            }
            "anomaly" -> {
                if (optionIndex == 0) {
                    activateAdBoost(state)
                } else {
                    state.copy(starCoins = state.starCoins + 5)
                }
            }
            else -> state
        }
    }

    private fun getDayNumber(): Long {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR)
    }
}
