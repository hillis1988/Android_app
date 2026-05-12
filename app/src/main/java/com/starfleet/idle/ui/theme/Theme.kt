package com.starfleet.idle.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SpaceBlack = Color(0xFF161B22) // Slightly lighter than original 0D1117
val DeepSpace = Color(0xFF1C2128)
val NebulaPurple = Color(0xFF8B5CF6)
val StarBlue = Color(0xFF58A6FF)
val CreditGold = Color(0xFFFBBF24)
val ShieldGreen = Color(0xFF34D399)
val AlertRed = Color(0xFFEF4444)
val TextPrimary = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF8B949E)
val CardBackground = Color(0xFF21262D) // Lighter than original 1C2128

private val DarkColorScheme = darkColorScheme(
    primary = StarBlue,
    secondary = NebulaPurple,
    tertiary = CreditGold,
    background = SpaceBlack,
    surface = DeepSpace,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary
)

@Composable
fun StarFleetIdleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
