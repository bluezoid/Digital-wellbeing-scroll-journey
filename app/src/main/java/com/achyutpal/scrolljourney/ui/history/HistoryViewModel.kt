package com.achyutpal.scrolljourney.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.calculator.StreakCalculator
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import com.achyutpal.scrolljourney.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DayAppLine(val appName: String, val minutes: Int)

data class DayDetail(val record: DailyRecord, val apps: List<DayAppLine>)

data class HistoryUiState(
    val month: YearMonth,
    val weeks: List<List<CalendarCell>> = emptyList(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val greenDays: Int = 0,
    val yellowDays: Int = 0,
    val redDays: Int = 0,
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val selectedDate: LocalDate? = null,
    val selected: DayDetail? = null,
)

/** Month calendar of saved days plus current/best green streak (spec §35–37). */
class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val appRepository: AppRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(today()))
    private val selectedDate = MutableStateFlow<LocalDate?>(today())
    private val selectedDetail = MutableStateFlow<DayDetail?>(null)
    private val records = historyRepository.observeRecentDays(LOOKBACK_DAYS)

    val uiState: StateFlow<HistoryUiState> = combine(records, month, selectedDate, selectedDetail) { days, shownMonth, date, detail ->
        val todayDate = today()
        val streak = StreakCalculator.calculate(StreakCalculator.contiguousStatuses(days, todayDate))
        val inMonth = days.filter { YearMonth.from(it.date) == shownMonth }
        val earliest = days.minOfOrNull { it.date }?.let(YearMonth::from) ?: YearMonth.from(todayDate)
        HistoryUiState(
            month = shownMonth,
            weeks = MonthCalendar.build(shownMonth, days.associate { it.date to it.status }, todayDate),
            currentStreak = streak.current,
            bestStreak = streak.best,
            greenDays = inMonth.count { it.status == DayStatus.GREEN },
            yellowDays = inMonth.count { it.status == DayStatus.YELLOW },
            redDays = inMonth.count { it.status == DayStatus.RED },
            canGoPrevious = shownMonth.isAfter(earliest),
            canGoNext = shownMonth.isBefore(YearMonth.from(todayDate)),
            selectedDate = date,
            selected = detail?.takeIf { it.record.date == date },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState(month = YearMonth.from(today())))

    init {
        selectDay(today())
    }

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    fun selectDay(date: LocalDate) {
        selectedDate.value = date
        viewModelScope.launch {
            val record = records.first().firstOrNull { it.date == date }
            selectedDetail.value = record?.let {
                val names = appRepository.getSavedSelections().associate { app -> app.packageName to app.appName }
                DayDetail(
                    record = it,
                    apps = historyRepository.getAppMinutes(date).map { row ->
                        DayAppLine(names[row.packageName] ?: row.packageName, row.minutes)
                    },
                )
            }
        }
    }

    companion object {
        const val LOOKBACK_DAYS = 400

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ScrollJourneyApp).container
                HistoryViewModel(container.historyRepository, container.appRepository)
            }
        }
    }
}
