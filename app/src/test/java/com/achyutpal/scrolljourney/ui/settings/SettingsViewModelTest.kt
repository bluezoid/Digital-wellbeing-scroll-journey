package com.achyutpal.scrolljourney.ui.settings

import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.GraphicsCapability
import com.achyutpal.scrolljourney.domain.model.SelectedApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.fakes.FakeAppRepository
import com.achyutpal.scrolljourney.fakes.FakeBackupRepository
import com.achyutpal.scrolljourney.fakes.FakeDiagnosticsRepository
import com.achyutpal.scrolljourney.fakes.FakeDocumentStore
import com.achyutpal.scrolljourney.fakes.FakeDeviceStatusRepository
import com.achyutpal.scrolljourney.fakes.FakeHistoryRepository
import com.achyutpal.scrolljourney.fakes.FakeSettingsRepository
import com.achyutpal.scrolljourney.fakes.FakeUsageAccessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Spec §69 (reset) and §41 (settings). */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val today = LocalDate.of(2026, 9, 28)
    private val settings = FakeSettingsRepository(
        UserSettings(
            dailyLimitMinutes = 120,
            onboardingComplete = true,
            currentFuel = 25,
            lifetimeFuel = 27,
            highestReachedPlanet = Destination.SATURN,
            journeyDistance = 10_000,
            journeyStartDate = LocalDate.of(2026, 6, 1),
        ),
    )
    private val apps = FakeAppRepository(
        saved = listOf(
            SelectedApp("a", "A", true, 0L),
            SelectedApp("b", "B", true, 0L),
            SelectedApp("c", "C", false, 0L),
        ),
    )
    private val access = FakeUsageAccessRepository(UsageAccessState.GRANTED)
    private val device = FakeDeviceStatusRepository(restricted = true)
    private val backups = FakeBackupRepository()
    private val documents = FakeDocumentStore()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = SettingsViewModel(settings, apps, access, device, backups, documents, FakeDiagnosticsRepository(), today = { today }).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `shows tracked app count, limit, rocket and version`() = runTest {
        val state = viewModel().uiState.value
        assertEquals(2, state.trackedAppCount) // only enabled apps
        assertEquals(120, state.limitMinutes)
        assertEquals(Destination.SATURN, state.rocket.highestReachedPlanet)
        assertEquals("0.1.0", state.versionName)
    }

    @Test
    fun `device status is read on refresh`() = runTest {
        val vm = viewModel()
        vm.refreshDeviceStatus()
        assertEquals(UsageAccessState.GRANTED, vm.uiState.value.device.usageAccess)
        assertTrue(vm.uiState.value.device.backgroundRestricted)

        device.restricted = false
        access.current = UsageAccessState.REVOKED
        vm.refreshDeviceStatus()
        assertFalse(vm.uiState.value.device.backgroundRestricted)
        assertEquals(UsageAccessState.REVOKED, vm.uiState.value.device.usageAccess)
    }

    @Test
    fun `spec 69 - confirmed reset restarts the journey today`() = runTest {
        val vm = viewModel()
        vm.onResetClick()
        assertTrue(vm.uiState.value.showResetConfirm)

        vm.onResetConfirm()

        val s = settings.state.value
        assertEquals(0, s.currentFuel)
        assertEquals(0, s.lifetimeFuel)
        assertEquals(Destination.EARTH, s.highestReachedPlanet)
        assertEquals(0L, s.journeyDistance)
        assertEquals(today, s.journeyStartDate)
        assertEquals(120, s.dailyLimitMinutes) // preferences untouched
        assertFalse(vm.uiState.value.showResetConfirm)
        assertTrue(vm.uiState.value.resetDone)
    }

    @Test
    fun `cancelling the dialog changes nothing`() = runTest {
        val vm = viewModel()
        vm.onResetClick()
        vm.onResetDismiss()
        assertFalse(vm.uiState.value.showResetConfirm)
        assertEquals(25, settings.state.value.currentFuel)
        assertEquals(Destination.SATURN, settings.state.value.highestReachedPlanet)
    }

    @Test
    fun `reset does not touch history`() = runTest {
        val history = FakeHistoryRepository()
        val day = DailyRecord(today.minusDays(1), 44, status = DayStatus.GREEN, fuelDelta = 1, fuelApplied = true, finalized = true)
        history.saveDay(day, emptyList())

        viewModel().apply { onResetClick(); onResetConfirm() }

        assertEquals(day, history.records[today.minusDays(1)])
    }

    @Test
    fun `3D switch is on by default and saves when toggled`() = runTest {
        val vm = viewModel()
        assertTrue(vm.uiState.value.journey3dEnabled)
        assertTrue(vm.uiState.value.device3dSupported)

        vm.onJourney3dChanged(false)

        assertFalse(settings.state.value.journey3dEnabled)
        assertFalse(vm.uiState.value.journey3dEnabled)
    }

    @Test
    fun `phones that can't run 3D show the switch as unsupported`() = runTest {
        val lowRam = FakeDeviceStatusRepository(graphicsCapability = GraphicsCapability(isLowRamDevice = true))
        val vm = SettingsViewModel(settings, apps, access, lowRam, backups, documents, FakeDiagnosticsRepository(), today = { today })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertFalse(vm.uiState.value.device3dSupported)
    }

    @Test
    fun `export writes the backup to the chosen file`() = runTest {
        val vm = viewModel()
        assertEquals("scroll-journey-backup-2026-09-28.json", vm.suggestedBackupName())

        vm.onExport("content://downloads/1")

        assertEquals("BACKUP:20", documents.files["content://downloads/1"])
        assertEquals(BackupMessage.Exported(days = 20), vm.uiState.value.backup.message)
        assertFalse(vm.uiState.value.backup.busy)
    }

    @Test
    fun `import asks for confirmation before restoring`() = runTest {
        documents.files["content://b"] = "BACKUP:14"
        val vm = viewModel()

        vm.onImportPicked("content://b")
        assertEquals(14, vm.uiState.value.backup.pendingRestore?.days)
        assertEquals(null, backups.restored) // nothing changed yet

        vm.onRestoreConfirm()
        assertEquals("BACKUP:14", backups.restored)
        assertEquals(null, vm.uiState.value.backup.pendingRestore)
        assertEquals(14, (vm.uiState.value.backup.message as BackupMessage.Restored).summary.days)
    }

    @Test
    fun `cancelling the restore changes nothing`() = runTest {
        documents.files["content://b"] = "BACKUP:14"
        val vm = viewModel()
        vm.onImportPicked("content://b")
        vm.onRestoreDismiss()
        vm.onRestoreConfirm() // no pending backup any more: ignored
        assertEquals(null, backups.restored)
        assertEquals(null, vm.uiState.value.backup.pendingRestore)
    }

    @Test
    fun `an invalid file shows why instead of crashing`() = runTest {
        documents.files["content://x"] = "a holiday photo"
        val vm = viewModel()
        vm.onImportPicked("content://x")
        assertEquals(BackupMessage.Failed("not a Scroll Journey backup"), vm.uiState.value.backup.message)
        assertEquals(null, vm.uiState.value.backup.pendingRestore)
    }

    @Test
    fun `file access errors become a message`() = runTest {
        documents.failWith = java.io.IOException("disk full")
        val vm = viewModel()
        vm.onExport("content://d")
        assertEquals(BackupMessage.Failed("disk full"), vm.uiState.value.backup.message)
    }

    @Test
    fun `share diagnostics hands the report to the screen once`() = runTest {
        val vm = viewModel()
        vm.onShareDiagnostics()
        assertEquals("diagnostics", vm.uiState.value.diagnosticsToShare)
        vm.onDiagnosticsShared()
        assertEquals(null, vm.uiState.value.diagnosticsToShare)
    }
}
