package com.achyutpal.scrolljourney.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.achyutpal.scrolljourney.data.diagnostics.CrashLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CrashLogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clean() {
        File(context.filesDir, "crashes").deleteRecursively()
    }

    @Test
    fun crashIsSavedWithItsStackTraceAndOnlyTheLastFiveAreKept() {
        val log = CrashLog(context)
        repeat(7) { i ->
            log.write("main", IllegalStateException("boom $i"), appVersion = "1.0.0")
            Thread.sleep(2) // distinct file names
        }

        val reports = log.reports()
        assertEquals(5, reports.size)
        assertTrue(reports.first().contains("IllegalStateException: boom 6")) // newest first
        assertTrue(reports.first().contains("App 1.0.0"))
        assertTrue(reports.first().contains("at com.achyutpal.scrolljourney"))
    }
}
