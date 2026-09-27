package com.achyutpal.scrolljourney.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.calculator.Comparison
import com.achyutpal.scrolljourney.domain.calculator.Landmark

@Composable
fun comparisonText(comparison: Comparison): String {
    val name = stringResource(comparison.landmark.nameRes())
    val times = formatTimes(comparison.times)
    return when {
        comparison.times < 1 -> stringResource(R.string.distance_fraction_of, formatTimes(comparison.times * 100), name)
        times == "1" -> stringResource(R.string.distance_about_one, name) // "≈ 1×" reads oddly
        else -> stringResource(R.string.distance_times, times, name)
    }
}

@StringRes
fun Landmark.nameRes(): Int = when (this) {
    Landmark.QUTUB_MINAR -> R.string.landmark_qutub_minar
    Landmark.GREAT_PYRAMID -> R.string.landmark_great_pyramid
    Landmark.EIFFEL_TOWER -> R.string.landmark_eiffel_tower
    Landmark.BURJ_KHALIFA -> R.string.landmark_burj_khalifa
    Landmark.MOUNT_EVEREST -> R.string.landmark_mount_everest
}
