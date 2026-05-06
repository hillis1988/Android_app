package com.starfleet.idle.data

enum class SectorSpecialty {
    NONE,
    FAST_PROGRESS,
    PRESTIGE_BONUS,
    RESEARCH_BOOST,
    UPGRADE_EFFICIENCY
}

data class Sector(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val unlockFleetPower: Double,
    val costMultiplier: Double,
    val incomeMultiplier: Double,
    val previousSectorPenalty: Double = 0.1,
    val specialty: SectorSpecialty = SectorSpecialty.NONE
)

val SECTORS = listOf(
    Sector(
        id = "solar",
        name = "Solar System",
        emoji = "☀️",
        description = "Home territory. Where it all begins.",
        unlockFleetPower = 0.0,
        costMultiplier = 1.0,
        incomeMultiplier = 1.0,
        previousSectorPenalty = 1.0,
        specialty = SectorSpecialty.NONE
    ),
    Sector(
        id = "nebula",
        name = "Orion Nebula",
        emoji = "🌌",
        description = "Dense gas clouds hide rich mining opportunities.",
        unlockFleetPower = 500_000.0,
        costMultiplier = 5.0,
        incomeMultiplier = 2.5,
        previousSectorPenalty = 0.08,
        specialty = SectorSpecialty.FAST_PROGRESS
    ),
    Sector(
        id = "deepspace",
        name = "Deep Space",
        emoji = "🌑",
        description = "The void between stars. Dangerous but profitable.",
        unlockFleetPower = 30_000_000.0,
        costMultiplier = 35.0,
        incomeMultiplier = 8.0,
        previousSectorPenalty = 0.05,
        specialty = SectorSpecialty.UPGRADE_EFFICIENCY
    ),
    Sector(
        id = "core",
        name = "Galactic Core",
        emoji = "💫",
        description = "The heart of the galaxy. Extreme energy.",
        unlockFleetPower = 800_000_000.0,
        costMultiplier = 250.0,
        incomeMultiplier = 25.0,
        previousSectorPenalty = 0.03,
        specialty = SectorSpecialty.RESEARCH_BOOST
    ),
    Sector(
        id = "void",
        name = "The Void",
        emoji = "🕳️",
        description = "Beyond the galaxy. Reality bends here.",
        unlockFleetPower = 3_000_000_000.0,
        costMultiplier = 200.0,
        incomeMultiplier = 60.0,
        previousSectorPenalty = 0.01,
        specialty = SectorSpecialty.PRESTIGE_BONUS
    )
)
