import kotlin.math.*

// ============================================================
// SIMULATION: StarFleet Idle Progression Model
// Simulates optimal play without real-money boosts
// Target: ~6 months (180 days) to complete
// ============================================================

// --- Ship Tiers ---
data class ShipTier(
    val id: String, val name: String,
    val baseCost: Double, val baseIncome: Double, val basePower: Double,
    val costMultiplier: Double = 1.22, val unlockPower: Double = 0.0
)

val SHIP_TIERS = listOf(
    ShipTier("probe", "Recon Probe", 15.0, 0.1, 1.0, 1.22, 0.0),
    ShipTier("shuttle", "Scout Shuttle", 250.0, 0.8, 4.0, 1.22, 10.0),
    ShipTier("corvette", "Patrol Corvette", 5_000.0, 6.0, 20.0, 1.22, 80.0),
    ShipTier("frigate", "Cargo Frigate", 100_000.0, 40.0, 100.0, 1.22, 500.0),
    ShipTier("cruiser", "Mining Cruiser", 2_500_000.0, 200.0, 500.0, 1.22, 3_000.0),
    ShipTier("destroyer", "Battle Destroyer", 75_000_000.0, 1_200.0, 3_000.0, 1.22, 20_000.0),
    ShipTier("battlecruiser", "Battlecruiser", 3_000_000_000.0, 8_000.0, 18_000.0, 1.22, 150_000.0),
    ShipTier("carrier", "Carrier Flagship", 150_000_000_000.0, 55_000.0, 120_000.0, 1.22, 1_200_000.0),
    ShipTier("titan", "Titan Warship", 10_000_000_000_000.0, 400_000.0, 900_000.0, 1.22, 12_000_000.0),
    ShipTier("dreadnought", "Dreadnought", 800_000_000_000_000.0, 3_500_000.0, 8_000_000.0, 1.22, 120_000_000.0),
    ShipTier("leviathan", "Leviathan", 80_000_000_000_000_000.0, 30_000_000.0, 80_000_000.0, 1.22, 1_500_000_000.0),
    ShipTier("dyson", "Dyson Sphere", 10_000_000_000_000_000_000.0, 300_000_000.0, 1_000_000_000.0, 1.22, 20_000_000_000.0),
)

// --- Sectors ---
data class Sector(
    val id: String, val name: String,
    val unlockFleetPower: Double, val costMultiplier: Double,
    val incomeMultiplier: Double, val previousPenalty: Double
)

val SECTORS = listOf(
    Sector("solar", "Solar System", 0.0, 1.0, 1.0, 1.0),
    Sector("nebula", "Orion Nebula", 5_000_000.0, 12.0, 2.5, 0.06),
    Sector("deepspace", "Deep Space", 2_000_000_000.0, 150.0, 8.0, 0.04),
    Sector("core", "Galactic Core", 1_000_000_000_000.0, 2_000.0, 30.0, 0.02),
    Sector("void", "The Void", 500_000_000_000_000.0, 30_000.0, 150.0, 0.01),
)

// --- Milestones ---
val MILESTONE_THRESHOLDS = listOf(10, 25, 50)
fun getMilestoneMultiplier(count: Int): Double {
    var d = 0
    for (t in MILESTONE_THRESHOLDS) if (count >= t) d++
    if (count >= 100) d += count / 100
    return 2.0.pow(d.toDouble())
}

// --- Prestige ---
fun calcPrestigeCoins(fp: Double): Int {
    if (fp < 10_000) return 0
    return floor(sqrt(fp / 10_000.0)).toInt()
}
fun prestigeMultiplier(coins: Int) = 1.0 + coins * 0.05

// --- Simulation State ---
data class SimSector(
    val ships: MutableMap<String, Int> = SHIP_TIERS.associate { it.id to 0 }.toMutableMap()
)

data class SimState(
    var credits: Double = 25.0,
    var starCoins: Int = 0,
    var totalPrestigeResets: Int = 0,
    val sectors: MutableMap<String, SimSector> = SECTORS.associate { it.id to SimSector() }.toMutableMap(),
    var activeSectorIdx: Int = 0,
    var elapsedSeconds: Double = 0.0,
    var sessionPlaySeconds: Double = 0.0 // active play per day
)

fun SimState.totalFleetPower(): Double {
    var total = 0.0
    for (sector in SECTORS) {
        val ss = sectors[sector.id]!!
        var sp = 0.0
        for (tier in SHIP_TIERS) {
            val count = ss.ships[tier.id]!!
            sp += count * tier.basePower * getMilestoneMultiplier(count)
        }
        total += sp * sector.incomeMultiplier
    }
    return total
}

