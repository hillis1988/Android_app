package com.starfleet.idle.data

data class ShipTier(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val baseCost: Double,
    val baseIncome: Double,       // credits per second per ship
    val basePower: Double,        // fleet power per ship
    val costMultiplier: Double = 1.15,
    val unlockPower: Double = 0.0 // fleet power needed to unlock
)

val SHIP_TIERS = listOf(
    ShipTier(
        id = "scout",
        name = "Scout Shuttle",
        emoji = "🛸",
        description = "Light recon vessel. Cheap and reliable.",
        baseCost = 10.0,
        baseIncome = 0.5,
        basePower = 1.0,
        unlockPower = 0.0
    ),
    ShipTier(
        id = "frigate",
        name = "Cargo Frigate",
        emoji = "🚀",
        description = "Hauler with decent cargo capacity.",
        baseCost = 120.0,
        baseIncome = 4.0,
        basePower = 5.0,
        unlockPower = 10.0
    ),
    ShipTier(
        id = "cruiser",
        name = "Mining Cruiser",
        emoji = "⛏️",
        description = "Mid-range vessel built for asteroid mining.",
        baseCost = 1_400.0,
        baseIncome = 30.0,
        basePower = 25.0,
        unlockPower = 100.0
    ),
    ShipTier(
        id = "destroyer",
        name = "Battle Destroyer",
        emoji = "💥",
        description = "Heavy warship. Earns bounties from patrols.",
        baseCost = 20_000.0,
        baseIncome = 200.0,
        basePower = 150.0,
        unlockPower = 800.0
    ),
    ShipTier(
        id = "flagship",
        name = "Carrier Flagship",
        emoji = "🛡️",
        description = "Command vessel. Boosts entire fleet operations.",
        baseCost = 500_000.0,
        baseIncome = 1_800.0,
        basePower = 1_000.0,
        unlockPower = 5_000.0
    ),
    ShipTier(
        id = "dreadnought",
        name = "Dreadnought",
        emoji = "👑",
        description = "Ultimate capital ship. Galaxy-class power.",
        baseCost = 20_000_000.0,
        baseIncome = 20_000.0,
        basePower = 10_000.0,
        unlockPower = 50_000.0
    )
)

data class UpgradeType(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val baseCostMultiplier: Double, // multiplied by ship base cost
    val incomeBoost: Double,        // percentage boost per level (0.1 = 10%)
    val costScaling: Double = 1.4
)

val UPGRADE_TYPES = listOf(
    UpgradeType(
        id = "engine",
        name = "Engine",
        emoji = "🔥",
        description = "Faster operations, more income",
        baseCostMultiplier = 2.0,
        incomeBoost = 0.15,
        costScaling = 1.4
    ),
    UpgradeType(
        id = "hull",
        name = "Hull",
        emoji = "🛡️",
        description = "Tougher hull, income multiplier",
        baseCostMultiplier = 3.0,
        incomeBoost = 0.25,
        costScaling = 1.6
    )
)

// --- Milestone thresholds: each doubles output ---
val MILESTONE_THRESHOLDS = listOf(10, 25, 50)
// After 50, every multiple of 100 also doubles (100, 200, 300...)

fun getMilestoneMultiplier(count: Int): Double {
    var doublings = 0
    for (threshold in MILESTONE_THRESHOLDS) {
        if (count >= threshold) doublings++
    }
    if (count >= 100) {
        doublings += count / 100
    }
    return Math.pow(2.0, doublings.toDouble())
}

fun getNextMilestone(count: Int): Int? {
    for (threshold in MILESTONE_THRESHOLDS) {
        if (count < threshold) return threshold
    }
    // Next multiple of 100
    val next = ((count / 100) + 1) * 100
    return next
}

// --- Shop Bonuses ---
data class ShopBonus(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val baseCost: Double,
    val costScaling: Double = 1.5,
    val maxLevel: Int = 10,
    val type: BonusType
)

enum class BonusType {
    GLOBAL_INCOME_MULT,    // multiplies all income
    FLEET_POWER_MULT,      // multiplies fleet power
    COST_REDUCTION,        // reduces ship purchase costs
    OFFLINE_BOOST,         // increases offline earning rate
    CLICK_BONUS            // credits per manual tap
}

val SHOP_BONUSES = listOf(
    ShopBonus(
        id = "warp_drive",
        name = "Warp Drive Tech",
        emoji = "🌀",
        description = "All ships earn 25% more per level",
        baseCost = 500.0,
        costScaling = 2.5,
        maxLevel = 10,
        type = BonusType.GLOBAL_INCOME_MULT
    ),
    ShopBonus(
        id = "shield_array",
        name = "Shield Array",
        emoji = "🔰",
        description = "Fleet Power boosted by 20% per level",
        baseCost = 800.0,
        costScaling = 2.8,
        maxLevel = 10,
        type = BonusType.FLEET_POWER_MULT
    ),
    ShopBonus(
        id = "trade_routes",
        name = "Trade Routes",
        emoji = "🗺️",
        description = "Ship costs reduced by 5% per level",
        baseCost = 1_000.0,
        costScaling = 3.0,
        maxLevel = 10,
        type = BonusType.COST_REDUCTION
    ),
    ShopBonus(
        id = "auto_pilot",
        name = "Auto-Pilot AI",
        emoji = "🤖",
        description = "Offline earnings +10% efficiency per level",
        baseCost = 2_000.0,
        costScaling = 2.2,
        maxLevel = 10,
        type = BonusType.OFFLINE_BOOST
    ),
    ShopBonus(
        id = "command_bridge",
        name = "Command Bridge",
        emoji = "🎯",
        description = "Tap to earn credits (more per level)",
        baseCost = 300.0,
        costScaling = 2.0,
        maxLevel = 15,
        type = BonusType.CLICK_BONUS
    )
)
