package com.starfleet.idle.data

data class ResearchNode(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val branch: ResearchBranch,
    val baseCost: Int,          // research points
    val costScaling: Double,    // cost multiplier per level
    val maxLevel: Int,
    val effectPerLevel: Double  // depends on branch type
)

enum class ResearchBranch(val label: String, val emoji: String) {
    PROPULSION("Propulsion", "🔥"),
    ECONOMICS("Economics", "💰"),
    EXPLORATION("Exploration", "🔭"),
    MILITARY("Military", "⚔️")
}

val RESEARCH_NODES = listOf(
    // Propulsion — income speed
    ResearchNode(
        id = "ion_drives", name = "Ion Drives", emoji = "⚡",
        description = "+8% income per level",
        branch = ResearchBranch.PROPULSION,
        baseCost = 5, costScaling = 1.8, maxLevel = 15, effectPerLevel = 0.08
    ),
    ResearchNode(
        id = "fusion_cores", name = "Fusion Cores", emoji = "☢️",
        description = "+12% income per level",
        branch = ResearchBranch.PROPULSION,
        baseCost = 20, costScaling = 2.0, maxLevel = 12, effectPerLevel = 0.12
    ),
    ResearchNode(
        id = "antimatter", name = "Antimatter Engines", emoji = "💥",
        description = "+18% income per level",
        branch = ResearchBranch.PROPULSION,
        baseCost = 80, costScaling = 2.2, maxLevel = 10, effectPerLevel = 0.18
    ),

    // Economics — cost reduction
    ResearchNode(
        id = "supply_chains", name = "Supply Chains", emoji = "📦",
        description = "Ship costs -4% per level",
        branch = ResearchBranch.ECONOMICS,
        baseCost = 5, costScaling = 1.8, maxLevel = 15, effectPerLevel = 0.04
    ),
    ResearchNode(
        id = "trade_networks", name = "Trade Networks", emoji = "🌐",
        description = "Ship costs -6% per level",
        branch = ResearchBranch.ECONOMICS,
        baseCost = 25, costScaling = 2.0, maxLevel = 12, effectPerLevel = 0.06
    ),
    ResearchNode(
        id = "galactic_bank", name = "Galactic Bank", emoji = "🏦",
        description = "Ship costs -8% per level",
        branch = ResearchBranch.ECONOMICS,
        baseCost = 100, costScaling = 2.2, maxLevel = 10, effectPerLevel = 0.08
    ),

    // Exploration — sector bonuses
    ResearchNode(
        id = "long_range", name = "Long Range Scanners", emoji = "📡",
        description = "Previous sector penalty -10% per level",
        branch = ResearchBranch.EXPLORATION,
        baseCost = 8, costScaling = 1.9, maxLevel = 10, effectPerLevel = 0.10
    ),
    ResearchNode(
        id = "wormholes", name = "Wormhole Tech", emoji = "🌀",
        description = "Sector unlock requirement -5% per level",
        branch = ResearchBranch.EXPLORATION,
        baseCost = 30, costScaling = 2.0, maxLevel = 10, effectPerLevel = 0.05
    ),
    ResearchNode(
        id = "dark_matter", name = "Dark Matter Harvesting", emoji = "🔮",
        description = "+15% research point generation per level",
        branch = ResearchBranch.EXPLORATION,
        baseCost = 50, costScaling = 2.3, maxLevel = 8, effectPerLevel = 0.15
    ),

    // Military — fleet power
    ResearchNode(
        id = "armor_plating", name = "Armor Plating", emoji = "🛡️",
        description = "+10% fleet power per level",
        branch = ResearchBranch.MILITARY,
        baseCost = 5, costScaling = 1.8, maxLevel = 15, effectPerLevel = 0.10
    ),
    ResearchNode(
        id = "weapons_array", name = "Weapons Array", emoji = "🎯",
        description = "+15% fleet power per level",
        branch = ResearchBranch.MILITARY,
        baseCost = 25, costScaling = 2.0, maxLevel = 12, effectPerLevel = 0.15
    ),
    ResearchNode(
        id = "titan_forge", name = "Titan Forge", emoji = "🏗️",
        description = "+20% fleet power per level",
        branch = ResearchBranch.MILITARY,
        baseCost = 100, costScaling = 2.2, maxLevel = 10, effectPerLevel = 0.20
    )
)
