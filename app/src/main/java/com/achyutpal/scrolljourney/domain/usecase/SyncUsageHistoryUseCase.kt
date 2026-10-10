package com.achyutpal.scrolljourney.domain.usecase

import com.achyutpal.scrolljourney.domain.calculator.AppSelectionRules
import com.achyutpal.scrolljourney.domain.calculator.DistanceCalculator
import com.achyutpal.scrolljourney.domain.calculator.LimitEvaluator
import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DailyUsageSummary
import com.achyutpal.scrolljourney.domain.model.ForegroundUsage
import com.achyutpal.scrolljourney.domain.model.SelectedApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import com.achyutpal.scrolljourney.domain.repository.HistoryRepository
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository
import com.achyutpal.scrolljourney.domain.repository.UsageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Brings saved history up to date with Android's usage log, then returns today's usage.
 * Runs on every app open/resume (and later from WorkManager), so it must be idempotent.
 *
 * For each of the last [BACKFILL_DAYS] days (oldest first):
 * - already finalized → skip (never recalculate a frozen day); only fill in a missing status/distance
 * - Android still has events → save it, finalized, with its day status and estimated distance
 * - no events, but we saved it earlier (e.g. as "today") → freeze what we have
 * - no events and nothing saved → skip (unknown ≠ 0 minutes)
 * Then finished days are fed to the rocket (once each), and today (midnight → now) is
 * recalculated and saved, not finalized, with its status so far.
 *
 * Only the currently enabled apps and the current daily limit are used, for past days too.
 * First, tracked apps that were uninstalled are switched off (history kept), so a reinstall
 * doesn't bring them back into the total by itself.
 */
class SyncUsageHistoryUseCase(
    private val usageAccessRepository: UsageAccessRepository,
    private val appRepository: AppRepository,
    private val usageRepository: UsageRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val applyJourneyFuel: ApplyJourneyFuelUseCase,
    private val distanceCalculator: DistanceCalculator = DistanceCalculator(),
    private val now: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    /** Disables tracked apps that are no longer installed; returns the ones still tracked. */
    private suspend fun trackedStillInstalled(): List<SelectedApp> {
        val tracked = appRepository.observeEnabledApps().first()
        val installed = tracked.associate { it.packageName to appRepository.isInstalled(it.packageName) }
        val disabled = AppSelectionRules.disableUninstalled(tracked) { installed.getValue(it) }
        if (disabled.isNotEmpty()) appRepository.saveSelections(disabled)
        return tracked.filter { installed.getValue(it.packageName) }
    }

    /**
     * @param backfillDays how many finished days to bring up to date (the 30-minute widget refresh
     *   only needs yesterday, so it finalizes the previous day soon after midnight cheaply).
     */
    suspend operator fun invoke(backfillDays: Int = BACKFILL_DAYS): TodayUsageResult {
        if (usageAccessRepository.checkUsageAccess() != UsageAccessState.GRANTED) {
            return TodayUsageResult.AccessDenied
        }
        return try {
            val zoneId = zone()
            val nowInstant = now()
            val today = LocalDate.ofInstant(nowInstant, zoneId)
            val tracked = trackedStillInstalled()
            val limit = settingsRepository.getSettings().dailyLimitMinutes

            historyRepository.deleteLiveDaysAfter(today)
            for (daysAgo in backfillDays.coerceIn(0, BACKFILL_DAYS) downTo 1) {
                syncPastDay(today.minusDays(daysAgo.toLong()), tracked, limit, zoneId)
            }

            // Users who finished onboarding before the rocket existed start their journey today.
            settingsRepository.ensureJourneyStarted(today)
            applyJourneyFuel()

            val todayUsage = usageRepository.getForegroundUsage(
                startMillis = today.startMillis(zoneId),
                endMillis = nowInstant.toEpochMilli(),
                packages = tracked.packageNames(),
            )
            val summary = summarize(today, tracked, todayUsage)
            val status = LimitEvaluator.evaluate(summary.totalMinutes, limit)
            val distance = distanceCalculator.centimetres(summary.totalMinutes)
            historyRepository.saveDay(
                DailyRecord(
                    date = today,
                    totalMinutes = summary.totalMinutes,
                    status = status,
                    limitMinutes = limit,
                    distance = distance,
                    finalized = false,
                ),
                summary.apps,
            )

            TodayUsageResult.Success(summary, limit, status, distance)
        } catch (e: CancellationException) {
            throw e // never swallow coroutine cancellation
        } catch (e: SecurityException) {
            TodayUsageResult.AccessDenied
        } catch (e: Exception) {
            TodayUsageResult.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private suspend fun syncPastDay(date: LocalDate, tracked: List<SelectedApp>, limit: Int, zoneId: ZoneId) {
        val existing = historyRepository.getDay(date)
        if (existing?.finalized == true) {
            // Days frozen before the status/distance engines existed get those values once, from saved
            // minutes. An existing status and its judged limit are kept as they are.
            val missingStatus = existing.status == null
            val missingDistance = existing.distance == 0L && existing.totalMinutes > 0
            if (missingStatus || missingDistance) {
                historyRepository.updateEvaluation(
                    date = date,
                    status = existing.status ?: LimitEvaluator.evaluate(existing.totalMinutes, limit),
                    limitMinutes = if (missingStatus) limit else existing.limitMinutes ?: limit,
                    distance = distanceCalculator.centimetres(existing.totalMinutes),
                )
            }
            return
        }

        val usage = usageRepository.getForegroundUsage(
            startMillis = date.startMillis(zoneId),
            endMillis = date.plusDays(1).startMillis(zoneId) - 1,
            packages = tracked.packageNames(),
        )
        when {
            usage.hasData -> {
                val summary = summarize(date, tracked, usage)
                historyRepository.saveDay(
                    DailyRecord(
                        date = date,
                        totalMinutes = summary.totalMinutes,
                        status = LimitEvaluator.evaluate(summary.totalMinutes, limit),
                        limitMinutes = limit,
                        distance = distanceCalculator.centimetres(summary.totalMinutes),
                        finalized = true,
                    ),
                    summary.apps,
                )
            }
            existing != null -> historyRepository.markFinalized(
                date = date,
                status = LimitEvaluator.evaluate(existing.totalMinutes, limit),
                limitMinutes = limit,
                distance = distanceCalculator.centimetres(existing.totalMinutes),
            )
        }
    }

    private fun summarize(date: LocalDate, tracked: List<SelectedApp>, usage: ForegroundUsage): DailyUsageSummary {
        val apps = tracked
            .map { AppUsage(it.packageName, it.appName, usage.millisByPackage[it.packageName] ?: 0L) }
            .sortedWith(compareByDescending<AppUsage> { it.foregroundMillis }.thenBy { it.appName.lowercase() })
        return DailyUsageSummary(date, apps)
    }

    private fun LocalDate.startMillis(zoneId: ZoneId) = atStartOfDay(zoneId).toInstant().toEpochMilli()

    private fun List<SelectedApp>.packageNames() = map { it.packageName }.toSet()

    companion object {
        /** How many past days to backfill/finalize. Android usually keeps about a week of events. */
        const val BACKFILL_DAYS = 7
    }
}
