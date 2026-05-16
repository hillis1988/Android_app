package com.starfleet.idle.data

data class Perk(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val cost: Int, // Star Coins
    val maxLevel: Int = 1
)

val STAR_COIN_PERKS = listOf(
    Perk("autopilot_2", "Auto-Pilot MK II", "🤖", "Increase max offline time to 24 hours.", 50),
    Perk("wormhole_mastery", "Wormhole Mastery", "🌀", "Start each prestige with 10K credits + 5 free Probes.", 100),
    Perk("gem_finder", "Gem Prospector", "💎", "+10% chance for extra Gems from achievements.", 250),
    Perk("speed_docking", "Speed Docking", "⚓", "Ship costs reduced by 10% permanently.", 500)
)
