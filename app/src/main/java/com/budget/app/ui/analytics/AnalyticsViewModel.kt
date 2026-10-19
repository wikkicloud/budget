package com.budget.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budget.app.data.local.entity.SavingsOperationEntity
import com.budget.app.data.local.model.SavingsWithBalance
import com.budget.app.data.local.model.TransactionWithDetails
import com.budget.app.data.repository.SavingsRepository
import com.budget.app.data.repository.TransactionRepository
import com.budget.app.domain.analytics.AnalyticsBucket
import com.budget.app.domain.analytics.AnalyticsPeriod
import com.budget.app.domain.analytics.AnalyticsRange
import com.budget.app.domain.model.SavingsOperationType
import com.budget.app.domain.model.SavingsProgress
import com.budget.app.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

/** Доля категории в расходах за период. */
data class CategorySlice(
    val categoryId: Long?,
    /** null — операция без категории. */
    val icon: String?,
    val name: String?,
    val colorHex: String?,
    /** Сумма в копейках. */
    val amount: Long,
    /** Доля от итога расходов, 0..1. */
    val fraction: Float
)

/** Одна точка графика «доходы vs расходы». */
data class PeriodPoint(
    /** Начало интервала: день или первый день месяца. */
    val date: LocalDate,
    /** Доходы по операциям. */
    val income: Long,
    val expense: Long,
    /** Проценты по копилкам за интервал — отдельный сегмент столбца доходов. */
    val interest: Long = 0
)

/** Расходы по одному кошельку за период. */
data class WalletSlice(
    val walletId: Long,
    val name: String,
    val amount: Long
)

/** Копилка в блоке «Копилки»: баланс и прогресс по цели. */
data class SavingsSlice(
    val id: Long,
    val name: String,
    /** Баланс в копейках. */
    val balance: Long,
    /** Цель в копейках; null — копилка без цели. */
    val targetAmount: Long?
) {
    /** Доля от цели, 0..1; null — цели нет. */
    val progress: Float? get() = SavingsProgress.fraction(balance, targetAmount)

    /** Целые проценты от цели; null — цели нет. */
    val progressPercent: Int? get() = SavingsProgress.percent(balance, targetAmount)
}

/** Состояние выбора периода. */
data class PeriodSelection(
    val period: AnalyticsPeriod = AnalyticsPeriod.MONTH,
    /** Выбранный диапазон для [AnalyticsPeriod.CUSTOM]. */
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null
)

