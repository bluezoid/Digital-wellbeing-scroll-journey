package com.achyutpal.scrolljourney.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.ui.common.RestoreConfirmDialog
import com.achyutpal.scrolljourney.ui.common.emoji
import com.achyutpal.scrolljourney.ui.common.formatMinutes
import com.achyutpal.scrolljourney.ui.common.nameRes
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import com.achyutpal.scrolljourney.ui.theme.StatusGreen
import com.achyutpal.scrolljourney.util.NotificationPermission
import com.achyutpal.scrolljourney.util.openAppDetailsSettings
import com.achyutpal.scrolljourney.util.openAppNotificationSettings
import com.achyutpal.scrolljourney.util.openUsageAccessSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangeApps: () -> Unit,
    onChangeLimit: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDeviceStatus()
        onPauseOrDispose { }
    }

    // The system "Save as" / "Open" screens (Storage Access Framework): no storage permission needed.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.onExport(it.toString()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onImportPicked(it.toString()) }
    }
    // Android 13+: turning reminders on asks for the notification permission first.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshDeviceStatus()
    }

    SettingsContent(
        state = state,
        onBack = onBack,
        onChangeApps = onChangeApps,
        onChangeLimit = onChangeLimit,
        onOpenUsageAccess = { context.openUsageAccessSettings() },
        onOpenAppSettings = { context.openAppDetailsSettings() },
        onResetClick = viewModel::onResetClick,
        onJourney3dChanged = viewModel::onJourney3dChanged,
        onResetConfirm = viewModel::onResetConfirm,
        onResetDismiss = viewModel::onResetDismiss,
        onExportClick = { exportLauncher.launch(viewModel.suggestedBackupName()) },
        onImportClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        onRestoreConfirm = viewModel::onRestoreConfirm,
        onRestoreDismiss = viewModel::onRestoreDismiss,
        onRemindersChanged = { on ->
            viewModel.onRemindersChanged(on)
            if (on && NotificationPermission.needsRequest(context)) notificationPermission.launch(NotificationPermission.PERMISSION)
        },
        onOpenNotificationSettings = { context.openAppNotificationSettings() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// internal (not private) so on-device UI tests can render it with fake state.
internal fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onChangeApps: () -> Unit,
    onChangeLimit: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onResetClick: () -> Unit,
    onResetConfirm: () -> Unit,
    onResetDismiss: () -> Unit,
    onJourney3dChanged: (Boolean) -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onRestoreConfirm: () -> Unit = {},
    onRestoreDismiss: () -> Unit = {},
    onRemindersChanged: (Boolean) -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
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
            Section(stringResource(R.string.settings_section_tracking)) {
                SettingRow(
                    title = stringResource(R.string.settings_tracked_apps),
                    value = pluralStringResource(R.plurals.settings_app_count, state.trackedAppCount, state.trackedAppCount),
                    onClick = onChangeApps,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingRow(
                    title = stringResource(R.string.settings_daily_limit),
                    value = formatMinutes(state.limitMinutes),
                    onClick = onChangeLimit,
                )
            }

            Section(stringResource(R.string.settings_section_phone)) {
                val accessOn = state.device.usageAccess == UsageAccessState.GRANTED
                SettingRow(
                    title = stringResource(R.string.settings_usage_access),
                    value = stringResource(if (accessOn) R.string.settings_on else R.string.settings_off),
                    valueColor = if (accessOn) StatusGreen else MaterialTheme.colorScheme.error,
                    onClick = onOpenUsageAccess,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                val backgroundOk = !state.device.backgroundRestricted
                SettingRow(
                    title = stringResource(R.string.settings_background),
                    value = stringResource(if (backgroundOk) R.string.settings_allowed else R.string.settings_restricted),
                    valueColor = if (backgroundOk) StatusGreen else MaterialTheme.colorScheme.error,
                    subtitle = stringResource(
                        if (state.device.ignoringBatteryOptimizations) R.string.settings_battery_unrestricted
                        else R.string.settings_battery_optimised,
                    ),
                    onClick = onOpenAppSettings,
                )
                Text(
                    text = stringResource(R.string.settings_background_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
            }

            Section(stringResource(R.string.settings_section_journey)) {
                JourneySummary(state.rocket, state.journeyStartDate)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingRow(
                    title = stringResource(R.string.settings_reset_journey),
                    titleColor = MaterialTheme.colorScheme.error,
                    subtitle = stringResource(R.string.settings_reset_subtitle),
                    onClick = onResetClick,
                )
                if (state.resetDone) {
                    Text(
                        text = stringResource(R.string.settings_reset_done),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusGreen,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            Section(stringResource(R.string.settings_section_backup)) {
                SettingRow(
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_subtitle),
                    onClick = onExportClick,
                    enabled = !state.backup.busy,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingRow(
                    title = stringResource(R.string.settings_import),
                    subtitle = stringResource(R.string.settings_import_subtitle),
                    onClick = onImportClick,
                    enabled = !state.backup.busy,
                )
                val message = when (val m = state.backup.message) {
                    null -> if (state.backup.busy) stringResource(R.string.settings_backup_working) to MaterialTheme.colorScheme.onSurfaceVariant else null
                    is BackupMessage.Exported -> pluralStringResource(R.plurals.settings_export_done, m.days, m.days) to StatusGreen
                    is BackupMessage.Restored -> pluralStringResource(
                        R.plurals.settings_restore_done,
                        m.summary.days,
                        m.summary.days,
                        stringResource(m.summary.destination.nameRes()),
                        m.summary.currentFuel,
                    ) to StatusGreen
                    is BackupMessage.Failed -> stringResource(R.string.settings_backup_failed, m.reason) to MaterialTheme.colorScheme.error
                }
                Text(
                    text = message?.first ?: stringResource(R.string.settings_backup_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = message?.second ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
            }

            Section(stringResource(R.string.settings_section_reminders)) {
                val blocked = state.remindersEnabled && !state.device.notificationsAllowed
                SwitchRow(
                    title = stringResource(R.string.settings_reminders),
                    subtitle = stringResource(if (state.remindersEnabled) R.string.settings_reminders_on else R.string.settings_reminders_off),
                    checked = state.remindersEnabled,
                    enabled = true,
                    onCheckedChange = onRemindersChanged,
                )
                if (blocked) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingRow(
                        title = stringResource(R.string.settings_reminders_blocked),
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = onOpenNotificationSettings,
                    )
                }
            }

            Section(stringResource(R.string.settings_section_view)) {
                val on = state.journey3dEnabled && state.device3dSupported
                SwitchRow(
                    title = stringResource(R.string.settings_journey_3d),
                    subtitle = stringResource(
                        when {
                            !state.device3dSupported -> R.string.settings_journey_3d_unsupported
                            on -> R.string.settings_journey_3d_on
                            else -> R.string.settings_journey_3d_off
                        },
                    ),
                    checked = on,
                    enabled = state.device3dSupported,
                    onCheckedChange = onJourney3dChanged,
                )
            }

            Section(stringResource(R.string.settings_section_privacy)) {
                Text(
                    text = stringResource(R.string.settings_privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Section(stringResource(R.string.settings_section_credits)) {
                listOf(R.string.settings_credits_textures, R.string.settings_credits_rocket, R.string.settings_credits_engine)
                    .forEach { credit ->
                        Text(
                            text = stringResource(credit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
            }

            Text(
                text = stringResource(R.string.settings_version, state.versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    state.backup.pendingRestore?.let { RestoreConfirmDialog(it, onConfirm = onRestoreConfirm, onDismiss = onRestoreDismiss) }

    if (state.showResetConfirm) {
        AlertDialog(
            onDismissRequest = onResetDismiss,
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_body)) },
            confirmButton = {
                TextButton(onClick = onResetConfirm) {
                    Text(stringResource(R.string.settings_reset_confirm_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onResetDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun JourneySummary(rocket: RocketState, start: LocalDate?) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
    val planet = rocket.highestReachedPlanet
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "${planet.emoji()} ${stringResource(planet.nameRes())} · " +
                stringResource(R.string.rocket_fuel_label, rocket.currentFuel),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        start?.let {
            Text(
                text = stringResource(R.string.settings_journey_since, it.format(dateFormat)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    onClick: () -> Unit,
    value: String? = null,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        value?.let {
            Spacer(Modifier.width(8.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge, color = valueColor, fontWeight = FontWeight.SemiBold)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A setting with an on/off switch; the whole row toggles it. */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        // onCheckedChange = null: the row handles the toggle, so the switch and the row act as one control.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Preview(heightDp = 1200)
@Composable
private fun SettingsPreview() {
    ScrollJourneyTheme(darkTheme = true) {
        SettingsContent(
            state = SettingsUiState(
                trackedAppCount = 5,
                limitMinutes = 120,
                device = DeviceStatus(UsageAccessState.GRANTED, backgroundRestricted = false, ignoringBatteryOptimizations = false),
                rocket = RocketState(currentFuel = 3, lifetimeFuel = 3, highestReachedPlanet = Destination.MOON),
                journeyStartDate = LocalDate.of(2026, 9, 27),
                versionName = "0.1.0",
            ),
            onBack = {}, onChangeApps = {}, onChangeLimit = {}, onOpenUsageAccess = {},
            onOpenAppSettings = {}, onResetClick = {}, onResetConfirm = {}, onResetDismiss = {},
        )
    }
}
