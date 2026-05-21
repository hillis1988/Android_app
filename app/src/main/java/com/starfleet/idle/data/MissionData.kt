package com.starfleet.idle.data

// --- Mission Type ---
enum class MissionType { COMBAT, MINING, EXPLORATION, DIPLOMACY }

// --- Mission Difficulty ---
enum class MissionDifficulty(
    val durationOptions: List<Long>,  // in milliseconds
    val rewardMultiplier: Double,
    val gemReward: Int,
    val researchPointReward: IntRange,
    val hasBonusRewardChance: Boolean
) {
    EASY(listOf(30 * 60 * 1000L, 60 * 60 * 1000L), 0.3, 10, 0..0, false),
    MEDIUM(listOf(60 * 60 * 1000L, 2 * 60 * 60 * 1000L), 0.5, 20, 0..0, false),
    HARD(listOf(2 * 60 * 60 * 1000L, 4 * 60 * 60 * 1000L), 0.65, 30, 1..3, false),
    ELITE(listOf(4 * 60 * 60 * 1000L, 8 * 60 * 60 * 1000L), 0.8, 50, 2..3, true)
}

// --- Mission Status ---
enum class MissionStatus { IN_PROGRESS, COMPLETED_SUCCESS, COMPLETED_FAILURE, COLLECTED }

// --- Bonus Reward Type ---
enum class BonusRewardType(val description: String, val durationMs: Long) {
    INCOME_BOOST("+20% income for 2 hours", 2 * 60 * 60 * 1000L),
    FLEET_POWER_BOOST("+20% fleet power for 2 hours", 2 * 60 * 60 * 1000L),
    RESEARCH_SPEED_BOOST("+30% research speed for 2 hours", 2 * 60 * 60 * 1000L)
}

// --- Mission Template ---
data class MissionTemplate(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val type: MissionType,
    val difficulty: MissionDifficulty,
    val sectorId: String,
    val requiredFleetPower: Double,
    val durationMs: Long,
    val cooldownUntil: Long = 0L
)

// --- Active Mission ---
data class ActiveMission(
    val id: String,
    val templateId: String,
    val missionName: String,
    val missionType: MissionType,
    val difficulty: MissionDifficulty,
    val sectorId: String,
    val requiredFleetPower: Double,
    val durationMs: Long,
    val deployedShipTiers: List<String>,
    val startTime: Long,
    val status: MissionStatus = MissionStatus.IN_PROGRESS
)

// --- Mission Result ---
data class MissionResult(
    val missionId: String,
    val missionName: String,
    val success: Boolean,
    val successChance: Double,
    val creditsEarned: Double,
    val gemsEarned: Int,
    val researchPointsEarned: Int,
    val bonusReward: BonusRewardType?,
    val completedAt: Long
)

// --- Ship Affinity ---
data class ShipAffinity(
    val shipTierId: String,
    val combat: Double,
    val mining: Double,
    val exploration: Double,
    val diplomacy: Double
)

// --- Deployment Validation ---
data class DeploymentValidation(
    val isValid: Boolean,
    val errorMessage: String? = null
)

// --- Mission Board State ---
data class MissionBoardState(
    val missions: List<MissionTemplate> = emptyList(),
    val lastRefreshTime: Long = 0L,
    val cooldowns: Map<String, Long> = emptyMap()  // templateId -> cooldownUntil timestamp
)

// --- Mission History Entry ---
data class MissionHistoryEntry(
    val missionName: String,
    val success: Boolean,
    val completedAt: Long
)

