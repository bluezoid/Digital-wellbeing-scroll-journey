package com.achyutpal.scrolljourney.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Uses WorkManager's in-memory test instance: no real jobs are scheduled on the phone. */
@RunWith(AndroidJUnit4::class)
class DailySyncSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val evening = ZonedDateTime.of(2026, 10, 10, 20, 0, 0, 0, ZoneId.of("Asia/Kolkata"))

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    private fun unique(name: String): List<WorkInfo> = WorkManager.getInstance(context).getWorkInfosForUniqueWork(name).get()

    @Test
    fun appStartSchedulesTheNightlyJobForHalfPastMidnightAndTheRefresh() {
        DailySyncScheduler.schedule(context, evening)

        val nightly = unique(DailySyncScheduler.NIGHTLY_WORK).single()
        assertEquals(WorkInfo.State.ENQUEUED, nightly.state)
        assertEquals(TimeUnit.MINUTES.toMillis(4 * 60 + 30), nightly.initialDelayMillis) // 20:00 → 00:30

        val refresh = unique(DailySyncScheduler.REFRESH_WORK).single()
        assertEquals(WorkInfo.State.ENQUEUED, refresh.state)
        assertEquals(TimeUnit.MINUTES.toMillis(30), refresh.periodicityInfo?.repeatIntervalMillis)
    }

    @Test
    fun startingTheAppAgainDoesNotDuplicateOrPostponeJobs() {
        DailySyncScheduler.schedule(context, evening)
        val first = unique(DailySyncScheduler.NIGHTLY_WORK).single().id

        DailySyncScheduler.schedule(context, evening.plusHours(1))

        assertEquals(first, unique(DailySyncScheduler.NIGHTLY_WORK).single().id)
        assertEquals(1, unique(DailySyncScheduler.REFRESH_WORK).size)
    }

    @Test
    fun eachNightAppendsTheNextNightAfterItself() {
        DailySyncScheduler.schedule(context, evening)
        val tonight = ZonedDateTime.of(2026, 10, 11, 0, 30, 5, 0, evening.zone)

        DailySyncScheduler.scheduleNextNight(context, tonight)

        val chain = unique(DailySyncScheduler.NIGHTLY_WORK)
        assertEquals(2, chain.size)
        val next = chain.single { it.state == WorkInfo.State.BLOCKED } // waits for tonight's run to finish
        assertTrue(next.initialDelayMillis in TimeUnit.HOURS.toMillis(23)..TimeUnit.HOURS.toMillis(24)) // → next 00:30
    }
}
