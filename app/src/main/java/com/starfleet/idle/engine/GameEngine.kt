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

        // Research point accumulation with fractional tracking
        val rpEarned = state.researchPointGenRate * elapsed
        val totalFraction = state.researchPointsFraction + rpEarned
        val wholeRp = totalFraction.toInt()
        val remainingFraction = totalFraction - wholeRp

        return state.copy(
            credits = state.credits + earned,
            totalCreditsEarned = state.totalCreditsEarned + earned,
            researchPoints = state.researchPoints + wholeRp,
            researchPointsFraction = remainingFraction,
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

        var newState = state.copy(credits = state.credits - totalCost, sectors = updatedSectors)
        newState = progressQuests(newState, QuestType.BUY_SHIPS, affordable.toLong())
        newState = progressQuests(newState, QuestType.SPEND_CREDITS, totalCost.toLong())
        // First time buying a ship of this tier counts toward "Unlock New Tier" quest
        if (currentShip.count == 0) {
            newState = progressQuests(newState, QuestType.UNLOCK_SHIP_TIER, 1L)
        }
        return newState
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

        var newState = state.copy(credits = state.credits - cost, sectors = updatedSectors)
        newState = progressQuests(newState, QuestType.BUY_UPGRADES, 1L)
        return newState
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

    private const val TAP_COOLDOWN_MS = 2000L
    private const val TAP_CRIT_CHANCE = 0.10
    private const val TAP_CRIT_MULTIPLIER = 5.0

    fun tap(state: GameState): GameState {
        val now = System.currentTimeMillis()
        if (now - state.lastTapTime < TAP_COOLDOWN_MS) return state

        val baseEarned = state.tapCredits
        if (baseEarned <= 0) return state

        val isCrit = Math.random() < TAP_CRIT_CHANCE
        val earned = if (isCrit) baseEarned * TAP_CRIT_MULTIPLIER else baseEarned

        var newState = state.copy(
            credits = state.credits + earned,
            totalCreditsEarned = state.totalCreditsEarned + earned,
            totalTaps = state.totalTaps + 1,
            totalCreditsFromTaps = state.totalCreditsFromTaps + earned,
            lastTapTime = now
        )
        newState = progressQuests(newState, QuestType.TAP_TIMES, 1L)
        return newState
    }

    /** Used by UI to know whether a tap was a critical hit (for visual feedback). */
    fun wasLastTapCritical(state: GameState): Boolean {
        // Reconstructed by checking if last tap earned > base value
        // (UI uses this to flash a "CRITICAL!" message)
        return false // simple version: UI tracks its own crit flag
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
        var prestiged = GameState(
            credits = 25.0,
            totalCreditsEarned = 0.0,
            gems = state.gems,
            gemBonusIncome = state.gemBonusIncome,
            sectors = SECTORS.associate { it.id to SectorState() },
            activeSectorId = "solar",
            researchLevels = state.researchLevels,  // persists
            perkLevels = state.perkLevels,          // persists
            researchPoints = state.researchPoints,    // persists
            researchPointsFraction = state.researchPointsFraction, // persists
            starCoins = state.starCoins + finalCoins,
            totalPrestigeResets = state.totalPrestigeResets + 1,
            unlockedAchievements = state.unlockedAchievements,
            dailyLoginStreak = state.dailyLoginStreak,
            lastLoginDay = state.lastLoginDay,
            dailyRewardsClaimed = state.dailyRewardsClaimed,
            adBoostEndTime = state.adBoostEndTime,
            speedBoostEndTime = state.speedBoostEndTime,
            buyAmount = state.buyAmount,
            totalTaps = state.totalTaps,
            totalCreditsFromTaps = state.totalCreditsFromTaps,
            activeQuests = state.activeQuests,
            questsRefreshedAt = state.questsRefreshedAt,
            seenTutorial = state.seenTutorial,
            lastTickTime = now,
            gameStartTime = now
        )
        prestiged = progressQuests(prestiged, QuestType.PRESTIGE, 1L)
        return prestiged
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
                val seconds = 3600.0
                val earned = newState.creditsPerSecond * seconds
                val rpEarned = (newState.researchPointGenRate * seconds).toInt()
                newState = newState.copy(
                    credits = newState.credits + earned,
                    totalCreditsEarned = newState.totalCreditsEarned + earned,
                    researchPoints = newState.researchPoints + rpEarned
                )
            }
            GemItemType.TIME_WARP_4H -> {
                val seconds = 3600.0 * 4
                val earned = newState.creditsPerSecond * seconds
                val rpEarned = (newState.researchPointGenRate * seconds).toInt()
                newState = newState.copy(
                    credits = newState.credits + earned,
                    totalCreditsEarned = newState.totalCreditsEarned + earned,
                    researchPoints = newState.researchPoints + rpEarned
                )
            }
            GemItemType.TIME_WARP_8H -> {
                val seconds = 3600.0 * 8
                val earned = newState.creditsPerSecond * seconds
                val rpEarned = (newState.researchPointGenRate * seconds).toInt()
                newState = newState.copy(
                    credits = newState.credits + earned,
                    totalCreditsEarned = newState.totalCreditsEarned + earned,
                    researchPoints = newState.researchPoints + rpEarned
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
    // In dev mode or after Google Play confirms a purchase, this grants the gems.
    fun grantGemPack(state: GameState, packId: String): GameState {
        val pack = GEM_PACKS.firstOrNull { it.id == packId } ?: return state
        val totalGems = pack.gems + pack.bonusGems
        return state.copy(gems = state.gems + totalGems)
    }

    // Kept for backward compatibility
    @Deprecated("Use grantGemPack instead", ReplaceWith("grantGemPack(state, packId)"))
    fun purchaseGemPack(state: GameState, packId: String): GameState = grantGemPack(state, packId)

    /** Called when a developer tip purchase is verified. Gives the gem bonus. */
    fun grantTip(state: GameState, tipId: String): GameState {
        val tip = DEVELOPER_TIPS.firstOrNull { it.id == tipId } ?: return state
        return state.copy(gems = state.gems + tip.gemBonus)
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

    // --- Random Encounters ---
    fun handleEncounter(state: GameState, encounter: EncounterData, optionIndex: Int): GameState {
        return when (encounter.type) {
            "asteroid" -> {
                if (optionIndex == 0) {
                    // Mine for credits: 30 minutes of income
                    val earned = state.creditsPerSecond * 1800
                    state.copy(credits = state.credits + earned, totalCreditsEarned = state.totalCreditsEarned + earned)
                } else {
                    // Extract gems
                    state.copy(gems = state.gems + 5)
                }
            }
            "trader" -> {
                if (optionIndex == 0) {
                    // Pay credits for RP
                    val cost = state.creditsPerSecond * 600
                    if (state.credits >= cost) {
                        state.copy(credits = state.credits - cost, researchPoints = state.researchPoints + 3)
                    } else state
                } else {
                    // Trade gems for RP
                    if (state.gems >= 5) {
                        state.copy(gems = state.gems - 5, researchPoints = state.researchPoints + 8)
                    } else state
                }
            }
            "anomaly" -> {
                if (optionIndex == 0) {
                    // Study: 1 hour of income
                    val earned = state.creditsPerSecond * 3600
                    state.copy(credits = state.credits + earned, totalCreditsEarned = state.totalCreditsEarned + earned)
                } else {
                    // Stabilize: star coins
                    val coins = maxOf(1, state.starCoins / 10)
                    state.copy(starCoins = state.starCoins + coins)
                }
            }
            else -> state
        }
    }

    private fun getDayNumber(): Long {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR)
    }

    // --- Daily Quests ---
    private const val QUEST_REFRESH_MS = 86_400_000L // 24 hours

    /** Refreshes daily quests if 24 hours have passed since last refresh. */
    fun refreshDailyQuestsIfNeeded(state: GameState): GameState {
        val now = System.currentTimeMillis()
        if (state.questsRefreshedAt > 0 && (now - state.questsRefreshedAt) < QUEST_REFRESH_MS) {
            return state
        }
        // Pick 3 random quests, weighted toward easier ones for new players
        val pool = QUEST_TEMPLATES.toMutableList().apply { shuffle() }
        val selected = pool.take(3).map { ActiveQuest(templateId = it.id) }
        return state.copy(activeQuests = selected, questsRefreshedAt = now)
    }

    /** Increments quest progress by [amount] for all quests of [type]. */
    fun progressQuests(state: GameState, type: QuestType, amount: Long = 1L): GameState {
        if (state.activeQuests.isEmpty()) return state
        val updated = state.activeQuests.map { q ->
            val template = q.template()
            if (template.type == type && !q.completed) {
                val newProgress = q.progress + amount
                val isDone = newProgress >= template.targetValue
                q.copy(progress = minOf(newProgress, template.targetValue), completed = isDone)
            } else q
        }
        return state.copy(activeQuests = updated)
    }

    fun claimQuestReward(state: GameState, questIndex: Int): GameState {
        if (questIndex !in state.activeQuests.indices) return state
        val quest = state.activeQuests[questIndex]
        if (!quest.completed || quest.claimed) return state
        val template = quest.template()

        val cps = state.creditsPerSecond
        val creditBonus = cps * template.creditMultiplier * 600 // bonus = N seconds of CPS

        val updatedQuests = state.activeQuests.toMutableList().apply {
            this[questIndex] = quest.copy(claimed = true)
        }

        return state.copy(
            gems = state.gems + template.gemReward,
            researchPoints = state.researchPoints + template.researchPointReward,
            credits = state.credits + creditBonus,
            totalCreditsEarned = state.totalCreditsEarned + creditBonus,
            activeQuests = updatedQuests
        )
    }

    // --- Random Encounter Triggering ---
    fun maybeTriggerEncounter(state: GameState): EncounterData? {
        // Encounters only happen if the player has meaningful CPS (> 100/s)
        if (state.creditsPerSecond < 100) return null
        // 1% chance per check (checks every minute = ~once an hour)
        if (Math.random() > 0.01) return null

        val type = listOf("asteroid", "trader", "anomaly").random()
        return EncounterData(id = System.currentTimeMillis().toString(), type = type)
    }

    fun completeTutorial(state: GameState): GameState = state.copy(seenTutorial = true)
}
