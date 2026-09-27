package com.achyutpal.scrolljourney.data.settings

import com.achyutpal.scrolljourney.data.local.dao.UserSettingsDao
import com.achyutpal.scrolljourney.data.local.entity.toDomain
import com.achyutpal.scrolljourney.data.local.entity.toEntity
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** The settings row is created lazily with defaults the first time anyone asks for it. */
class RoomSettingsRepository(
    private val dao: UserSettingsDao,
) : SettingsRepository {

    override suspend fun getSettings(): UserSettings {
        ensureRow()
        return checkNotNull(dao.get()).toDomain()
    }

    override fun observeSettings(): Flow<UserSettings> =
        dao.observe()
            .onStart { ensureRow() }
            .filterNotNull()
            .map { it.toDomain() }

    override suspend fun setOnboardingComplete() {
        ensureRow()
        dao.setOnboardingComplete()
    }

    private suspend fun ensureRow() {
        dao.insertIfMissing(UserSettings().toEntity())
    }
}
