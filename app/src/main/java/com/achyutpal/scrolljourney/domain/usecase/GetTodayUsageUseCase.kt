package com.achyutpal.scrolljourney.domain.usecase

import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyUsageSummary
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository
import com.achyutpal.scrolljourney.domain.repository.UsageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface TodayUsageResult {
    data class Success(val summary: DailyUsageSummary) : TodayUsageResult

    /** Usage Access is off: numbers can't be trusted, so don't show zeros. */
    data object AccessDenied : TodayUsageResult

    data class Failure(val message: String) : TodayUsageResult
}

/**
 * Today's usage (local midnight → now) for the user's enabled apps only.
 * Unselected apps never count, however much they were used.
 */
class GetTodayUsageUseCase(
    private val usageAccessRepository: UsageAccessRepository,
    private val appRepository: AppRepository,
    private val usageRepository: UsageRepository,
    private val now: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    suspend operator fun invoke(): TodayUsageResult {
        if (usageAccessRepository.checkUsageAccess() != UsageAccessState.GRANTED) {
            return TodayUsageResult.AccessDenied
        }
        return try {
            val zoneId = zone()
            val nowInstant = now()
            val today = LocalDate.ofInstant(nowInstant, zoneId)
            val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

            val tracked = appRepository.observeEnabledApps().first()
            val millis = usageRepository.getForegroundMillis(
                startMillis = startOfDay,
                endMillis = nowInstant.toEpochMilli(),
                packages = tracked.map { it.packageName }.toSet(),
            )
            val apps = tracked
                .map { AppUsage(it.packageName, it.appName, millis[it.packageName] ?: 0L) }
                .sortedWith(compareByDescending<AppUsage> { it.foregroundMillis }.thenBy { it.appName.lowercase() })

            TodayUsageResult.Success(DailyUsageSummary(today, apps))
        } catch (e: CancellationException) {
            throw e // never swallow coroutine cancellation
        } catch (e: SecurityException) {
            TodayUsageResult.AccessDenied
        } catch (e: Exception) {
            TodayUsageResult.Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}
