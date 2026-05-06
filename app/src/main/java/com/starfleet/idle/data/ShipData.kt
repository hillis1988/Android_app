package com.starfleet.idle.data

data class ShipTier(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val baseCost: Double,
    val baseIncome: Double,       // credits per second per ship
    val basePower: Double,        // fleet power per ship
    val costMultiplier: Double = 1.17,
    val unlockPower: Double = 0.0 // fleet power needed to unlock in active sector
)

val SHIP_TIERS = listOf(
    ShipTier(
        id = "probe", name = "Recon Probe", emoji = "📡",
        description = "Automated deep-space scanner.",
        baseCost = 10.0, baseIncome = 0.2, basePower = 1.0, unlockPower = 0.0
    ),
    ShipTier(
        id = "shuttle", name = "Scout Shuttle", emoji = "🛸",
        description = "Light recon vessel. Cheap and reliable.",
        baseCost = 150.0, baseIncome = 1.5, basePower = 5.0, unlockPower = 8.0
    ),
    ShipTier(
        id = "corvette", name = "Patrol Corvette", emoji = "⚡",
        description = "Fast attack craft for border patrols.",
        baseCost = 2_500.0, baseIncome = 10.0, basePower = 25.0, unlockPower = 60.0
    ),
    ShipTier(
        id = "frigate", name = "Cargo Frigate", emoji = "🚀",
        description = "Hauler with decent cargo capacity.",
        baseCost = 50_000.0, baseIncome = 60.0, basePower = 130.0, unlockPower = 400.0
    ),
    ShipTier(
        id = "cruiser", name = "Mining Cruiser", emoji = "⛏️",
        description = "Mid-range vessel built for asteroid mining.",
        baseCost = 1_200_000.0, baseIncome = 350.0, basePower = 700.0, unlockPower = 2_500.0
    ),
    ShipTier(
        id = "destroyer", name = "Battle Destroyer", emoji = "💥",
        description = "Heavy warship. Earns bounties from patrols.",
        baseCost = 35_000_000.0, baseIncome = 2_200.0, basePower = 4_500.0, unlockPower = 15_000.0
    ),
    ShipTier(
        id = "battlecruiser", name = "Battlecruiser", emoji = "🔱",
        description = "Versatile capital ship with heavy armament.",
        baseCost = 1_200_000_000.0, baseIncome = 15_000.0, basePower = 30_000.0, unlockPower = 100_000.0
    ),
    ShipTier(
        id = "carrier", name = "Carrier Flagship", emoji = "🛡️",
        description = "Command vessel. Deploys fighter wings.",
        baseCost = 50_000_000_000.0, baseIncome = 100_000.0, basePower = 200_000.0, unlockPower = 800_000.0
    ),
    ShipTier(
        id = "titan", name = "Titan Warship", emoji = "🏛️",
        description = "Massive siege platform. Cracks planets.",
        baseCost = 2_500_000_000_000.0, baseIncome = 750_000.0, basePower = 1_500_000.0, unlockPower = 6_000_000.0
    ),
    ShipTier(
        id = "dreadnought", name = "Dreadnought", emoji = "👑",
        description = "Ultimate capital ship. Galaxy-class power.",
        baseCost = 150_000_000_000_000.0, baseIncome = 6_000_000.0, basePower = 12_000_000.0, unlockPower = 50_000_000.0
    ),
    ShipTier(
        id = "leviathan", name = "Leviathan", emoji = "🐉",
        description = "Ancient bio-mechanical warform. Devours stars.",
        baseCost = 10_000_000_000_000_000.0, baseIncome = 50_000_000.0, basePower = 100_000_000.0, unlockPower = 500_000_000.0
    ),
    ShipTier(
        id = "dyson", name = "Dyson Sphere", emoji = "☀️",
        description = "Harnesses an entire star. The endgame.",
        baseCost = 800_000_000_000_000_000.0, baseIncome = 500_000_000.0, basePower = 1_000_000_000.0, unlockPower = 5_000_000_000.0
    )
)

