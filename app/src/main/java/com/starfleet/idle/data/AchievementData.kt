package com.starfleet.idle.data

data class Achievement(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val gemReward: Int,
    val condition: (GameState) -> Boolean
)

private fun GameState.totalShipCount(): Int =
    sectors.values.sumOf { sector -> sector.ships.values.sumOf { it.count } }

val ACHIEVEMENTS = listOf(
    // Ship count milestones
    Achievement("first_ship", "First Command", "🚀", "Buy your first ship", 5) {
        it.totalShipCount() > 0
    },
    Achievement("fleet_10", "Small Fleet", "🛸", "Own 10 total ships", 10) {
        it.totalShipCount() >= 10
    },
    Achievement("fleet_50", "Growing Armada", "⚡", "Own 50 total ships", 20) {
        it.totalShipCount() >= 50
    },
    Achievement("fleet_200", "Grand Fleet", "💥", "Own 200 total ships", 50) {
        it.totalShipCount() >= 200
    },
    Achievement("fleet_500", "Galactic Armada", "👑", "Own 500 total ships", 100) {
        it.totalShipCount() >= 500
    },

    // Fleet power milestones
    Achievement("power_1k", "Rising Power", "⭐", "Reach 1K fleet power", 10) {
        it.totalFleetPower >= 1_000
    },
    Achievement("power_100k", "Sector Force", "🌟", "Reach 100K fleet power", 25) {
        it.totalFleetPower >= 100_000
    },
    Achievement("power_10m", "Galactic Might", "💫", "Reach 10M fleet power", 50) {
        it.totalFleetPower >= 10_000_000
    },
    Achievement("power_1b", "Universal Power", "🔥", "Reach 1B fleet power", 100) {
        it.totalFleetPower >= 1_000_000_000
    },

    // Income milestones
    Achievement("cps_100", "Steady Income", "💰", "Earn 100 credits/sec", 10) {
        it.creditsPerSecond >= 100
    },
    Achievement("cps_10k", "Profitable", "💎", "Earn 10K credits/sec", 25) {
        it.creditsPerSecond >= 10_000
    },
    Achievement("cps_1m", "Tycoon", "🏆", "Earn 1M credits/sec", 50) {
        it.creditsPerSecond >= 1_000_000
    },
    Achievement("cps_1b", "Mogul", "🌍", "Earn 1B credits/sec", 100) {
        it.creditsPerSecond >= 1_000_000_000
    },

    // Prestige milestones
    Achievement("prestige_1", "First Reset", "🪙", "Prestige for the first time", 15) {
        it.totalPrestigeResets >= 1
    },
    Achievement("prestige_5", "Veteran Commander", "🪙", "Prestige 5 times", 30) {
        it.totalPrestigeResets >= 5
    },
    Achievement("prestige_10", "Seasoned Admiral", "🪙", "Prestige 10 times", 75) {
        it.totalPrestigeResets >= 10
    },

    // Sector milestones
    Achievement("sector_nebula", "Nebula Explorer", "🌌", "Unlock the Orion Nebula", 30) {
        it.unlockedSectorCount >= 2
    },
    Achievement("sector_deep", "Deep Space Pioneer", "🌑", "Unlock Deep Space", 60) {
        it.unlockedSectorCount >= 3
    },
    Achievement("sector_core", "Core Breacher", "💫", "Unlock the Galactic Core", 100) {
        it.unlockedSectorCount >= 4
    },
    Achievement("sector_void", "Void Walker", "🕳️", "Unlock The Void", 200) {
        it.unlockedSectorCount >= 5
    },

    // Special
    Achievement("daily_7", "Dedicated Commander", "📅", "Log in 7 days in a row", 50) {
        it.dailyLoginStreak >= 7
    },
    Achievement("research_first", "Scientist", "🔬", "Complete your first research", 10) {
        it.researchLevels.values.any { l -> l > 0 }
    }
)
