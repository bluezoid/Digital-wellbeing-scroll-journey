package com.achyutpal.scrolljourney.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import kotlinx.coroutines.flow.first
import java.time.LocalDate

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
    )
}

private const val STREAK_LOOKBACK_DAYS = 400

/** Small 2×1 widget. */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        provideContent { TodayWidgetContent(snapshot) }
    }
}

/** Medium 4×2 widget. */
class JourneyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        provideContent { JourneyWidgetContent(snapshot) }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

class JourneyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = JourneyWidget()
}

/** Redraws every placed widget with the latest saved data (cheap; no-op if none are placed). */
object WidgetRefresher {
    suspend fun refresh(context: Context) {
        TodayWidget().updateAll(context)
        JourneyWidget().updateAll(context)
    }
}