data class UpgradeType(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val baseCostMultiplier: Double,
    val incomeBoost: Double,
    val costScaling: Double = 1.4
)

val UPGRADE_TYPES = listOf(
    UpgradeType(
        id = "engine",
        name = "Engine",
        emoji = "🔥",
        description = "Faster operations, more income",
        baseCostMultiplier = 5.0,
        incomeBoost = 0.10,
        costScaling = 1.5
    ),
    UpgradeType(
        id = "hull",
        name = "Hull",
        emoji = "🛡️",
        description = "Tougher hull, income multiplier",
        baseCostMultiplier = 8.0,
        incomeBoost = 0.18,
        costScaling = 1.7
    )
)

// --- Milestone thresholds: each doubles output ---
val MILESTONE_THRESHOLDS = listOf(10, 25, 50)

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
    val next = ((count / 100) + 1) * 100
    return next
}

// --- Buy amount options ---
enum class BuyAmount(val label: String) {
    X1("x1"),
    X5("x5"),
    X10("x10"),
    X25("x25"),
    NEXT("Next")
}

fun getBuyCount(amount: BuyAmount, currentCount: Int): Int {
    return when (amount) {
        BuyAmount.X1 -> 1
        BuyAmount.X5 -> 5
        BuyAmount.X10 -> 10
        BuyAmount.X25 -> 25
        BuyAmount.NEXT -> {
            val next = getNextMilestone(currentCount) ?: (currentCount + 1)
            maxOf(1, next - currentCount)
        }
    }
}

// --- Prestige (Star Coins) ---
fun calculatePrestigeCoins(fleetPower: Double): Int {
    if (fleetPower < 1_000) return 0
    return Math.floor(Math.sqrt(fleetPower / 1_000.0)).toInt()
}

fun getPrestigeMultiplier(starCoins: Int): Double {
    return 1.0 + (starCoins * 0.05) // each coin = +5% income
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
    GLOBAL_INCOME_MULT,
    FLEET_POWER_MULT,
    COST_REDUCTION,
    OFFLINE_BOOST,
    CLICK_BONUS
}

val SHOP_BONUSES = listOf(
    ShopBonus(
        id = "warp_drive",
        name = "Warp Drive Tech",
        emoji = "🌀",
        description = "All ships earn 20% more per level",
        baseCost = 5_000.0,
        costScaling = 3.5,
        maxLevel = 10,
        type = BonusType.GLOBAL_INCOME_MULT
    ),
    ShopBonus(
        id = "shield_array",
        name = "Shield Array",
        emoji = "🔰",
        description = "Fleet Power boosted by 15% per level",
        baseCost = 8_000.0,
        costScaling = 3.8,
        maxLevel = 10,
        type = BonusType.FLEET_POWER_MULT
    ),
    ShopBonus(
        id = "trade_routes",
        name = "Trade Routes",
        emoji = "🗺️",
        description = "Ship costs reduced by 3% per level",
        baseCost = 15_000.0,
        costScaling = 4.0,
        maxLevel = 10,
        type = BonusType.COST_REDUCTION
    ),
    ShopBonus(
        id = "auto_pilot",
        name = "Auto-Pilot AI",
        emoji = "🤖",
        description = "Offline earnings +8% efficiency per level",
        baseCost = 20_000.0,
        costScaling = 3.0,
        maxLevel = 10,
        type = BonusType.OFFLINE_BOOST
    ),
    ShopBonus(
        id = "command_bridge",
        name = "Command Bridge",
        emoji = "🎯",
        description = "Tap to earn credits (more per level)",
        baseCost = 2_000.0,
        costScaling = 2.8,
        maxLevel = 15,
        type = BonusType.CLICK_BONUS
    )
)

// --- Daily Login Rewards ---
data class DailyReward(
    val day: Int,
    val emoji: String,
    val description: String,
    val credits: Double = 0.0,
    val gems: Int = 0,
    val researchPoints: Int = 0
)

