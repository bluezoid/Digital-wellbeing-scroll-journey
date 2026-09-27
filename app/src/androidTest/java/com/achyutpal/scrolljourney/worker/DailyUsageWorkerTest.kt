package com.achyutpal.scrolljourney.worker

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.achyutpal.scrolljourney.ScrollJourneyApp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Runs the real worker against the real app container on the phone. It performs exactly the
 * same idempotent sync as opening the app, so it's safe for the device's data.
 * Skipped (not failed) on a device where onboarding hasn't been completed.
 */
@RunWith(AndroidJUnit4::class)
class DailyUsageWorkerTest {

    @Test
    fun workerSyncsAndSucceeds() = runTest {
        val app = ApplicationProvider.getApplicationContext<ScrollJourneyApp>()
        assumeTrue(app.container.settingsRepository.getSettings().onboardingComplete)

        val worker = TestListenableWorkerBuilder<DailyUsageWorker>(app).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertNotNull(app.container.historyRepository.getDay(LocalDate.now())) // today saved by the sync
    }
}
