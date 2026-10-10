package com.achyutpal.scrolljourney.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.achyutpal.scrolljourney.MainActivity
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.ui.common.emoji
import com.achyutpal.scrolljourney.ui.common.formatMinutes
import com.achyutpal.scrolljourney.ui.common.nameRes

// The app's palette (widgets can't use the Compose MaterialTheme).
private val Background = Color(0xF20B1026)
private val TextMain = Color(0xFFF4F4FA)
private val TextMuted = Color(0xFFB4B8D6)
private val Track = Color(0xFF1F2750)
private val Orange = Color(0xFFFF8A3D)

private fun DayStatus.widgetColor(): Color = when (this) {
    DayStatus.GREEN -> Color(0xFF4ADE80)
    DayStatus.YELLOW -> Color(0xFFFFC247)
    DayStatus.RED -> Color(0xFFFF6B6B)
}

private fun DayStatus.labelRes(): Int = when (this) {
    DayStatus.GREEN -> R.string.widget_status_green
    DayStatus.YELLOW -> R.string.widget_status_yellow
    DayStatus.RED -> R.string.widget_status_red
}

private fun text(color: Color, size: Int, bold: Boolean = false) = TextStyle(
    color = ColorProvider(color),
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
)

/** Rounded card that opens the app when tapped. */
@Composable
private fun WidgetCard(content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Background)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}

/** "Usage Access is off" / "Open the app to start": shown instead of numbers. */
@Composable
private fun Notice(snapshot: WidgetSnapshot): Boolean {
    val context = LocalContext.current
    val message = when (snapshot.state) {
        WidgetSnapshot.State.READY -> return false
        WidgetSnapshot.State.NOT_SET_UP -> context.getString(R.string.widget_not_set_up)
        WidgetSnapshot.State.NO_ACCESS -> context.getString(R.string.widget_no_access)
    }
    Text(message, style = text(TextMuted, 13))
    return true
}

@Composable
private fun StatusDot(status: DayStatus, size: Int = 12) {
    Box(
        modifier = GlanceModifier.size(size.dp).background(status.widgetColor()).cornerRadius((size / 2).dp),
        content = {},
    )
}

/** Small 2×1: status dot, today's minutes and the limit. */
@Composable
fun TodayWidgetContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    WidgetCard {
        if (Notice(snapshot)) return@WidgetCard
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(snapshot.status)
            Spacer(GlanceModifier.width(10.dp))
            Column {
                Text(formatMinutes(snapshot.todayMinutes), style = text(snapshot.status.widgetColor(), 22, bold = true))
                Text(
                    context.getString(R.string.widget_of_limit, formatMinutes(snapshot.limitMinutes)),
                    style = text(TextMuted, 12),
                )
            }
        }
    }
}

/** Medium 4×2: today with a progress bar, the rocket's current leg and the green streak. */
@Composable
fun JourneyWidgetContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    WidgetCard {
        if (Notice(snapshot)) return@WidgetCard
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(snapshot.status, size = 10)
                Spacer(GlanceModifier.width(8.dp))
                Text(context.getString(snapshot.status.labelRes()), style = text(snapshot.status.widgetColor(), 13, bold = true))
            }
            Spacer(GlanceModifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatMinutes(snapshot.todayMinutes), style = text(TextMain, 24, bold = true))
                Spacer(GlanceModifier.width(6.dp))
                Text(context.getString(R.string.widget_of_limit, formatMinutes(snapshot.limitMinutes)), style = text(TextMuted, 13))
            }
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = snapshot.limitFraction,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = ColorProvider(snapshot.status.widgetColor()),
                backgroundColor = ColorProvider(Track),
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(journeyLine(context, snapshot), style = text(Orange, 13, bold = true), maxLines = 1)
            Text(streakLine(context, snapshot.streak), style = text(TextMuted, 12), maxLines = 1)
        }
    }
}

private fun journeyLine(context: Context, snapshot: WidgetSnapshot): String {
    val rocket = snapshot.rocket
    val from = rocket.highestReachedPlanet
    val to = rocket.nextPlanet ?: return "${from.emoji()} ${context.getString(R.string.rocket_journey_complete)}"
    return context.getString(
        R.string.widget_journey_line,
        "${from.emoji()} ${context.getString(from.nameRes())}",
        "${to.emoji()} ${context.getString(to.nameRes())}",
        rocket.currentFuel,
        to.fuelRequired,
    )
}

private fun streakLine(context: Context, streak: Int): String =
    if (streak > 0) context.resources.getQuantityString(R.plurals.widget_streak, streak, streak)
    else context.getString(R.string.widget_no_streak)
