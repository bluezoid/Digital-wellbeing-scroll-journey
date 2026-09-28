package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.repository.DeviceStatusRepository

class FakeDeviceStatusRepository(
    var restricted: Boolean = false,
    var ignoringBatteryOptimizations: Boolean = false,
    override val appVersionName: String = "0.1.0",
) : DeviceStatusRepository {
    override fun isBackgroundRestricted() = restricted
    override fun isIgnoringBatteryOptimizations() = ignoringBatteryOptimizations
}
