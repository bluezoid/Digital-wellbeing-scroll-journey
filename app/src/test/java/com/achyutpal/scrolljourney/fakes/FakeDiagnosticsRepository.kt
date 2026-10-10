package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.repository.DiagnosticsRepository

class FakeDiagnosticsRepository(var text: String = "diagnostics") : DiagnosticsRepository {
    override suspend fun report() = text
}
