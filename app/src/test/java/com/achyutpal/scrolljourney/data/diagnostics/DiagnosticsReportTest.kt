package com.achyutpal.scrolljourney.data.diagnostics

import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReportTest {

    private val report = DiagnosticsReport(
        generatedAt = "2026-10-10T17:00:00Z",
        app = "1.0.0 (com.achyutpal.scrolljourney)",
        device = "realme RMX3031 · Android 13 (API 33)",
        status = listOf("Daily limit" to "120 min", "Usage Access" to "true"),
        jobs = listOf("nightly-sync: ENQUEUED · attempts 0 · next 2026-10-10T19:00:00Z"),
        recentDays = listOf("2026-10-09 · 0 · GREEN · yes · 1"),
        exitReasons = emptyList(),
        crashes = listOf("Crash at …\njava.lang.IllegalStateException: boom"),
    )

    @Test
    fun `report has every section with its content`() {
        val text = report.format()
        listOf(
            "Scroll Journey diagnostics", "App: 1.0.0", "Device: realme", "Daily limit: 120 min",
            "== Background jobs ==", "nightly-sync: ENQUEUED", "== Last days", "2026-10-09 · 0 · GREEN",
            "== Recent app exits (Android) ==\nnone", "IllegalStateException: boom", "no app names",
        ).forEach { assertTrue("missing: $it", text.contains(it)) }
    }
}