val DAILY_REWARDS = listOf(
    DailyReward(1, "💰", "500 Credits", credits = 500.0),
    DailyReward(2, "💰", "2K Credits", credits = 2_000.0),
    DailyReward(3, "💎", "5 Gems", gems = 5),
    DailyReward(4, "💰", "10K Credits", credits = 10_000.0),
    DailyReward(5, "🔬", "3 Research Points", researchPoints = 3),
    DailyReward(6, "💎", "15 Gems", gems = 15),
    DailyReward(7, "💎", "50 Gems + 50K Credits", gems = 50, credits = 50_000.0)
)

// --- Gem Store Items ---
data class GemStoreItem(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val gemCost: Int,
    val type: GemItemType
)

enum class GemItemType {
    TIME_WARP_1H,
    TIME_WARP_4H,
    TIME_WARP_8H,
    PERMANENT_INCOME_5,
    PERMANENT_INCOME_15,
    RESEARCH_POINTS_5,
    RESEARCH_POINTS_20
}

val GEM_STORE_ITEMS = listOf(
    GemStoreItem("warp_1h", "Time Warp 1H", "⏩", "Collect 1 hour of income instantly", 10, GemItemType.TIME_WARP_1H),
    GemStoreItem("warp_4h", "Time Warp 4H", "⏭️", "Collect 4 hours of income instantly", 35, GemItemType.TIME_WARP_4H),
    GemStoreItem("warp_8h", "Time Warp 8H", "🚀", "Collect 8 hours of income instantly", 60, GemItemType.TIME_WARP_8H),
    GemStoreItem("perm_income_5", "Income Boost +5%", "📈", "Permanent +5% income (stacks)", 50, GemItemType.PERMANENT_INCOME_5),
    GemStoreItem("perm_income_15", "Income Boost +15%", "📊", "Permanent +15% income (stacks)", 120, GemItemType.PERMANENT_INCOME_15),
    GemStoreItem("rp_5", "Research Pack (5)", "🔬", "Gain 5 Research Points", 25, GemItemType.RESEARCH_POINTS_5),
    GemStoreItem("rp_20", "Research Pack (20)", "🧪", "Gain 20 Research Points", 80, GemItemType.RESEARCH_POINTS_20)
)

// --- Gem IAP Packs (In-App Purchase) ---
data class GemPack(
    val id: String,
    val name: String,
    val emoji: String,
    val gems: Int,
    val bonusGems: Int = 0,
    val priceDisplay: String,   // shown to user
    val priceValue: Double,     // actual price in USD (0.0 = free in dev)
    val isBestValue: Boolean = false
)

val GEM_PACKS = listOf(
    GemPack(
        id = "pack_tiny", name = "Handful of Gems", emoji = "💎",
        gems = 50, bonusGems = 0,
        priceDisplay = "£0.99", priceValue = 0.99
    ),
    GemPack(
        id = "pack_small", name = "Pouch of Gems", emoji = "💎",
        gems = 150, bonusGems = 15,
        priceDisplay = "£1.99", priceValue = 1.99
    ),
    GemPack(
        id = "pack_medium", name = "Chest of Gems", emoji = "💎💎",
        gems = 500, bonusGems = 75,
        priceDisplay = "£4.99", priceValue = 4.99,
        isBestValue = true
    ),
    GemPack(
        id = "pack_large", name = "Vault of Gems", emoji = "💎💎💎",
        gems = 1200, bonusGems = 250,
        priceDisplay = "£9.99", priceValue = 9.99
    ),
    GemPack(
        id = "pack_mega", name = "Galaxy Hoard", emoji = "🌟💎🌟",
        gems = 3000, bonusGems = 800,
        priceDisplay = "£19.99", priceValue = 19.99
    )
)

// Set to true for development (all packs are free), false for production
const val DEV_MODE_FREE_GEMS = true
