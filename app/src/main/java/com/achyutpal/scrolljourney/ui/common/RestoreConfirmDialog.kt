package com.achyutpal.scrolljourney.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.repository.BackupSummary

/** "Restore this backup?" with what the file contains; used in Settings and on the intro screen. */
@Composable
fun RestoreConfirmDialog(summary: BackupSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_restore_confirm_title)) },
        text = {
            Text(
                pluralStringResource(
                    R.plurals.settings_restore_confirm_body,
                    summary.days,
                    summary.days,
                    summary.exportedOn,
                    stringResource(summary.destination.nameRes()),
                    summary.currentFuel,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_restore_confirm_action), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
