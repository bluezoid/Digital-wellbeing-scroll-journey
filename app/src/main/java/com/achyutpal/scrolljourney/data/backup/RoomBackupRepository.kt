package com.achyutpal.scrolljourney.data.backup

import com.achyutpal.scrolljourney.data.local.dao.BackupDao
import com.achyutpal.scrolljourney.data.local.entity.toEntity
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.repository.BackupRepository
import com.achyutpal.scrolljourney.domain.repository.BackupSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RoomBackupRepository(
    private val dao: BackupDao,
    private val appVersion: String,
    private val now: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : BackupRepository {

    override suspend fun export(): String {
        val s = dao.snapshot()
        val file = BackupFile(
            exportedAt = now().toString(),
            appVersion = appVersion,
            settings = (s.settings ?: UserSettings().toEntity()).toRow(),
            apps = s.apps.map { it.toRow() },
            days = s.days.map { it.toRow() },
            appMinutes = s.appMinutes.map { it.toRow() },
        )
        return BackupCodec.encode(file)
    }

    override fun inspect(text: String): BackupSummary = BackupCodec.decode(text).summary()

    override suspend fun restore(text: String): BackupSummary {
        val file = BackupCodec.decode(text)
        dao.replaceAll(
            settings = file.settings.toEntity(),
            apps = file.apps.map { it.toEntity() },
            days = file.days.map { it.toEntity() },
            appMinutes = file.appMinutes.map { it.toEntity() },
        )
        return file.summary()
    }

    private fun BackupFile.summary() = BackupSummary(
        exportedOn = runCatching { Instant.parse(exportedAt).atZone(zone()).toLocalDate().toString() }
            .getOrDefault(exportedAt.take(10)),
        days = days.size,
        destination = Destination.valueOf(settings.highestReachedPlanet),
        currentFuel = settings.currentFuel,
    )

    companion object {
        /** File name offered in the system "Save as" screen. */
        fun suggestedFileName(today: LocalDate) = "scroll-journey-backup-$today.json"
    }
}
