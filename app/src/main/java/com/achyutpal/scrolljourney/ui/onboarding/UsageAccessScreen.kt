package com.achyutpal.scrolljourney.ui.onboarding

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
import androidx.compose.runtime.LaunchedEffect
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
 * Explains Usage Access and sends the user to Settings.
 *
 * Flow: tap button → Android Settings → user flips toggle → presses Back →
 * this screen RESUMES → we re-check → if granted, [onGranted] moves on automatically.
 */
@Composable
fun UsageAccessScreen(
    onGranted: () -> Unit,
    viewModel: UsageAccessViewModel = viewModel(factory = UsageAccessViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-check every time the screen comes back to the foreground (e.g. returning from Settings).
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(state) {
        if (state == UsageAccessState.GRANTED) onGranted()
    }

    UsageAccessContent(
        state = state,
        onOpenSettings = { context.openUsageAccessSettings() },
    )
}

@Composable
// internal (not private) so on-device UI tests can render it with fake state.
internal fun UsageAccessContent(
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
            Text(text = "📊", fontSize = 56.sp)
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.usage_access_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.usage_access_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        R.string.usage_access_step1,
                        R.string.usage_access_step2,
                        R.string.usage_access_step3,
                        R.string.usage_access_step4,
                    ).forEach { step ->
                        Text(
                            text = stringResource(step),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            if (state == UsageAccessState.REVOKED) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.usage_access_not_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.action_open_settings))
            }
        }
    }
}

@Preview
@Composable
private fun UsageAccessContentPreview() {
    ScrollJourneyTheme(darkTheme = true) {
        UsageAccessContent(state = UsageAccessState.REVOKED, onOpenSettings = {})
    }
}
