package com.achyutpal.scrolljourney.ui.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.achyutpal.scrolljourney.MainActivity
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.ui.common.emoji
import com.achyutpal.scrolljourney.ui.common.formatDistance
import com.achyutpal.scrolljourney.ui.common.formatMinutes
import com.achyutpal.scrolljourney.ui.common.nameRes
import java.time.format.DateTimeFormatter
import java.util.Locale

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

private fun text(color: Color, size: Int, bold: Boolean = false, align: TextAlign = TextAlign.Start) = TextStyle(
    color = ColorProvider(color),
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    textAlign = align,
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

/** "Usage Access is off" / "Open the app to start", shown instead of numbers; null when READY. */
private fun noticeFor(context: Context, snapshot: WidgetSnapshot): String? = when (snapshot.state) {
    WidgetSnapshot.State.READY -> null
    WidgetSnapshot.State.NOT_SET_UP -> context.getString(R.string.widget_not_set_up)
    WidgetSnapshot.State.NO_ACCESS -> context.getString(R.string.widget_no_access)
}

@Composable
private fun Dot(color: Color, size: Int) {
    Box(modifier = GlanceModifier.size(size.dp).background(color).cornerRadius((size / 2).dp), content = {})
}

/** Today's progress ring (painted as a bitmap) with a short label in the middle. */
@Composable
private fun TodayRing(snapshot: WidgetSnapshot, size: Int, label: String, labelSize: Int) {
    Box(modifier = GlanceModifier.size(size.dp), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(WidgetRing.bitmap(snapshot.limitFraction, snapshot.status.widgetColor(), Track)),
            contentDescription = null,
            modifier = GlanceModifier.size(size.dp),
        )
        Text(label, style = text(TextMain, labelSize, bold = true, align = TextAlign.Center), maxLines = 1)
    }
}

/** "1h 15m left" or "20m over". */
private fun leftLine(context: Context, snapshot: WidgetSnapshot): String =
    if (snapshot.minutesOver > 0) context.getString(R.string.widget_over, formatMinutes(snapshot.minutesOver))
    else context.getString(R.string.widget_left, formatMinutes(snapshot.minutesLeft))

private fun percentLabel(snapshot: WidgetSnapshot): String =
    if (snapshot.limitMinutes <= 0) "–" else "${(snapshot.todayMinutes * 100 / snapshot.limitMinutes).coerceAtMost(999)}%"

/** Small 2×1: ring, today's minutes and what's left. Taller sizes get a bigger ring and the status. */
@Composable
fun TodayWidgetContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val tall = LocalSize.current.height >= TALL_TODAY.height
    WidgetCard {
        noticeFor(context, snapshot)?.let { Text(it, style = text(TextMuted, 13)); return@WidgetCard }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TodayRing(snapshot, size = if (tall) 64 else 40, label = percentLabel(snapshot), labelSize = if (tall) 14 else 10)
            Spacer(GlanceModifier.width(10.dp))
            Column {
                if (tall) Text(context.getString(snapshot.status.labelRes()), style = text(snapshot.status.widgetColor(), 12, bold = true), maxLines = 1)
                Text(formatMinutes(snapshot.todayMinutes), style = text(snapshot.status.widgetColor(), if (tall) 24 else 20, bold = true), maxLines = 1)
                Text(leftLine(context, snapshot), style = text(TextMuted, 12), maxLines = 1)
            }
        }
    }
}

/**
 * Journey widget (3×2 / 4×2 and bigger): today with a ring, the rocket's current leg with a fuel
 * bar, and when there's room the last 7 days, the streak and distance.
 */
@Composable
fun JourneyWidgetContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val size = LocalSize.current
    val wide = size.width >= WIDE_JOURNEY.width
    val tall = size.height >= TALL_JOURNEY.height
    WidgetCard {
        noticeFor(context, snapshot)?.let { Text(it, style = text(TextMuted, 13)); return@WidgetCard }
        Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TodayRing(snapshot, size = if (tall) 60 else 50, label = formatMinutes(snapshot.todayMinutes), labelSize = if (tall) 13 else 11)
                Spacer(GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    StatusPill(context, snapshot.status)
                    Spacer(GlanceModifier.height(3.dp))
                    Text(leftLine(context, snapshot), style = text(TextMain, 15, bold = true), maxLines = 1)
                    Text(context.getString(R.string.widget_of_limit, formatMinutes(snapshot.limitMinutes)), style = text(TextMuted, 12), maxLines = 1)
                }
                if (wide && !tall) WeekStrip(context, snapshot, dotSize = 10, letters = false)
            }
            Spacer(GlanceModifier.height(if (tall) 10.dp else 8.dp))
            if (tall) {
                WeekStrip(context, snapshot, dotSize = 12, letters = true, modifier = GlanceModifier.fillMaxWidth())
                Spacer(GlanceModifier.height(10.dp))
            }
            LegRow(context, snapshot)
            Spacer(GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = snapshot.rocket.legProgress,
                modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                color = ColorProvider(Orange),
                backgroundColor = ColorProvider(Track),
            )
            Spacer(GlanceModifier.height(5.dp))
            FooterRow(context, snapshot, showUpdated = tall || wide)
        }
    }
}