fun SimState.creditsPerSecond(): Double {
    var totalIncome = 0.0
    for ((idx, sector) in SECTORS.withIndex()) {
        val ss = sectors[sector.id]!!
        val penalty = when {
            idx < activeSectorIdx -> sector.previousPenalty
            idx == activeSectorIdx -> 1.0
            else -> 0.0
        }
        if (penalty <= 0.0) continue

        var sectorIncome = 0.0
        for (tier in SHIP_TIERS) {
            val count = ss.ships[tier.id]!!
            if (count == 0) continue
            val milestone = getMilestoneMultiplier(count)
            sectorIncome += tier.baseIncome * count * milestone * sector.incomeMultiplier
        }
        totalIncome += sectorIncome * penalty
    }
    return totalIncome * prestigeMultiplier(starCoins)
}

fun SimState.getShipCost(tierId: String, sectorIdx: Int): Double {
    val tier = SHIP_TIERS.first { it.id == tierId }
    val sector = SECTORS[sectorIdx]
    val count = sectors[sector.id]!!.ships[tierId]!!
    return tier.baseCost * sector.costMultiplier * tier.costMultiplier.pow(count.toDouble())
}

fun SimState.getShipFleetPower(tierId: String, sectorIdx: Int): Double {
    val tier = SHIP_TIERS.first { it.id == tierId }
    val sector = SECTORS[sectorIdx]
    val count = sectors[sector.id]!!.ships[tierId]!!
    val newCount = count + 1
    val newMilestone = getMilestoneMultiplier(newCount)
    val oldMilestone = getMilestoneMultiplier(count)
    return (newCount * newMilestone - count * oldMilestone) * tier.basePower * sector.incomeMultiplier
}

fun SimState.buyBestShip(): Boolean {
    // Strategy: buy the ship with best income/cost ratio in active sector
    var bestRatio = 0.0
    var bestTier: String? = null

    val sector = SECTORS[activeSectorIdx]
    val ss = sectors[sector.id]!!

    for (tier in SHIP_TIERS) {
        // Check unlock
        if (totalFleetPower() < tier.unlockPower) continue
        val cost = getShipCost(tier.id, activeSectorIdx)
        if (cost > credits) continue

        val count = ss.ships[tier.id]!!
        val newMilestone = getMilestoneMultiplier(count + 1)
        val incomeGain = tier.baseIncome * (count + 1) * newMilestone * sector.incomeMultiplier -
            tier.baseIncome * count * getMilestoneMultiplier(count) * sector.incomeMultiplier
        val ratio = incomeGain / cost
        if (ratio > bestRatio) {
            bestRatio = ratio
            bestTier = tier.id
        }
    }

    if (bestTier != null) {
        val cost = getShipCost(bestTier, activeSectorIdx)
        credits -= cost
        sectors[SECTORS[activeSectorIdx].id]!!.ships[bestTier] =
            sectors[SECTORS[activeSectorIdx].id]!!.ships[bestTier]!! + 1
        return true
    }
    return false
}

fun SimState.shouldPrestige(): Boolean {
    val coins = calcPrestigeCoins(totalFleetPower())
    if (coins <= 0) return false
    // Prestige if we'd gain at least 20% more coins than we have
    return coins >= maxOf(5, (starCoins * 0.2).toInt())
}

fun SimState.prestige() {
    val coins = calcPrestigeCoins(totalFleetPower())
    starCoins += coins
    totalPrestigeResets++
    credits = 25.0
    for (sector in SECTORS) {
        sectors[sector.id] = SimSector()
    }
    activeSectorIdx = 0
}

fun SimState.tryAdvanceSector(): Boolean {
    if (activeSectorIdx >= SECTORS.size - 1) return false
    val nextSector = SECTORS[activeSectorIdx + 1]
    if (totalFleetPower() >= nextSector.unlockFleetPower) {
        activeSectorIdx++
        return true
    }
    return false
}

