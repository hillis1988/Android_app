package com.starfleet.idle.data

/**
 * Daily quests refresh once every 24 hours and give players short-term goals
 * with meaningful rewards. Three quests per day, picked from the pool below.
 */
data class QuestTemplate(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val targetValue: Long,
    val gemReward: Int,
    val creditMultiplier: Double = 0.0, // multiplier of CPS as bonus credits
    val researchPointReward: Int = 0,
    val type: QuestType
)

enum class QuestType {
    BUY_SHIPS,           // Buy N ships in any sector
    BUY_UPGRADES,        // Buy N upgrades
    EARN_CREDITS,        // Earn N credits this session
    TAP_TIMES,           // Tap the click button N times
    PRESTIGE,            // Prestige once
    UNLOCK_SHIP_TIER,    // Unlock a new ship tier
    SUBMIT_LEADERBOARD,  // Submit any score (auto-completes)
    SPEND_CREDITS        // Spend N credits
}

val QUEST_TEMPLATES = listOf(
    // Easy quests (reward: 5-10 gems)
    QuestTemplate("buy_5", "Quick Build", "Buy 5 ships of any kind", "🚀", 5L, 5, type = QuestType.BUY_SHIPS),
    QuestTemplate("buy_15", "Fleet Expansion", "Buy 15 ships of any kind", "🚀", 15L, 8, type = QuestType.BUY_SHIPS),
    QuestTemplate("upgrade_3", "Maintenance Run", "Buy 3 upgrades", "🔧", 3L, 5, type = QuestType.BUY_UPGRADES),
    QuestTemplate("tap_10", "Hands On", "Tap the bonus button 10 times", "👆", 10L, 5, type = QuestType.TAP_TIMES),

    // Medium quests (reward: 10-20 gems)
    QuestTemplate("buy_50", "Mass Production", "Buy 50 ships of any kind", "🏭", 50L, 15, type = QuestType.BUY_SHIPS),
    QuestTemplate("upgrade_10", "Engineer's Special", "Buy 10 upgrades", "🔧", 10L, 12, type = QuestType.BUY_UPGRADES),
    QuestTemplate("tap_30", "Persistent", "Tap the bonus button 30 times", "👆", 30L, 10, type = QuestType.TAP_TIMES),
    QuestTemplate("research_1", "Lab Work", "Buy 1 research upgrade", "🔬", 1L, 10, researchPointReward = 5, type = QuestType.BUY_UPGRADES),

    // Hard quests (reward: 20-30 gems + RP)
    QuestTemplate("prestige", "Reset for Glory", "Prestige your fleet", "🪙", 1L, 25, researchPointReward = 10, type = QuestType.PRESTIGE),
    QuestTemplate("unlock_tier", "New Vessel", "Unlock a new ship tier", "⚡", 1L, 15, type = QuestType.UNLOCK_SHIP_TIER),
    QuestTemplate("buy_100", "Mass Mobilization", "Buy 100 ships of any kind", "🌟", 100L, 25, type = QuestType.BUY_SHIPS)
)

data class ActiveQuest(
    val templateId: String,
    val progress: Long = 0L,
    val completed: Boolean = false,
    val claimed: Boolean = false
) {
    fun template(): QuestTemplate = QUEST_TEMPLATES.first { it.id == templateId }
}
