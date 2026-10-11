package com.achyutpal.scrolljourney.ui.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * Glance widgets: Compose-style code that Glance turns into RemoteViews, which the launcher draws.
 * They read saved data only (fast); the app's syncs and background jobs keep that data fresh and
 * call [WidgetRefresher.refresh] afterwards.
 */
private suspend fun loadSnapshot(context: Context): WidgetSnapshot {
    val container = (context.applicationContext as ScrollJourneyApp).container
    return WidgetSnapshot.build(
        settings = container.settingsRepository.getSettings(),
        records = container.historyRepository.observeRecentDays(STREAK_LOOKBACK_DAYS).first(),
        today = LocalDate.now(),
        hasUsageAccess = container.usageAccessRepository.checkUsageAccess() == UsageAccessState.GRANTED,
        now = LocalTime.now(),
    )
}

private const val STREAK_LOOKBACK_DAYS = 400

/*
 * SizeMode.Responsive: Glance lays the widget out once per listed size and the launcher picks the
 * best fit as the user resizes it, so bigger widgets show more instead of leaving empty space.
 */
internal val SMALL_TODAY = DpSize(110.dp, 40.dp)
internal val TALL_TODAY = DpSize(110.dp, 90.dp)
internal val SMALL_JOURNEY = DpSize(180.dp, 110.dp)
internal val WIDE_JOURNEY = DpSize(250.dp, 110.dp)
internal val TALL_JOURNEY = DpSize(180.dp, 160.dp)

/** Small 2×1 widget. */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL_TODAY, TALL_TODAY))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        provideContent { TodayWidgetContent(snapshot) }
    }
}

/** Medium 3×2 / 4×2 widget. */
class JourneyWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL_JOURNEY, WIDE_JOURNEY, TALL_JOURNEY, DpSize(250.dp, 160.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        provideContent { JourneyWidgetContent(snapshot) }
    }
}

/** Wide 4×1 widget: the last 7 days. */
class WeekWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        provideContent { WeekWidgetContent(snapshot) }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

class JourneyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = JourneyWidget()
}

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekWidget()
}

/** Redraws every placed widget with the latest saved data (cheap; no-op if none are placed). */
object WidgetRefresher {
    suspend fun refresh(context: Context) {
        TodayWidget().updateAll(context)
        JourneyWidget().updateAll(context)
        WeekWidget().updateAll(context)
    }
}
