package com.achyutpal.scrolljourney.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.ui.theme.color

@StringRes
fun DayStatus.labelRes(): Int = when (this) {
    DayStatus.GREEN -> R.string.status_green
    DayStatus.YELLOW -> R.string.status_yellow
    DayStatus.RED -> R.string.status_red
}

/** Coloured dot for a day; grey when there's no status (no data). */
@Composable
fun StatusDot(status: DayStatus?, size: Dp = 10.dp) {
    Box(
        Modifier
            .size(size)
            .background(status?.color() ?: MaterialTheme.colorScheme.surfaceVariant, CircleShape),
    )
}

@Composable
fun StatusLabel(status: DayStatus, suffix: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(status)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(status.labelRes()) + suffix,
            style = MaterialTheme.typography.labelLarge,
            color = status.color(),
        )
    }
}

fun formatFuel(delta: Int): String = when {
    delta > 0 -> "+$delta"
    delta < 0 -> "−${-delta}"
    else -> "0"
}
