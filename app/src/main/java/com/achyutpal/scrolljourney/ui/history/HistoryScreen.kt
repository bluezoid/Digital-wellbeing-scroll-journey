package com.achyutpal.scrolljourney.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.ui.common.StatusDot
import com.achyutpal.scrolljourney.ui.common.StatusLabel
import com.achyutpal.scrolljourney.ui.common.formatDistance
import com.achyutpal.scrolljourney.ui.common.formatFuel
import com.achyutpal.scrolljourney.ui.common.formatMinutes
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import com.achyutpal.scrolljourney.ui.theme.StatusGreen
import com.achyutpal.scrolljourney.ui.theme.StatusRed
import com.achyutpal.scrolljourney.ui.theme.StatusYellow
import com.achyutpal.scrolljourney.ui.theme.color
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        onBack = onBack,
        onPrevious = viewModel::previousMonth,
        onNext = viewModel::nextMonth,
        onSelect = viewModel::selectDay,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    state: HistoryUiState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
            StreakCard(current = state.currentStreak, best = state.bestStreak)
            CalendarCard(state, onPrevious, onNext, onSelect)
            state.selected?.let { DayDetailCard(it) }
                ?: state.selectedDate?.let { NoDataCard(it) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StreakCard(current: Int, best: Int) {
    HistoryCard {
        Row(Modifier.fillMaxWidth()) {
            StreakStat(Modifier.weight(1f), "🔥", current, stringResource(R.string.history_current_streak))
            StreakStat(Modifier.weight(1f), "🏆", best, stringResource(R.string.history_best_streak))
        }
    }
}

@Composable
private fun StreakStat(modifier: Modifier, emoji: String, days: Int, label: String) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$emoji $days",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CalendarCard(
    state: HistoryUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val monthFormat = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    HistoryCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, enabled = state.canGoPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.history_previous_month))
            }
            Text(
                text = state.month.format(monthFormat),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = onNext, enabled = state.canGoNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.history_next_month))
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        state.weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        cell.date?.let { date ->
                            DayCell(cell, selected = date == state.selectedDate, onClick = { onSelect(date) })
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            LegendItem(StatusGreen, stringResource(R.string.history_green_count, state.greenDays))
            LegendItem(StatusYellow, stringResource(R.string.history_yellow_count, state.yellowDays))
            LegendItem(StatusRed, stringResource(R.string.history_red_count, state.redDays))
        }
    }
}

/** A day circle: filled with its colour, outlined if selected, ringed if today, faded if future/no data. */
@Composable
private fun DayCell(cell: CalendarCell, selected: Boolean, onClick: () -> Unit) {
    val date = cell.date ?: return
    val fill = cell.status?.color()?.copy(alpha = 0.85f) ?: Color.Transparent
    val ring = when {
        selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
        cell.isToday -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        !cell.hasRecord -> BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        else -> null
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (cell.isFuture) 0.35f else 1f)
            .background(fill, CircleShape)
            .then(if (ring != null) Modifier.border(ring, CircleShape) else Modifier)
            .clickable(enabled = !cell.isFuture, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (cell.status != null) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayDetailCard(detail: DayDetail) {
    val record = detail.record
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }
    HistoryCard {
        Text(
            text = record.date.format(dateFormat) +
                if (record.finalized) "" else " · " + stringResource(R.string.home_recent_live),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        record.status?.let { StatusLabel(it) }
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatMinutes(record.totalMinutes) +
                (record.limitMinutes?.let { " · " + stringResource(R.string.home_recent_limit, formatMinutes(it)) } ?: ""),
            style = MaterialTheme.typography.headlineSmall,
            color = record.status?.color() ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = listOfNotNull(
                stringResource(R.string.history_distance, formatDistance(record.distance)),
                if (record.fuelApplied) stringResource(R.string.home_recent_fuel, formatFuel(record.fuelDelta)) else null,
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (detail.apps.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            detail.apps.forEach { app ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(app.appName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(formatMinutes(app.minutes), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun NoDataCard(date: LocalDate) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }
    HistoryCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(null)
            Spacer(Modifier.width(8.dp))
            Text(date.format(dateFormat), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.history_no_data), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistoryCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Preview(heightDp = 1100)
@Composable
private fun HistoryPreview() {
    val today = LocalDate.of(2026, 9, 27)
    val statuses = mapOf(
        today.minusDays(3) to DayStatus.RED,
        today.minusDays(2) to DayStatus.GREEN,
        today.minusDays(1) to DayStatus.GREEN,
        today to DayStatus.YELLOW,
    )
    ScrollJourneyTheme(darkTheme = true) {
        HistoryContent(
            state = HistoryUiState(
                month = YearMonth.from(today),
                weeks = MonthCalendar.build(YearMonth.from(today), statuses, today),
                currentStreak = 0,
                bestStreak = 3,
                greenDays = 2,
                yellowDays = 1,
                redDays = 1,
                selectedDate = today.minusDays(1),
                selected = DayDetail(
                    DailyRecord(today.minusDays(1), 21, status = DayStatus.GREEN, limitMinutes = 60, distance = 2_100, finalized = true),
                    listOf(DayAppLine("Instagram", 15), DayAppLine("YouTube", 6)),
                ),
            ),
            onBack = {},
            onPrevious = {},
            onNext = {},
            onSelect = {},
        )
    }
}
