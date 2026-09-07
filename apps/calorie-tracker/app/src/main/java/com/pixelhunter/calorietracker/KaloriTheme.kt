package com.pixelhunter.calorietracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val KaloriGreen = Color(0xFF00E676)
val KaloriGreenSoft = Color(0xFF21F38A)
val KaloriBackground = Color(0xFF050807)
val KaloriSurface = Color(0xFF101715)
val KaloriSurfaceAlt = Color(0xFF121A17)
val KaloriBorder = Color(0xFF26312E)
val KaloriDialog = Color(0xFF1A1B22)
val KaloriInput = Color(0xFF151821)
val KaloriText = Color(0xFFF5F7F6)
val KaloriMuted = Color(0xFFA7B0AB)
val KaloriBlue = Color(0xFF39A9FF)
val KaloriYellow = Color(0xFFFFB020)
val KaloriDanger = Color(0xFFFF5A5F)

private val KaloriDarkColors = darkColorScheme(
    primary = KaloriGreen,
    onPrimary = Color(0xFF001B0D),
    primaryContainer = Color(0xFF0A3A24),
    onPrimaryContainer = Color(0xFFB8FFD2),
    secondary = KaloriBlue,
    tertiary = KaloriYellow,
    background = KaloriBackground,
    onBackground = KaloriText,
    surface = KaloriSurface,
    onSurface = KaloriText,
    surfaceVariant = KaloriSurfaceAlt,
    onSurfaceVariant = KaloriMuted,
    surfaceContainerHigh = KaloriDialog,
    surfaceContainer = KaloriSurfaceAlt,
    outline = KaloriBorder,
    error = KaloriDanger
)

private val KaloriShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun KaloriDarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KaloriDarkColors,
        shapes = KaloriShapes,
        content = content
    )
}
