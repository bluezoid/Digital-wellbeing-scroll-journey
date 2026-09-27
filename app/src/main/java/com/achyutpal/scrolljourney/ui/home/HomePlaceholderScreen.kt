package com.achyutpal.scrolljourney.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.ui.permission.UsageAccessViewModel
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import com.achyutpal.scrolljourney.util.openUsageAccessSettings

/**
 * TEMPORARY post-onboarding screen for Phase 1. It proves the permission round trip
 * and the "revoked later" warning. Replaced by App Picker (Phase 2) and Dashboard (Phase 8).
 */
@Composable
fun HomePlaceholderScreen(
    viewModel: UsageAccessViewModel = viewModel(factory = UsageAccessViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    HomePlaceholderContent(
        state = state,
        onOpenSettings = { context.openUsageAccessSettings() },
    )
}

@Composable
private fun HomePlaceholderContent(
    state: UsageAccessState,
    onOpenSettings: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            when (state) {
                UsageAccessState.UNKNOWN -> Text(
                    text = stringResource(R.string.home_checking),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                UsageAccessState.GRANTED -> {
                    Text(text = "✅", fontSize = 56.sp)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.home_granted_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.home_granted_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                UsageAccessState.REVOKED -> Card(
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
        }
    }
}

@Preview
@Composable
private fun HomeRevokedPreview() {
    ScrollJourneyTheme(darkTheme = true) {
        HomePlaceholderContent(state = UsageAccessState.REVOKED, onOpenSettings = {})
    }
}
