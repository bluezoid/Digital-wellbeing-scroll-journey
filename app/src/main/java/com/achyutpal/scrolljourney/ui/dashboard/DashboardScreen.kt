package com.achyutpal.scrolljourney.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.calculator.DistanceComparisons
import com.achyutpal.scrolljourney.domain.calculator.DistanceConfig
import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyUsageSummary
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.Planet
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.ui.common.StatusDot
import com.achyutpal.scrolljourney.ui.common.StatusLabel
import com.achyutpal.scrolljourney.ui.common.comparisonText
import com.achyutpal.scrolljourney.ui.common.formatDistance
import com.achyutpal.scrolljourney.ui.common.formatMinutes
import com.achyutpal.scrolljourney.ui.journey.RocketJourneyView
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import com.achyutpal.scrolljourney.ui.theme.color
import com.achyutpal.scrolljourney.util.openUsageAccessSettings
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/** The main screen after onboarding. Syncs usage on every resume. */
@Composable
fun DashboardScreen(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    DashboardContent(
        state = state,
        onRefresh = viewModel::refresh,
        onOpenUsageAccess = { context.openUsageAccessSettings() },
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_title)) },
                            onClick = { menuOpen = false; onOpenHistory() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_title)) },
                            onClick = { menuOpen = false; onOpenSettings() },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isLoading && state.today == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (state.accessDenied) AccessDeniedCard(onOpenUsageAccess)
            state.errorMessage?.let {
                Text(stringResource(R.string.home_usage_error, it), color = MaterialTheme.colorScheme.error)
            }

            RocketJourneyView(rocket = state.rocket, journeyStartDate = state.journeyStartDate)

            state.today?.let { today ->
                TodayCard(today, state.limitMinutes, state.status)
            }

            StreakWeekCard(streak = state.currentStreak, week = state.week, onOpenHistory = onOpenHistory)

            if (state.today != null) DistanceCard(state.distanceCentimetres)

            state.updatedAtMillis?.let {
                Text(
                    text = stringResource(
                        R.string.home_updated_at,
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TodayCard(summary: DailyUsageSummary, limitMinutes: Int, status: DayStatus?) {
    DashboardCard {
        Text(
            text = stringResource(R.string.home_today_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        status?.let { StatusLabel(it, suffix = stringResource(R.string.status_so_far)) }
        Spacer(Modifier.height(8.dp))
        val accent = status?.color() ?: MaterialTheme.colorScheme.primary
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatMinutes(summary.totalMinutes),
                style = MaterialTheme.typography.displaySmall,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_limit_of, formatMinutes(limitMinutes)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { (summary.totalMinutes.toFloat() / limitMinutes).coerceIn(0f, 1f) },
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
        if (!summary.hasAnyUsage) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_no_usage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        summary.apps.forEach { app ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(app.appName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = formatMinutes(app.minutes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun StreakWeekCard(streak: Int, week: List<WeekDay>, onOpenHistory: () -> Unit) {
    DashboardCard(onClick = onOpenHistory) {
        Text(
            text = if (streak > 0) {
                pluralStringResource(R.plurals.streak_days, streak, streak)
            } else {
                stringResource(R.string.streak_none)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(6.dp))
                    StatusDot(day.status, size = 14.dp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.history_open) + " ›",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun DistanceCard(distanceCentimetres: Long) {
    DashboardCard {
        Text(
            text = stringResource(R.string.distance_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "📏 " + formatDistance(distanceCentimetres),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
        )
        DistanceComparisons.compare(distanceCentimetres)?.let {
            Spacer(Modifier.height(4.dp))
            Text(comparisonText(it), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.distance_footnote, formatDistance(DistanceConfig.DEFAULT_CENTIMETRES_PER_MINUTE)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccessDeniedCard(onOpenSettings: () -> Unit) {
    DashboardCard {
        Text(
            text = "⚠️ " + stringResource(R.string.home_revoked_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_revoked_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_open_settings))
        }
    }
}

@Composable
private fun DashboardCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        Card(onClick = onClick, colors = colors, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) { content() }
        }
    } else {
        Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) { content() }
        }
    }
}

@Preview(heightDp = 1400)
@Composable
private fun DashboardPreview() {
    val today = LocalDate.of(2026, 9, 27)
    ScrollJourneyTheme(darkTheme = true) {
        DashboardContent(
            state = DashboardUiState(
                isLoading = false,
                today = DailyUsageSummary(
                    today,
                    listOf(
                        AppUsage("com.instagram.android", "Instagram", 43 * 60_000L),
                        AppUsage("com.google.android.youtube", "YouTube", 7 * 60_000L),
                    ),
                ),
                limitMinutes = 60,
                status = DayStatus.GREEN,
                distanceCentimetres = 5_000,
                rocket = RocketState(currentFuel = 5, lifetimeFuel = 6, highestReachedPlanet = Planet.MOON),
                journeyStartDate = today.minusDays(10),
                currentStreak = 3,
                week = (6 downTo 0).map { WeekDay(today.minusDays(it.toLong()), DayStatus.GREEN, it == 0) },
                updatedAtMillis = 0L,
            ),
            onRefresh = {},
            onOpenUsageAccess = {},
            onOpenHistory = {},
            onOpenSettings = {},
        )
    }
}
