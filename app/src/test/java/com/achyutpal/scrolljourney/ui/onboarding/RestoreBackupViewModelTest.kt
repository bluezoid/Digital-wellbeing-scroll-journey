package com.achyutpal.scrolljourney.ui.onboarding

import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.fakes.FakeBackupRepository
import com.achyutpal.scrolljourney.fakes.FakeDocumentStore
import com.achyutpal.scrolljourney.fakes.FakeSettingsRepository
import com.achyutpal.scrolljourney.fakes.FakeUsageAccessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestoreBackupViewModelTest {

    private val documents = FakeDocumentStore().apply { files["content://b"] = "BACKUP:20" }
    private val backups = FakeBackupRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** The fake restore doesn't write settings, so the "restored" onboarding flag is set up front. */
    private fun vm(onboarded: Boolean = true, access: UsageAccessState = UsageAccessState.REVOKED) = RestoreBackupViewModel(
        backups, documents, FakeSettingsRepository(UserSettings(onboardingComplete = onboarded)), FakeUsageAccessRepository(access),
    )

    @Test
    fun `restore on a new phone goes to Usage Access next`() = runTest {
        val vm = vm()
        vm.onPicked("content://b")
        assertEquals(20, vm.state.value.pending?.days)
        vm.onConfirm()
        assertEquals("BACKUP:20", backups.restored)
        assertEquals(AfterRestore.USAGE_ACCESS, vm.state.value.done)
    }

    @Test
    fun `restore with access already granted goes to the dashboard`() = runTest {
        val vm = vm(access = UsageAccessState.GRANTED)
        vm.onPicked("content://b")
        vm.onConfirm()
        assertEquals(AfterRestore.DASHBOARD, vm.state.value.done)
    }

    @Test
    fun `a backup from unfinished onboarding continues onboarding`() = runTest {
        val vm = vm(onboarded = false)
        vm.onPicked("content://b")
        vm.onConfirm()
        assertEquals(AfterRestore.ONBOARDING, vm.state.value.done)
    }

    @Test
    fun `a wrong file shows the reason and restores nothing`() = runTest {
        documents.files["content://p"] = "not a backup"
        val vm = vm()
        vm.onPicked("content://p")
        assertEquals("not a Scroll Journey backup", vm.state.value.error)
        assertNull(vm.state.value.pending)
        assertNull(backups.restored)
    }

    @Test
    fun `cancel keeps everything as it was`() = runTest {
        val vm = vm()
        vm.onPicked("content://b")
        vm.onDismiss()
        vm.onConfirm()
        assertNull(backups.restored)
        assertNull(vm.state.value.done)
    }
}
