package com.achyutpal.scrolljourney.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.achyutpal.scrolljourney.domain.repository.DocumentStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Storage Access Framework: the user picks the file in the system screen and Android grants this
 * app access to just that one document, so no storage permission is needed.
 */
class AndroidDocumentStore(private val resolver: ContentResolver) : DocumentStore {

    override suspend fun write(uri: String, text: String) = withContext(Dispatchers.IO) {
        // "wt" = write + truncate, so overwriting an existing file doesn't leave old bytes at the end.
        val stream = resolver.openOutputStream(Uri.parse(uri), "wt") ?: throw IOException("can't open the file")
        stream.bufferedWriter().use { it.write(text) }
    }

    override suspend fun read(uri: String): String = withContext(Dispatchers.IO) {
        val stream = resolver.openInputStream(Uri.parse(uri)) ?: throw IOException("can't open the file")
        val text = stream.bufferedReader().use { it.readText() }
        // A real backup is a few hundred KB; refuse anything absurdly large.
        if (text.length > MAX_CHARS) throw IOException("file too large")
        text
    }

    private companion object {
        const val MAX_CHARS = 20 * 1024 * 1024
    }
}
