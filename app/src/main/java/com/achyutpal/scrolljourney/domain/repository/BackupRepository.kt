package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.model.Destination

/** Export all app data to a backup text, and restore from one (spec: keep history safe across phones/reinstalls). */
interface BackupRepository {
    /** The whole database as backup text (JSON). */
    suspend fun export(): String

    /** Reads and checks a backup without changing anything. @throws InvalidBackupException */
    fun inspect(text: String): BackupSummary

    /** Replaces ALL current data with the backup, in one transaction. @throws InvalidBackupException */
    suspend fun restore(text: String): BackupSummary
}

/** What a backup contains, shown before restoring it. */
data class BackupSummary(
    /** "YYYY-MM-DD" of the export. */
    val exportedOn: String,
    val days: Int,
    val destination: Destination,
    val currentFuel: Int,
)

/** The chosen file isn't a backup this app can restore; [message] is a short reason. */
class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
