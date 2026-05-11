package com.starfleet.idle.data

data class EncounterData(
    val id: String,
    val type: String
)

data class EncounterUI(
    val emoji: String,
    val title: String,
    val description: String,
    val options: List<String>
)

fun getEncounterUI(type: String): EncounterUI {
    return when (type) {
        "asteroid" -> EncounterUI(
            "☄️", "Golden Asteroid",
            "A shimmering asteroid drifts by your fleet. It's rich in rare minerals!",
            listOf("Mine for Credits", "Extract Gems")
        )
        "trader" -> EncounterUI(
            "🛸", "Alien Merchant",
            "A wandering trader offers to share advanced knowledge for a fee.",
            listOf("Pay Credits for RP", "Trade Gems for RP")
        )
        "anomaly" -> EncounterUI(
            "🌀", "Space Anomaly",
            "A rift in spacetime has opened. It pulses with strange energy.",
            listOf("Study (Income Boost)", "Stabilize (Star Coins)")
        )
        else -> EncounterUI("❓", "Unknown", "Something strange is happening.", listOf("Ignore"))
    }
}