// --- Ship Affinity Table ---
// All values ≥ 0.8, diplomacy ≥ 1.0 per Requirement 2.7
val SHIP_AFFINITIES = listOf(
    ShipAffinity(shipTierId = "probe", combat = 0.8, mining = 0.8, exploration = 2.0, diplomacy = 1.0),
    ShipAffinity(shipTierId = "shuttle", combat = 0.8, mining = 0.9, exploration = 1.8, diplomacy = 1.2),
    ShipAffinity(shipTierId = "corvette", combat = 1.5, mining = 0.8, exploration = 1.2, diplomacy = 1.0),
    ShipAffinity(shipTierId = "frigate", combat = 0.8, mining = 1.8, exploration = 1.0, diplomacy = 1.5),
    ShipAffinity(shipTierId = "cruiser", combat = 0.8, mining = 2.0, exploration = 0.8, diplomacy = 1.0),
    ShipAffinity(shipTierId = "destroyer", combat = 2.0, mining = 0.8, exploration = 0.8, diplomacy = 1.0),
    ShipAffinity(shipTierId = "battlecruiser", combat = 1.8, mining = 0.9, exploration = 1.0, diplomacy = 1.0),
    ShipAffinity(shipTierId = "carrier", combat = 1.2, mining = 0.8, exploration = 1.5, diplomacy = 1.2),
    ShipAffinity(shipTierId = "titan", combat = 1.8, mining = 1.0, exploration = 0.8, diplomacy = 1.0),
    ShipAffinity(shipTierId = "dreadnought", combat = 2.0, mining = 0.8, exploration = 0.8, diplomacy = 1.0),
    ShipAffinity(shipTierId = "leviathan", combat = 1.5, mining = 1.2, exploration = 1.2, diplomacy = 1.5),
    ShipAffinity(shipTierId = "dyson", combat = 1.2, mining = 1.5, exploration = 1.5, diplomacy = 2.0)
)

