package com.achyutpal.scrolljourney.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.ui.common.RestoreConfirmDialog

/** The intro screen plus "Restore from a backup" (system file picker → confirm → restore). */
@Composable
fun IntroRoute(
    onFinished: () -> Unit,
    onRestored: (AfterRestore) -> Unit,
    viewModel: RestoreBackupViewModel = viewModel(factory = RestoreBackupViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onPicked(it.toString()) }
    }

    LaunchedEffect(state.done) {
        state.done?.let {
            viewModel.onNavigated()
            onRestored(it)
        }
    }

    IntroScreen(
        onFinished = onFinished,
        onRestoreBackup = { if (!state.busy) picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        restoreNote = state.error?.let { stringResource(R.string.settings_backup_failed, it) },
    )
    state.pending?.let { RestoreConfirmDialog(it, onConfirm = viewModel::onConfirm, onDismiss = viewModel::onDismiss) }
}
