package com.achyutpal.scrolljourney.domain.repository

/** Builds the "Share diagnostics" text: app status and recent crashes, with no app names. */
interface DiagnosticsRepository {
    suspend fun report(): String
}
