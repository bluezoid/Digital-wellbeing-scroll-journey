package com.achyutpal.scrolljourney.ui.dailylimit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.calculator.DailyLimitRules
import com.achyutpal.scrolljourney.domain.calculator.LimitEvaluator
import com.achyutpal.scrolljourney.ui.common.formatMinutes
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import com.achyutpal.scrolljourney.ui.theme.StatusGreen
import com.achyutpal.scrolljourney.ui.theme.StatusRed
import com.achyutpal.scrolljourney.ui.theme.StatusYellow

private val presets = listOf(30, 60, 90, 120)

/** Slider + presets for the combined daily limit, with a live explanation of green/yellow/red. */
@Composable
fun DailyLimitScreen(
    isOnboarding: Boolean,
    onDone: () -> Unit,
    viewModel: DailyLimitViewModel = viewModel(factory = DailyLimitViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isDone) {
        if (state.isDone) onDone()
    }

    DailyLimitContent(
        state = state,
        isOnboarding = isOnboarding,
        onLimitChange = viewModel::onLimitChange,
        onSave = viewModel::onSave,
    )
}

@Composable
private fun DailyLimitContent(
    state: DailyLimitUiState,
    isOnboarding: Boolean,
    onLimitChange: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val limit = state.limitMinutes
    Scaffold(
        bottomBar = {
            Button(
                onClick = onSave,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(52.dp),
            ) {
                Text(stringResource(if (isOnboarding) R.string.limit_start_journey else R.string.limit_save))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.limit_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.limit_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            Text(
                text = formatMinutes(limit),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = stringResource(R.string.limit_per_day),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))

            Slider(
                value = limit.toFloat(),
                onValueChange = { onLimitChange(it.toInt()) },
                valueRange = DailyLimitRules.MIN_MINUTES.toFloat()..DailyLimitRules.MAX_MINUTES.toFloat(),
                // steps = number of stops BETWEEN the ends: (240 − 15) / 5 − 1 = 44
                steps = (DailyLimitRules.MAX_MINUTES - DailyLimitRules.MIN_MINUTES) / DailyLimitRules.STEP_MINUTES - 1,
                enabled = !state.isLoading,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMinutes(DailyLimitRules.MIN_MINUTES), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMinutes(DailyLimitRules.MAX_MINUTES), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = limit == preset,
                        onClick = { onLimitChange(preset) },
                        label = { Text(formatMinutes(preset)) },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val yellowMax = LimitEvaluator.yellowMaxMinutes(limit)
                    LegendRow(StatusGreen, stringResource(R.string.limit_legend_green, formatMinutes(limit)))
                    LegendRow(StatusYellow, stringResource(R.string.limit_legend_yellow, formatMinutes(yellowMax)))
                    LegendRow(StatusRed, stringResource(R.string.limit_legend_red, formatMinutes(yellowMax)))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview
@Composable
private fun DailyLimitPreview() {
    ScrollJourneyTheme(darkTheme = true) {
        DailyLimitContent(
            state = DailyLimitUiState(isLoading = false, limitMinutes = 60),
            isOnboarding = true,
            onLimitChange = {},
            onSave = {},
        )
    }
}
