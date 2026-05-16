package com.starfleet.idle.data

/**
 * Developer tips — one-time consumable IAPs that let players support the
 * developers directly. The product IDs here must match Play Console exactly.
 *
 * Tippers also get a small in-game thank-you (gems) so the purchase doesn't
 * feel "wasted" — Google encourages giving consumable IAPs at least a token reward.
 */
data class DeveloperTip(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val priceDisplay: String,
    val priceValue: Double,
    val gemBonus: Int  // gems given as a thank-you
)

val DEVELOPER_TIPS = listOf(
    DeveloperTip(
        id = "tip_developers",
        name = "Buy Roy & Abbie a Coffee",
        emoji = "☕",
        description = "Show your support for the developers. Includes 100 gems!",
        priceDisplay = "£5.00",
        priceValue = 5.00,
        gemBonus = 100
    )
)
