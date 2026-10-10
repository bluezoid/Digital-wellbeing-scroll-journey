package com.achyutpal.scrolljourney.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.achyutpal.scrolljourney.domain.model.DayStatus

/**
 * Day-status colours that stay readable on both themes: bright on dark backgrounds, deeper on
 * light ones (≥ 4.5:1 contrast for text). Screens with an always-dark background (the 3D Explore
 * screen) provide `true` themselves.
 */
internal val LocalBrightStatusColors = staticCompositionLocalOf { true }

val StatusGreen: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalBrightStatusColors.current) Color(0xFF4ADE80) else Color(0xFF15803D)

val StatusYellow: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalBrightStatusColors.current) Color(0xFFFFC247) else Color(0xFFB45309)

val StatusRed: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalBrightStatusColors.current) Color(0xFFFF6B6B) else Color(0xFFDC2626)

@Composable
@ReadOnlyComposable
fun DayStatus.color(): Color = when (this) {
    DayStatus.GREEN -> StatusGreen
    DayStatus.YELLOW -> StatusYellow
    DayStatus.RED -> StatusRed
}