// --- Main Simulation ---
fun main() {
    val state = SimState()

    // Simulate with assumptions:
    // - Player plays actively 30 min/day (makes purchases)
    // - Offline for remaining 23.5 hours at 40% efficiency
    // - Optimal buying strategy
    // - Prestiges when beneficial

    val ACTIVE_SECONDS_PER_DAY = 1800.0  // 30 min active
    val OFFLINE_SECONDS_PER_DAY = 84600.0 // 23.5 hours
    val OFFLINE_EFFICIENCY = 0.4
    val TICK_INTERVAL = 60.0 // simulate in 1-minute ticks during active play
    val TARGET_DAYS = 210 // simulate up to 7 months

    val milestones = mutableListOf<String>()
    var lastSectorUnlocked = 0
    var gameComplete = false

    println("=" .repeat(70))
    println("STARFLEET IDLE - PROGRESSION SIMULATION")
    println("=" .repeat(70))
    println("Assumptions: 30 min active play/day, optimal strategy, no IAP")
    println("-".repeat(70))

    for (day in 1..TARGET_DAYS) {
        // --- Active play phase ---
        var activeTimeLeft = ACTIVE_SECONDS_PER_DAY
        while (activeTimeLeft > 0) {
            // Earn credits for this tick
            val cps = state.creditsPerSecond()
            val tickTime = minOf(TICK_INTERVAL, activeTimeLeft)
            state.credits += cps * tickTime
            state.elapsedSeconds += tickTime
            activeTimeLeft -= tickTime

            // Try to buy ships
            var bought = true
            while (bought) {
                bought = state.buyBestShip()
            }

            // Check sector advancement
            state.tryAdvanceSector()

            // Check prestige
            if (state.shouldPrestige()) {
                state.prestige()
            }
        }

        // --- Offline phase ---
        val offlineCps = state.creditsPerSecond()
        state.credits += offlineCps * OFFLINE_SECONDS_PER_DAY * OFFLINE_EFFICIENCY
        state.elapsedSeconds += OFFLINE_SECONDS_PER_DAY

        // --- Daily reporting ---
        val fp = state.totalFleetPower()
        val cps = state.creditsPerSecond()

        // Check sector milestones
        while (lastSectorUnlocked < SECTORS.size - 1) {
            val next = SECTORS[lastSectorUnlocked + 1]
            if (fp >= next.unlockFleetPower) {
                lastSectorUnlocked++
                milestones.add("Day $day: Unlocked ${next.name} (FP: ${formatNum(fp)})")
            } else break
        }

        // Check game completion
        val voidDysons = state.sectors["void"]!!.ships["dyson"]!!
        if (voidDysons >= 3 && !gameComplete) {
            gameComplete = true
            milestones.add("Day $day: 🎉 GAME COMPLETE (3 Dyson Spheres in The Void)")
        }

        // Print weekly summary
        if (day % 7 == 0 || day == 1) {
            println("Day %4d | CPS: %12s | FP: %12s | Coins: %4d | Sector: %s | Prestiges: %d".format(
                day, formatNum(cps), formatNum(fp), state.starCoins,
                SECTORS[state.activeSectorIdx].name, state.totalPrestigeResets
            ))
        }

        if (gameComplete) break
    }

    println("-".repeat(70))
    println("\nMILESTONES:")
    milestones.forEach { println("  $it") }

    println("\n" + "=".repeat(70))
    if (gameComplete) {
        val days = (state.elapsedSeconds / 86400).toInt()
        println("RESULT: Game completed in $days days (~${days/30} months)")
    } else {
        println("RESULT: Game NOT completed in $TARGET_DAYS days")
        println("  Final FP: ${formatNum(state.totalFleetPower())}")
        println("  Final CPS: ${formatNum(state.creditsPerSecond())}")
        println("  Active Sector: ${SECTORS[state.activeSectorIdx].name}")
        println("  Void Dysons: ${state.sectors["void"]!!.ships["dyson"]}")
    }
    println("=".repeat(70))
}

fun formatNum(v: Double): String = when {
    v >= 1e33 -> "%.2fDc".format(v / 1e33)
    v >= 1e30 -> "%.2fNo".format(v / 1e30)
    v >= 1e27 -> "%.2fOc".format(v / 1e27)
    v >= 1e24 -> "%.2fSp".format(v / 1e24)
    v >= 1e21 -> "%.2fSx".format(v / 1e21)
    v >= 1e18 -> "%.2fQi".format(v / 1e18)
    v >= 1e15 -> "%.2fQa".format(v / 1e15)
    v >= 1e12 -> "%.2fT".format(v / 1e12)
    v >= 1e9 -> "%.2fB".format(v / 1e9)
    v >= 1e6 -> "%.2fM".format(v / 1e6)
    v >= 1e3 -> "%.2fK".format(v / 1e3)
    v >= 1 -> "%.1f".format(v)
    else -> "%.2f".format(v)
}
