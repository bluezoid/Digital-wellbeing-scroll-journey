package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository

class FakeUsageAccessRepository(
    var current: UsageAccessState = UsageAccessState.GRANTED,
    var restricted: Boolean = false,
) : UsageAccessRepository {
    override fun checkUsageAccess() = current
    override fun isBlockedByRestrictedSettings() = restricted
}
