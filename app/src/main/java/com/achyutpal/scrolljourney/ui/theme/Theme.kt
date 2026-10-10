package com.achyutpal.scrolljourney.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColors = darkColorScheme(
    primary = RocketOrange,
    onPrimary = SpaceNavy,
    secondary = OrbitTeal,
    onSecondary = SpaceNavy,
    tertiary = FlameYellow,
    background = SpaceNavy,
    onBackground = StarWhite,
    surface = SpaceSurface,
    onSurface = StarWhite,
    surfaceVariant = SpaceSurfaceHigh,
    onSurfaceVariant = MutedLavender,
    error = AlertRed,
)

private val LightColors = lightColorScheme(
    primary = RocketOrangeDeep,
    onPrimary = DaySurface,
    secondary = OrbitTealDeep,
    onSecondary = DaySurface,
    tertiary = RocketOrange,
    background = DaySky,
    onBackground = InkNavy,
    surface = DaySurface,
    onSurface = InkNavy,
    surfaceVariant = DaySurfaceHigh,
    onSurfaceVariant = InkMuted,
    error = AlertRedDeep,
)

@Composable
fun ScrollJourneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBrightStatusColors provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