/** Единственное состояние экрана «Аналитика». */
data class AnalyticsUiState(
    val selection: PeriodSelection = PeriodSelection(),
    val range: AnalyticsRange = AnalyticsRange.of(AnalyticsPeriod.MONTH, LocalDate.now(), null),
    val totalExpense: Long = 0,
    /** Доходы вместе с процентами по копилкам. */
    val totalIncome: Long = 0,
    /** Проценты по копилкам за период — часть [totalIncome], показывается отдельной строкой. */
    val totalInterest: Long = 0,
    /** Доходы минус расходы за период. */
    val difference: Long = 0,
    val categorySlices: List<CategorySlice> = emptyList(),
    val points: List<PeriodPoint> = emptyList(),
    val walletSlices: List<WalletSlice> = emptyList(),
    /** Копилки с балансами и прогрессом по целям. */
    val savingsSlices: List<SavingsSlice> = emptyList(),
    /** Сумма во всех копилках в копейках. */
    val savingsTotal: Long = 0,
    /** Пополнено в копилки за период. */
    val savingsDeposited: Long = 0,
    /** Снято из копилок за период. */
    val savingsWithdrawn: Long = 0,
    val isLoading: Boolean = true
) {
    /** Шаг группировки точек графика. */
    val bucket: AnalyticsBucket get() = range.bucket

    /**
     * true — за период нет ни операций, ни копилок.
     * Копилки считаются отдельно: у них свой блок, и заглушка его не должна перекрывать.
     */
    val isEmpty: Boolean
        get() = !isLoading && totalExpense == 0L && totalIncome == 0L && savingsSlices.isEmpty()
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val savingsRepository: SavingsRepository
) : ViewModel() {

    private val selection = MutableStateFlow(PeriodSelection())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsUiState> = selection
        .flatMapLatest { current ->
            val range = current.toRange()
            combine(
                transactionRepository.getTransactionsWithDetails(range.from, range.to),
                savingsRepository.getOperationsBetween(range.from, range.to),
                savingsRepository.getSavingsWithBalance()
            ) { transactions, savingsOperations, savings ->
                buildState(current, range, transactions, savingsOperations, savings)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsUiState()
        )

    // --- Выбор периода ---

    fun onPeriodSelect(period: AnalyticsPeriod) {
        selection.update { it.copy(period = period) }
    }

    /** Диапазон можно задать в любом порядке — границы нормализуются. */
    fun onCustomRangeSelect(from: LocalDate, to: LocalDate) {
        val start = minOf(from, to)
        val end = maxOf(from, to)
        selection.update {
            it.copy(period = AnalyticsPeriod.CUSTOM, customFrom = start, customTo = end)
        }
    }

    private fun PeriodSelection.toRange(): AnalyticsRange {
        val custom = if (customFrom != null && customTo != null) customFrom..customTo else null
        return AnalyticsRange.of(period, LocalDate.now(), custom)
    }

    private fun buildState(
        selection: PeriodSelection,
        range: AnalyticsRange,
        transactions: List<TransactionWithDetails>,
        savingsOperations: List<SavingsOperationEntity>,
        savings: List<SavingsWithBalance>
    ): AnalyticsUiState {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE.name }
        val totalExpense = expenses.sumOf { it.amount }
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME.name }
            .sumOf { it.amount }

        // Проценты по копилкам считаются доходом — каждое начисление попадает сюда ровно один раз
        // (оно живёт только в savings_operations, в transactions его нет)
        val interest = savingsOperations
            .filter { it.type == SavingsOperationType.INTEREST.name }
            .sumOf { it.amount }

        val totalIncomeWithInterest = totalIncome + interest

        return AnalyticsUiState(
            selection = selection,
            range = range,
            totalExpense = totalExpense,
            totalIncome = totalIncomeWithInterest,
            totalInterest = interest,
            difference = totalIncomeWithInterest - totalExpense,
            categorySlices = buildCategorySlices(expenses, totalExpense),
            points = buildPoints(transactions, savingsOperations, range),
            walletSlices = buildWalletSlices(expenses),
            savingsSlices = savings.map {
                SavingsSlice(id = it.id, name = it.name, balance = it.balance, targetAmount = it.targetAmount)
            },
            savingsTotal = savings.sumOf { it.balance },
            savingsDeposited = savingsOperations
                .filter { it.type == SavingsOperationType.DEPOSIT.name }
                .sumOf { it.amount },
            savingsWithdrawn = savingsOperations
                .filter { it.type == SavingsOperationType.WITHDRAW.name }
                .sumOf { it.amount },
            isLoading = false
        )
    }

    /**
     * Расходы по категориям, по убыванию суммы. Операции без категории попадают в свою долю,
     * проценты по копилкам — отдельной позицией «Проценты по депозиту».
     */
    private fun buildCategorySlices(
        expenses: List<TransactionWithDetails>,
        totalExpense: Long
    ): List<CategorySlice> {
        if (totalExpense == 0L) return emptyList()

        return expenses
            .groupBy { it.categoryId }
            .map { (categoryId, group) ->
                val amount = group.sumOf { it.amount }
                CategorySlice(
                    categoryId = categoryId,
                    icon = group.first().categoryIcon,
                    name = group.first().categoryName,
                    colorHex = group.first().categoryColor,
                    amount = amount,
                    fraction = amount.toFloat() / totalExpense
                )
            }
            .sortedByDescending { it.amount }
    }

    private fun buildWalletSlices(expenses: List<TransactionWithDetails>): List<WalletSlice> =
        expenses
            .groupBy { it.walletId }
            .map { (walletId, group) ->
                WalletSlice(
                    walletId = walletId,
                    name = group.first().walletName,
                    amount = group.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.amount }

    /**
     * Точки графика по дням или месяцам, включая интервалы без операций.
     * Проценты по копилкам идут отдельным полем [PeriodPoint.interest] — в столбце доходов
     * они рисуются отдельным сегментом, поэтому в [PeriodPoint.income] не подмешиваются.
     */
    private fun buildPoints(
        transactions: List<TransactionWithDetails>,
        savingsOperations: List<SavingsOperationEntity>,
        range: AnalyticsRange
    ): List<PeriodPoint> {
        val key: (LocalDate) -> LocalDate = when (range.bucket) {
            AnalyticsBucket.DAY -> { date -> date }
            AnalyticsBucket.MONTH -> { date -> date.withDayOfMonth(1) }
        }

        val next: (LocalDate) -> LocalDate = when (range.bucket) {
            AnalyticsBucket.DAY -> { date -> date.plusDays(1) }
            AnalyticsBucket.MONTH -> { date -> date.plusMonths(1) }
        }

        val incomeByKey = transactions
            .filter { it.type == TransactionType.INCOME.name }
            .groupBy { key(AnalyticsRange.toLocalDate(it.date)) }
            .mapValues { (_, group) -> group.sumOf { it.amount } }

        val interestByKey = savingsOperations
            .filter { it.type == SavingsOperationType.INTEREST.name }
            .groupBy { key(AnalyticsRange.toLocalDate(it.date)) }
            .mapValues { (_, group) -> group.sumOf { it.amount } }

        val expenseByKey = transactions
            .filter { it.type == TransactionType.EXPENSE.name }
            .groupBy { key(AnalyticsRange.toLocalDate(it.date)) }
            .mapValues { (_, group) -> group.sumOf { it.amount } }

        val points = mutableListOf<PeriodPoint>()
        var date = key(range.fromDate)
        val last = range.toDate

        // Предохранитель на случай очень длинного пользовательского диапазона
        while (!date.isAfter(last) && points.size < MAX_POINTS) {
            points += PeriodPoint(
                date = date,
                income = incomeByKey[date] ?: 0L,
                expense = expenseByKey[date] ?: 0L,
                interest = interestByKey[date] ?: 0L
            )
            date = next(date)
        }

        return points
    }

    private companion object {
        /** Больше точек, чем поместится на графике, рисовать бессмысленно. */
        const val MAX_POINTS = 800
    }
}