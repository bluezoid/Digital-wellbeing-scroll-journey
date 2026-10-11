package com.achyutpal.scrolljourney.ui.permission

import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.fakes.FakeUsageAccessRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageAccessRestrictedTest {

    @Test
    fun `restricted is shown only while access is still off`() {
        val repo = FakeUsageAccessRepository(UsageAccessState.REVOKED, restricted = true)
        val vm = UsageAccessViewModel(repo)

        vm.refresh()
        assertTrue(vm.restricted.value)

        repo.current = UsageAccessState.GRANTED // the user allowed it and switched access on
        vm.refresh()
        assertFalse(vm.restricted.value)
        assertEquals(UsageAccessState.GRANTED, vm.state.value)
    }

    @Test
    fun `normal phones never see the restricted card`() {
        val vm = UsageAccessViewModel(FakeUsageAccessRepository(UsageAccessState.REVOKED, restricted = false))
        vm.refresh()
        assertFalse(vm.restricted.value)
    }
}
