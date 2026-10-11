package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.model.UsageAccessState

/**
 * Answers "is Usage Access switched on right now?".
 *
 * An interface (not the Android class directly) so ViewModels can be unit-tested
 * with a fake, without a phone or the Android framework.
 */
interface UsageAccessRepository {
    fun checkUsageAccess(): UsageAccessState

    /**
     * True if Android currently blocks the Usage Access switch for this app as a "restricted
     * setting" (Android 13+, apps installed from an APK file). False if not, or if unknown.
     */
    fun isBlockedByRestrictedSettings(): Boolean
}
