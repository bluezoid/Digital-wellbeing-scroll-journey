package com.achyutpal.scrolljourney.ui.apppicker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.ui.common.BackTopBar
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme

/** Lets the user tick which installed apps count toward the daily limit. */
@Composable
fun AppPickerScreen(
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: AppPickerViewModel = viewModel(factory = AppPickerViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isDone) {
        if (state.isDone) onDone()
    }

    AppPickerContent(
        state = state,
        onBack = onBack,
        onToggle = viewModel::onToggle,
        onQueryChange = viewModel::onQueryChange,
        onSave = viewModel::onSave,
        loadIcon = viewModel::loadIcon,
    )
}

@Composable
// internal (not private) so on-device UI tests can render it with fake state.
internal fun AppPickerContent(
    state: AppPickerUiState,
    onBack: (() -> Unit)?,
    onToggle: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSave: () -> Unit,
    loadIcon: suspend (String) -> ImageBitmap?,
) {
    Scaffold(
        topBar = { onBack?.let { BackTopBar(it) } },
        bottomBar = {
            Button(
                onClick = onSave,
                enabled = state.canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.app_picker_continue, state.selectedCount))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(if (onBack == null) 24.dp else 0.dp))
            Text(
                text = stringResource(R.string.app_picker_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_picker_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.app_picker_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.rows.isEmpty() -> Text(
                    text = stringResource(R.string.app_picker_empty),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 24.dp),
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.visibleRows, key = { it.packageName }) { row ->
                        AppRowItem(row = row, onToggle = onToggle, loadIcon = loadIcon)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRowItem(
    row: AppRow,
    onToggle: (String) -> Unit,
    loadIcon: suspend (String) -> ImageBitmap?,
) {
    // produceState runs the suspend loader once per package and recomposes when the icon arrives.
    val icon by produceState<ImageBitmap?>(initialValue = null, row.packageName) {
        value = loadIcon(row.packageName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(row.packageName) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        val iconModifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
        icon?.let {
            Image(bitmap = it, contentDescription = null, modifier = iconModifier)
        } ?: Box(iconModifier.background(MaterialTheme.colorScheme.surfaceVariant))

        Spacer(Modifier.width(16.dp))
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Checkbox(checked = row.selected, onCheckedChange = { onToggle(row.packageName) })
    }
}

@Preview
@Composable
private fun AppPickerPreview() {
    ScrollJourneyTheme(darkTheme = true) {
        AppPickerContent(
            onBack = null,
            state = AppPickerUiState(
                isLoading = false,
                rows = listOf(
                    AppRow("com.instagram.android", "Instagram", true),
                    AppRow("com.google.android.youtube", "YouTube", true),
                    AppRow("com.android.chrome", "Chrome", false),
                ),
            ),
            onToggle = {},
            onQueryChange = {},
            onSave = {},
            loadIcon = { null },
        )
    }
}