// --- Mission Templates ---
// Missions scaled by sector progression with increasing fleet power requirements.
// Each sector has missions across all 4 types and multiple difficulties.
val MISSION_TEMPLATES = listOf(
    // ===== SOLAR SYSTEM (starter sector, low power requirements) =====
    // Combat
    MissionTemplate(
        id = "combat_easy_solar_1", name = "Patrol Route Alpha", emoji = "🛡️",
        description = "Routine patrol of inner system shipping lanes.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.EASY,
        sectorId = "solar", requiredFleetPower = 50.0,
        durationMs = 30 * 60 * 1000L
    ),
    MissionTemplate(
        id = "combat_medium_solar_1", name = "Asteroid Belt Skirmish", emoji = "⚔️",
        description = "Engage pirate raiders in the asteroid belt.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "solar", requiredFleetPower = 200.0,
        durationMs = 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "combat_hard_solar_1", name = "Orbital Defense Drill", emoji = "💥",
        description = "Repel a simulated invasion force near Mars.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.HARD,
        sectorId = "solar", requiredFleetPower = 500.0,
        durationMs = 2 * 60 * 60 * 1000L
    ),
    // Mining
    MissionTemplate(
        id = "mining_easy_solar_1", name = "Lunar Ore Collection", emoji = "🌙",
        description = "Harvest mineral deposits from the Moon's surface.",
        type = MissionType.MINING, difficulty = MissionDifficulty.EASY,
        sectorId = "solar", requiredFleetPower = 40.0,
        durationMs = 30 * 60 * 1000L
    ),
    MissionTemplate(
        id = "mining_medium_solar_1", name = "Belt Mining Operation", emoji = "⛏️",
        description = "Extract rare metals from asteroid belt fragments.",
        type = MissionType.MINING, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "solar", requiredFleetPower = 180.0,
        durationMs = 2 * 60 * 60 * 1000L
    ),
    // Exploration
    MissionTemplate(
        id = "exploration_easy_solar_1", name = "Probe Jupiter's Moons", emoji = "🔭",
        description = "Survey the Galilean moons for anomalies.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.EASY,
        sectorId = "solar", requiredFleetPower = 30.0,
        durationMs = 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "exploration_medium_solar_1", name = "Kuiper Belt Survey", emoji = "🌌",
        description = "Map uncharted objects beyond Neptune's orbit.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "solar", requiredFleetPower = 150.0,
        durationMs = 60 * 60 * 1000L
    ),
    // Diplomacy
    MissionTemplate(
        id = "diplomacy_easy_solar_1", name = "Trade Negotiation", emoji = "🤝",
        description = "Negotiate supply contracts with Mars colonies.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.EASY,
        sectorId = "solar", requiredFleetPower = 30.0,
        durationMs = 30 * 60 * 1000L
    ),
    MissionTemplate(
        id = "diplomacy_medium_solar_1", name = "Colony Dispute Resolution", emoji = "⚖️",
        description = "Mediate a territorial dispute between outer colonies.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "solar", requiredFleetPower = 160.0,
        durationMs = 60 * 60 * 1000L
    ),

    // ===== ORION NEBULA (mid-early, moderate power) =====
    // Combat
    MissionTemplate(
        id = "combat_easy_nebula_1", name = "Nebula Perimeter Sweep", emoji = "🛡️",
        description = "Clear hostile drones from the nebula's edge.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.EASY,
        sectorId = "nebula", requiredFleetPower = 2_000.0,
        durationMs = 30 * 60 * 1000L
    ),
    MissionTemplate(
        id = "combat_medium_nebula_1", name = "Pirate Stronghold Raid", emoji = "⚔️",
        description = "Assault a hidden pirate base in the gas clouds.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "nebula", requiredFleetPower = 8_000.0,
        durationMs = 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "combat_hard_nebula_1", name = "Nebula Warlord Takedown", emoji = "💥",
        description = "Eliminate a notorious warlord's fleet.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.HARD,
        sectorId = "nebula", requiredFleetPower = 25_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    // Mining
    MissionTemplate(
        id = "mining_easy_nebula_1", name = "Gas Cloud Harvesting", emoji = "💨",
        description = "Collect rare gases from nebula pockets.",
        type = MissionType.MINING, difficulty = MissionDifficulty.EASY,
        sectorId = "nebula", requiredFleetPower = 1_500.0,
        durationMs = 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "mining_medium_nebula_1", name = "Crystal Vein Extraction", emoji = "💎",
        description = "Mine crystalline formations deep in the nebula.",
        type = MissionType.MINING, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "nebula", requiredFleetPower = 7_000.0,
        durationMs = 2 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "mining_hard_nebula_1", name = "Neutronium Deposit", emoji = "⛏️",
        description = "Extract ultra-dense neutronium from a collapsed star remnant.",
        type = MissionType.MINING, difficulty = MissionDifficulty.HARD,
        sectorId = "nebula", requiredFleetPower = 20_000.0,
        durationMs = 2 * 60 * 60 * 1000L
    ),
    // Exploration
    MissionTemplate(
        id = "exploration_easy_nebula_1", name = "Nebula Cartography", emoji = "🗺️",
        description = "Map safe navigation routes through the nebula.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.EASY,
        sectorId = "nebula", requiredFleetPower = 1_200.0,
        durationMs = 30 * 60 * 1000L
    ),
    MissionTemplate(
        id = "exploration_medium_nebula_1", name = "Anomaly Investigation", emoji = "🔬",
        description = "Investigate strange energy readings in sector 7G.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "nebula", requiredFleetPower = 6_000.0,
        durationMs = 60 * 60 * 1000L
    ),
    // Diplomacy
    MissionTemplate(
        id = "diplomacy_easy_nebula_1", name = "Miner Guild Parley", emoji = "🤝",
        description = "Establish trade relations with the Nebula Miners Guild.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.EASY,
        sectorId = "nebula", requiredFleetPower = 1_000.0,
        durationMs = 30 * 60 * 1000L
    ),
    MissionTemplate(
        id = "diplomacy_medium_nebula_1", name = "Ceasefire Brokering", emoji = "🕊️",
        description = "Negotiate a ceasefire between rival mining factions.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "nebula", requiredFleetPower = 5_500.0,
        durationMs = 2 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "diplomacy_hard_nebula_1", name = "Alliance Formation", emoji = "⚖️",
        description = "Unite nebula factions under a mutual defense pact.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.HARD,
        sectorId = "nebula", requiredFleetPower = 18_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),

    // ===== DEEP SPACE (mid-game, high power) =====
    // Combat
    MissionTemplate(
        id = "combat_medium_deepspace_1", name = "Void Raider Ambush", emoji = "⚔️",
        description = "Intercept raiders preying on deep space convoys.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "deepspace", requiredFleetPower = 500_000.0,
        durationMs = 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "combat_hard_deepspace_1", name = "Rogue AI Fleet", emoji = "🤖",
        description = "Destroy a rogue AI's automated war fleet.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.HARD,
        sectorId = "deepspace", requiredFleetPower = 2_000_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "combat_elite_deepspace_1", name = "Leviathan Hunt", emoji = "🐉",
        description = "Track and engage a space leviathan threatening trade routes.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.ELITE,
        sectorId = "deepspace", requiredFleetPower = 5_000_000.0,
        durationMs = 8 * 60 * 60 * 1000L
    ),
    // Mining
    MissionTemplate(
        id = "mining_medium_deepspace_1", name = "Dark Matter Siphon", emoji = "🌑",
        description = "Collect dark matter from interstellar voids.",
        type = MissionType.MINING, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "deepspace", requiredFleetPower = 400_000.0,
        durationMs = 2 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "mining_hard_deepspace_1", name = "Collapsed Star Mining", emoji = "⭐",
        description = "Extract exotic matter from a white dwarf remnant.",
        type = MissionType.MINING, difficulty = MissionDifficulty.HARD,
        sectorId = "deepspace", requiredFleetPower = 1_500_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    // Exploration
    MissionTemplate(
        id = "exploration_medium_deepspace_1", name = "Uncharted System Survey", emoji = "🔭",
        description = "Explore a newly detected star system.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "deepspace", requiredFleetPower = 350_000.0,
        durationMs = 2 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "exploration_hard_deepspace_1", name = "Wormhole Mapping", emoji = "🕳️",
        description = "Chart a stable wormhole's exit coordinates.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.HARD,
        sectorId = "deepspace", requiredFleetPower = 1_800_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "exploration_elite_deepspace_1", name = "Dimensional Rift Probe", emoji = "🌀",
        description = "Send probes into a dimensional rift and retrieve data.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.ELITE,
        sectorId = "deepspace", requiredFleetPower = 4_000_000.0,
        durationMs = 8 * 60 * 60 * 1000L
    ),
    // Diplomacy
    MissionTemplate(
        id = "diplomacy_medium_deepspace_1", name = "First Contact Protocol", emoji = "👽",
        description = "Initiate communication with an unknown species.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.MEDIUM,
        sectorId = "deepspace", requiredFleetPower = 300_000.0,
        durationMs = 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "diplomacy_hard_deepspace_1", name = "Peace Summit", emoji = "🕊️",
        description = "Host a multi-species peace summit in neutral space.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.HARD,
        sectorId = "deepspace", requiredFleetPower = 1_200_000.0,
        durationMs = 2 * 60 * 60 * 1000L
    ),

    // ===== GALACTIC CORE (late-game, very high power) =====
    // Combat
    MissionTemplate(
        id = "combat_hard_core_1", name = "Core Guardian Battle", emoji = "👑",
        description = "Challenge the ancient guardians of the galactic core.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.HARD,
        sectorId = "core", requiredFleetPower = 100_000_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "combat_elite_core_1", name = "Singularity Siege", emoji = "💫",
        description = "Assault a fortress built around a black hole.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.ELITE,
        sectorId = "core", requiredFleetPower = 500_000_000.0,
        durationMs = 8 * 60 * 60 * 1000L
    ),
    // Mining
    MissionTemplate(
        id = "mining_hard_core_1", name = "Accretion Disk Harvest", emoji = "🌟",
        description = "Mine exotic particles from a black hole's accretion disk.",
        type = MissionType.MINING, difficulty = MissionDifficulty.HARD,
        sectorId = "core", requiredFleetPower = 80_000_000.0,
        durationMs = 2 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "mining_elite_core_1", name = "Quantum Ore Extraction", emoji = "⚛️",
        description = "Extract quantum-entangled ore from the core's heart.",
        type = MissionType.MINING, difficulty = MissionDifficulty.ELITE,
        sectorId = "core", requiredFleetPower = 400_000_000.0,
        durationMs = 8 * 60 * 60 * 1000L
    ),
    // Exploration
    MissionTemplate(
        id = "exploration_hard_core_1", name = "Black Hole Observation", emoji = "🕳️",
        description = "Gather data from the event horizon of a supermassive black hole.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.HARD,
        sectorId = "core", requiredFleetPower = 90_000_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "exploration_elite_core_1", name = "Galactic Center Mapping", emoji = "🗺️",
        description = "Complete the definitive map of the galaxy's central region.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.ELITE,
        sectorId = "core", requiredFleetPower = 350_000_000.0,
        durationMs = 8 * 60 * 60 * 1000L
    ),
    // Diplomacy
    MissionTemplate(
        id = "diplomacy_hard_core_1", name = "Elder Race Audience", emoji = "🏛️",
        description = "Seek an audience with the ancient elder race of the core.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.HARD,
        sectorId = "core", requiredFleetPower = 75_000_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "diplomacy_elite_core_1", name = "Galactic Council Formation", emoji = "👑",
        description = "Unite all known species into a galactic governing council.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.ELITE,
        sectorId = "core", requiredFleetPower = 300_000_000.0,
        durationMs = 8 * 60 * 60 * 1000L
    ),

    // ===== THE VOID (endgame, extreme power) =====
    // Combat
    MissionTemplate(
        id = "combat_hard_void_1", name = "Extragalactic Invaders", emoji = "👾",
        description = "Repel an invasion force from beyond the galaxy.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.HARD,
        sectorId = "void", requiredFleetPower = 5_000_000_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "combat_elite_void_1", name = "Reality Breach Containment", emoji = "🌀",
        description = "Seal a breach where hostile entities pour through from another dimension.",
        type = MissionType.COMBAT, difficulty = MissionDifficulty.ELITE,
        sectorId = "void", requiredFleetPower = 20_000_000_000.0,
        durationMs = 8 * 60 * 60 * 1000L
    ),
    // Mining
    MissionTemplate(
        id = "mining_hard_void_1", name = "Void Crystal Harvest", emoji = "🔮",
        description = "Collect reality-bending crystals from the void's edge.",
        type = MissionType.MINING, difficulty = MissionDifficulty.HARD,
        sectorId = "void", requiredFleetPower = 4_000_000_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "mining_elite_void_1", name = "Primordial Matter Extraction", emoji = "☀️",
        description = "Extract matter from before the universe's formation.",
        type = MissionType.MINING, difficulty = MissionDifficulty.ELITE,
        sectorId = "void", requiredFleetPower = 15_000_000_000.0,
        durationMs = 8 * 60 * 60 * 1000L
    ),
    // Exploration
    MissionTemplate(
        id = "exploration_hard_void_1", name = "Beyond the Edge", emoji = "🌌",
        description = "Explore what lies beyond the galaxy's boundary.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.HARD,
        sectorId = "void", requiredFleetPower = 4_500_000_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "exploration_elite_void_1", name = "Multiverse Gateway", emoji = "🚪",
        description = "Locate and stabilize a gateway to a parallel universe.",
        type = MissionType.EXPLORATION, difficulty = MissionDifficulty.ELITE,
        sectorId = "void", requiredFleetPower = 18_000_000_000.0,
        durationMs = 8 * 60 * 60 * 1000L
    ),
    // Diplomacy
    MissionTemplate(
        id = "diplomacy_hard_void_1", name = "Void Entity Communion", emoji = "👁️",
        description = "Attempt communication with the enigmatic void entities.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.HARD,
        sectorId = "void", requiredFleetPower = 3_500_000_000.0,
        durationMs = 4 * 60 * 60 * 1000L
    ),
    MissionTemplate(
        id = "diplomacy_elite_void_1", name = "Universal Accord", emoji = "🌟",
        description = "Forge an accord that transcends dimensions and realities.",
        type = MissionType.DIPLOMACY, difficulty = MissionDifficulty.ELITE,
        sectorId = "void", requiredFleetPower = 12_000_000_000.0,
        durationMs = 8 * 60 * 60 * 1000L
    )
)
