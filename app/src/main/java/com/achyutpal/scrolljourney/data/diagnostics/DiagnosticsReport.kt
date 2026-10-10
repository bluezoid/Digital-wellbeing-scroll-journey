package com.achyutpal.scrolljourney.data.diagnostics

/** Everything in a diagnostics report. Plain values only, so [format] is unit-tested. */
data class DiagnosticsReport(
    val generatedAt: String,
    val app: String,
    val device: String,
    val status: List<Pair<String, String>>,
    val jobs: List<String>,
    val recentDays: List<String>,
    val exitReasons: List<String>,
    val crashes: List<String>,
) {
    fun format(): String = buildString {
        appendLine("Scroll Journey diagnostics")
        appendLine("Generated: $generatedAt")
        appendLine("App: $app")
        appendLine("Device: $device")
        appendLine("(Contains no app names or per-app usage.)")
        section("Status") { status.forEach { (k, v) -> appendLine("$k: $v") } }
        section("Background jobs") { list(jobs) }
        section("Last days (date · minutes · status · final · fuel)") { list(recentDays) }
        section("Recent app exits (Android)") { list(exitReasons) }
        section("Saved crash reports") {
            if (crashes.isEmpty()) appendLine("none") else crashes.forEachIndexed { i, c ->
                if (i > 0) appendLine("---")
                appendLine(c.trimEnd())
            }
        }
    }

    private fun StringBuilder.section(title: String, body: StringBuilder.() -> Unit) {
        appendLine()
        appendLine("== $title ==")
        body()
    }

    private fun StringBuilder.list(lines: List<String>) {
        if (lines.isEmpty()) appendLine("none") else lines.forEach { appendLine(it) }
    }
}
