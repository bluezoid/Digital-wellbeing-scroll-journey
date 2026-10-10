package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.repository.BackupRepository
import com.achyutpal.scrolljourney.domain.repository.BackupSummary
import com.achyutpal.scrolljourney.domain.repository.DocumentStore
import com.achyutpal.scrolljourney.domain.repository.InvalidBackupException
import java.io.IOException

/** Treats any text starting with "BACKUP:" as a valid backup of N days ("BACKUP:20"). */
class FakeBackupRepository(var days: Int = 20) : BackupRepository {
    var restored: String? = null

    override suspend fun export() = "BACKUP:$days"

    override fun inspect(text: String): BackupSummary {
        if (!text.startsWith("BACKUP:")) throw InvalidBackupException("not a Scroll Journey backup")
        return BackupSummary("2026-10-10", text.removePrefix("BACKUP:").toInt(), Destination.MOON, 6)
    }

    override suspend fun restore(text: String): BackupSummary = inspect(text).also { restored = text }
}

class FakeDocumentStore : DocumentStore {
    val files = mutableMapOf<String, String>()
    var failWith: Exception? = null

    override suspend fun write(uri: String, text: String) {
        failWith?.let { throw it }
        files[uri] = text
    }

    override suspend fun read(uri: String): String {
        failWith?.let { throw it }
        return files[uri] ?: throw IOException("can't open the file")
    }
}
