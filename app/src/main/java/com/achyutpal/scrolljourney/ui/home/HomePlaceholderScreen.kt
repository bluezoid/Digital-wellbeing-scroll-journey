package com.achyutpal.scrolljourney.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DailyUsageSummary
import com.achyutpal.scrolljourney.domain.usecase.TodayUsageResult
import com.achyutpal.scrolljourney.ui.common.formatMinutes
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import com.achyutpal.scrolljourney.util.openUsageAccessSettings
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * TEMPORARY post-onboarding screen (Phases 3–4): today's minutes per tracked app and the
 * last 7 saved days, refreshed (and synced) on every resume. Replaced by the Dashboard in Phase 8.
 */
@Composable
fun HomePlaceholderScreen(
    onChangeApps: () -> Unit,
    viewModel: HomePlaceholderViewModel = viewModel(factory = HomePlaceholderViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentDays by viewModel.recentDays.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-read usage whenever the user comes back (e.g. after scrolling Instagram, or from Settings).
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    HomePlaceholderContent(
        state = state,
        recentDays = recentDays,
        onRefresh = viewModel::refresh,
        onOpenSettings = { context.openUsageAccessSettings() },
        onChangeApps = onChangeApps,
    )
}

@Composable
private fun HomePlaceholderContent(
    state: HomeUiState,
    recentDays: List<DailyRecord>,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeApps: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.home_today_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            state.updatedAtMillis?.let {
                Text(
                    text = stringResource(
                        R.string.home_updated_at,
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }

            when (val result = state.result) {
                null -> Unit
                is TodayUsageResult.AccessDenied -> AccessDeniedCard(onOpenSettings)
                is TodayUsageResult.Failure -> Text(
                    text = stringResource(R.string.home_usage_error, result.message),
                    color = MaterialTheme.colorScheme.error,
                )
                is TodayUsageResult.Success -> UsageCard(result.summary)
            }

            if (recentDays.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                RecentDaysCard(recentDays)
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_refresh))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onChangeApps, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_change_apps))
            }
        }
    }
}

@Composable
private fun UsageCard(summary: DailyUsageSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = formatMinutes(summary.totalMinutes),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.home_total_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!summary.hasAnyUsage) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_no_usage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            summary.apps.forEach { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
}

@Composable
private fun RecentDaysCard(days: List<DailyRecord>) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE d MMM") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_recent_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            days.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = day.date.format(dateFormat) +
                            if (day.finalized) "" else " · " + stringResource(R.string.home_recent_live),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMinutes(day.totalMinutes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessDeniedCard(onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
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
}

@Preview
@Composable
private fun HomePreview() {
    ScrollJourneyTheme(darkTheme = true) {
        HomePlaceholderContent(
            recentDays = listOf(
                DailyRecord(LocalDate.of(2026, 9, 27), 49),
                DailyRecord(LocalDate.of(2026, 9, 26), 72, finalized = true),
            ),
            state = HomeUiState(
                isLoading = false,
                result = TodayUsageResult.Success(
                    DailyUsageSummary(
                        LocalDate.of(2026, 9, 27),
                        listOf(
                            AppUsage("com.instagram.android", "Instagram", 32 * 60_000L),
                            AppUsage("com.google.android.youtube", "YouTube", 17 * 60_000L),
                        ),
                    ),
                ),
                updatedAtMillis = 0L,
            ),
            onRefresh = {},
            onOpenSettings = {},
            onChangeApps = {},
        )
    }
}
