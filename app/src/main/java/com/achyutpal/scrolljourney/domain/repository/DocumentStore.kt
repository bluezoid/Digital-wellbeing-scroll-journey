package com.achyutpal.scrolljourney.domain.repository

/**
 * Reads/writes a user-chosen document (from the system "Save as" / "Open" screen).
 * The location is the document's URI as text, so ViewModels stay free of Android types.
 */
interface DocumentStore {
    suspend fun write(uri: String, text: String)
    suspend fun read(uri: String): String
}
