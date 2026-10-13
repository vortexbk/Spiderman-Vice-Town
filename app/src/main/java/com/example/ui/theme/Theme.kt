package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SpiderColorScheme = darkColorScheme(
    primary = SpiderCrimson,
    onPrimary = TextPureWhite,
    primaryContainer = SpiderCrimsonDark,
    onPrimaryContainer = TextPureWhite,
    secondary = WebCyan,
    onSecondary = SpiderMidnight,
    secondaryContainer = SpiderSurfaceElevated,
    onSecondaryContainer = WebCyanSoft,
    tertiary = SpiderSenseGold,
    onTertiary = SpiderMidnight,
    background = SpiderMidnight,
    onBackground = TextPureWhite,
    surface = SpiderDeepNavy,
    onSurface = TextPureWhite,
    surfaceVariant = SpiderSurfaceCard,
    onSurfaceVariant = TextSecondaryIce,
    outline = WebCobalt
)

val SpiderShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SpiderCityTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpiderColorScheme,
        typography = Typography,
        shapes = SpiderShapes,
        content = content
    )
}
