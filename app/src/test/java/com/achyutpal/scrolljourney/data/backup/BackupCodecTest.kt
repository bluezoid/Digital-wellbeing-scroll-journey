package com.achyutpal.scrolljourney.data.backup

import com.achyutpal.scrolljourney.domain.repository.InvalidBackupException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val sample = BackupFile(
        exportedAt = "2026-10-10T14:00:00Z",
        appVersion = "1.0.0",
        settings = BackupFile.SettingsRow(
            dailyLimitMinutes = 120, onboardingComplete = true, currentFuel = 6, lifetimeFuel = 8,
            highestReachedPlanet = "MOON", journeyDistance = 186_400, journeyStartDate = "2026-09-27", journey3dEnabled = true,
        ),
        apps = listOf(BackupFile.AppRow("com.google.android.youtube", "YouTube", true, 1L)),
        days = listOf(
            BackupFile.DayRow("2026-09-28", 62, "GREEN", 120, 1, 6_200, finalized = true, fuelApplied = true),
            BackupFile.DayRow("2026-10-10", 0, null, null, 0, 0, finalized = false, fuelApplied = false),
        ),
        appMinutes = listOf(BackupFile.AppMinutesRow("2026-09-28", "com.instagram.android", 31)),
    )

    private fun reason(text: String): String = assertThrows(InvalidBackupException::class.java) { BackupCodec.decode(text) }.message!!

    @Test
    fun `round trip keeps every value`() {
        assertEquals(sample, BackupCodec.decode(BackupCodec.encode(sample)))
    }

    @Test
    fun `file is readable JSON with format and version`() {
        val text = BackupCodec.encode(sample)
        assertTrue(text.contains("\"format\": \"scroll-journey-backup\""))
        assertTrue(text.contains("\"version\": 1"))
    }

    @Test
    fun `random text is rejected`() {
        assertEquals("not a Scroll Journey backup", reason("hello"))
        assertEquals("not a Scroll Journey backup", reason("{\"a\": 1}"))
    }

    @Test
    fun `another app's JSON is rejected`() {
        assertEquals("not a Scroll Journey backup", reason(BackupCodec.encode(sample.copy(format = "something-else"))))
    }

    @Test
    fun `a backup from a newer app version is refused`() {
        assertEquals("made by a newer version of the app", reason(BackupCodec.encode(sample.copy(version = 2))))
    }

    @Test
    fun `unknown fields from a newer minor format are ignored`() {
        val text = BackupCodec.encode(sample).replaceFirst("{", "{\n  \"futureField\": 42,")
        assertEquals(sample, BackupCodec.decode(text))
    }

    @Test
    fun `invalid values are refused before touching the database`() {
        assertEquals("unknown destination", reason(BackupCodec.encode(sample.copy(settings = sample.settings.copy(highestReachedPlanet = "NARNIA")))))
        assertEquals("invalid daily limit", reason(BackupCodec.encode(sample.copy(settings = sample.settings.copy(dailyLimitMinutes = 3)))))
        assertEquals("invalid journey values", reason(BackupCodec.encode(sample.copy(settings = sample.settings.copy(currentFuel = -1)))))
        assertEquals("invalid date 2026-13-01", reason(BackupCodec.encode(sample.copy(days = listOf(sample.days[0].copy(date = "2026-13-01"))))))
        assertEquals("invalid day status", reason(BackupCodec.encode(sample.copy(days = listOf(sample.days[0].copy(status = "PURPLE"))))))
        assertEquals("duplicate days", reason(BackupCodec.encode(sample.copy(days = listOf(sample.days[0], sample.days[0])))))
    }
}
