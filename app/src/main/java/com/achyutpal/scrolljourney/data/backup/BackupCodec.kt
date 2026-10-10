package com.achyutpal.scrolljourney.data.backup

import com.achyutpal.scrolljourney.domain.calculator.DailyLimitRules
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.repository.InvalidBackupException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Turns a [BackupFile] into JSON and back, refusing anything that isn't a valid backup. Pure: unit-tested. */
object BackupCodec {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true // a newer app may add fields; their absence is fine here
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /** @throws InvalidBackupException with a short reason if the text isn't a usable backup. */
    fun decode(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw InvalidBackupException("not a Scroll Journey backup", e)
        } catch (e: IllegalArgumentException) {
            throw InvalidBackupException("not a Scroll Journey backup", e)
        }
        validate(file)
        return file
    }

    private fun validate(file: BackupFile) {
        fun fail(reason: String): Nothing = throw InvalidBackupException(reason)
        if (file.format != BackupFile.FORMAT) fail("not a Scroll Journey backup")
        if (file.version > BackupFile.VERSION) fail("made by a newer version of the app")
        if (file.version < 1) fail("unknown backup version")

        val s = file.settings
        if (!DailyLimitRules.isValid(s.dailyLimitMinutes)) fail("invalid daily limit")
        if (s.currentFuel < 0 || s.lifetimeFuel < 0 || s.journeyDistance < 0) fail("invalid journey values")
        if (Destination.entries.none { it.name == s.highestReachedPlanet }) fail("unknown destination")
        s.journeyStartDate?.let { checkDate(it) ?: fail("invalid journey start date") }

        file.days.forEach { d ->
            checkDate(d.date) ?: fail("invalid date ${d.date}")
            if (d.status != null && DayStatus.entries.none { it.name == d.status }) fail("invalid day status")
            if (d.totalMinutes < 0 || d.distance < 0) fail("invalid minutes on ${d.date}")
        }
        if (file.days.map { it.date }.toSet().size != file.days.size) fail("duplicate days")
        file.appMinutes.forEach { m ->
            checkDate(m.date) ?: fail("invalid date ${m.date}")
            if (m.minutes < 0) fail("invalid minutes on ${m.date}")
        }
        if (file.apps.map { it.packageName }.toSet().size != file.apps.size) fail("duplicate apps")
    }

    private fun checkDate(text: String): LocalDate? =
        try { LocalDate.parse(text) } catch (e: DateTimeParseException) { null }
}
