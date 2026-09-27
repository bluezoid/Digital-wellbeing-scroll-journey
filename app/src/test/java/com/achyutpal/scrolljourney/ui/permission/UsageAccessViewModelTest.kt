package com.achyutpal.scrolljourney.ui.permission

import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.fakes.FakeUsageAccessRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageAccessViewModelTest {

    @Test
    fun `state is UNKNOWN before the first check`() {
        val vm = UsageAccessViewModel(FakeUsageAccessRepository(UsageAccessState.GRANTED))
        assertEquals(UsageAccessState.UNKNOWN, vm.state.value)
    }

    @Test
    fun `refresh reports GRANTED when access is on`() {
        val vm = UsageAccessViewModel(FakeUsageAccessRepository(UsageAccessState.GRANTED))
        vm.refresh()
        assertEquals(UsageAccessState.GRANTED, vm.state.value)
    }

    @Test
    fun `refresh reports REVOKED when access is off`() {
        val vm = UsageAccessViewModel(FakeUsageAccessRepository(UsageAccessState.REVOKED))
        vm.refresh()
        assertEquals(UsageAccessState.REVOKED, vm.state.value)
    }

    @Test
    fun `revoking in Settings is detected on the next refresh`() {
        val repo = FakeUsageAccessRepository(UsageAccessState.GRANTED)
        val vm = UsageAccessViewModel(repo)
        vm.refresh()

        repo.current = UsageAccessState.REVOKED
        vm.refresh()
        assertEquals(UsageAccessState.REVOKED, vm.state.value)

        repo.current = UsageAccessState.GRANTED
        vm.refresh()
        assertEquals(UsageAccessState.GRANTED, vm.state.value)
    }
}
