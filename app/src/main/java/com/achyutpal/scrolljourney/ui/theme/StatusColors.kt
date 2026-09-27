package com.achyutpal.scrolljourney.ui.theme

import androidx.compose.ui.graphics.Color
import com.achyutpal.scrolljourney.domain.model.DayStatus

val StatusGreen = Color(0xFF4ADE80)
val StatusYellow = Color(0xFFFFC247)
val StatusRed = Color(0xFFFF6B6B)

fun DayStatus.color(): Color = when (this) {
    DayStatus.GREEN -> StatusGreen
    DayStatus.YELLOW -> StatusYellow
    DayStatus.RED -> StatusRed
}