/**
 * Week widget (4×1): the last 7 days as coloured dots with weekday letters, plus the streak and
 * today's minutes.
 */
@Composable
fun WeekWidgetContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    WidgetCard {
        noticeFor(context, snapshot)?.let { Text(it, style = text(TextMuted, 13)); return@WidgetCard }
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WeekStrip(context, snapshot, dotSize = 14, letters = true, modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(context.getString(R.string.widget_streak_short, snapshot.streak), style = text(Orange, 18, bold = true), maxLines = 1)
                Text(
                    context.getString(R.string.widget_today_short, formatMinutes(snapshot.todayMinutes)),
                    style = text(snapshot.status.widgetColor(), 12, bold = true),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(context: Context, status: DayStatus) {
    Box(
        modifier = GlanceModifier
            .background(status.widgetColor().copy(alpha = 0.18f))
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(context.getString(status.labelRes()), style = text(status.widgetColor(), 11, bold = true), maxLines = 1)
    }
}

/** 7 dots, oldest left; today's is outlined in white. Days with no data are empty track dots. */
@Composable
private fun WeekStrip(context: Context, snapshot: WidgetSnapshot, dotSize: Int, letters: Boolean, modifier: GlanceModifier = GlanceModifier) {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        snapshot.week.forEachIndexed { i, day ->
            val isToday = i == snapshot.week.lastIndex
            Column(
                modifier = if (letters) GlanceModifier.defaultWeight() else GlanceModifier.padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (letters) {
                    Text(
                        day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale),
                        style = text(if (isToday) TextMain else TextMuted, 10, bold = isToday, align = TextAlign.Center),
                    )
                    Spacer(GlanceModifier.height(3.dp))
                }
                val color = day.status?.widgetColor() ?: Track
                if (isToday) {
                    // A white ring around today's dot: a white dot with the coloured one on top.
                    Box(
                        modifier = GlanceModifier.size((dotSize + 4).dp).background(TextMain).cornerRadius(((dotSize + 4) / 2).dp),
                        contentAlignment = Alignment.Center,
                    ) { Dot(color, dotSize) }
                } else {
                    Dot(color, dotSize)
                }
            }
        }
    }
}

/** "🌙 Moon → 🔴 Mars" on the left, "⛽ 6/12" on the right (or "journey complete"). */
@Composable
private fun LegRow(context: Context, snapshot: WidgetSnapshot) {
    val rocket = snapshot.rocket
    val from = rocket.highestReachedPlanet
    val to = rocket.nextPlanet
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val leg = if (to == null) {
            "${from.emoji()} ${context.getString(R.string.rocket_journey_complete)}"
        } else {
            context.getString(
                R.string.widget_leg,
                "${from.emoji()} ${context.getString(from.nameRes())}",
                "${to.emoji()} ${context.getString(to.nameRes())}",
            )
        }
        Text(leg, style = text(Orange, 13, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
        if (to != null) {
            Text(context.getString(R.string.widget_fuel, rocket.currentFuel, to.fuelRequired), style = text(TextMuted, 12, bold = true), maxLines = 1)
        }
    }
}

/** "🔥 5-day green streak · 137 m" and, when there's room, "↻ 10:52" on the right. */
@Composable
private fun FooterRow(context: Context, snapshot: WidgetSnapshot, showUpdated: Boolean) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val parts = buildList {
            add(streakLine(context, snapshot.streak))
            if (snapshot.todayDistance > 0) add(context.getString(R.string.widget_distance_today, formatDistance(snapshot.todayDistance)))
        }
        Text(parts.joinToString(" · "), style = text(TextMuted, 11), maxLines = 1, modifier = GlanceModifier.defaultWeight())
        val updated = snapshot.updatedAt
        if (showUpdated && updated != null) {
            val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
            Text(context.getString(R.string.widget_updated, updated.format(DateTimeFormatter.ofPattern(pattern))), style = text(TextMuted, 10), maxLines = 1)
        }
    }
}

private fun streakLine(context: Context, streak: Int): String =
    if (streak > 0) context.resources.getQuantityString(R.plurals.widget_streak, streak, streak)
    else context.getString(R.string.widget_no_streak)
