package com.budget.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.getSelectedEndDate
import androidx.compose.material3.getSelectedStartDate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budget.app.R
import com.budget.app.domain.analytics.AnalyticsBucket
import com.budget.app.domain.analytics.AnalyticsPeriod
import com.budget.app.ui.theme.analyticsColors
import com.budget.app.util.MoneyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Экран «Аналитика»: итоги, расходы по категориям и динамика доходов и расходов. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomDialog by remember { mutableStateOf(false) }

    val analytics = analyticsColors()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.screen_analytics),
            style = MaterialTheme.typography.headlineSmall,
            color = analytics.accent,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        PeriodSwitcher(
            selected = state.selection.period,
            onSelect = { period ->
                if (period == AnalyticsPeriod.CUSTOM) {
                    showCustomDialog = true
                } else {
                    viewModel.onPeriodSelect(period)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Text(
            text = stringResource(
                R.string.analytics_period_range,
                state.range.fromDate.toDisplayDate(),
                state.range.toDate.toDisplayDate()
            ),
            style = MaterialTheme.typography.bodySmall,
            color = analytics.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (state.isEmpty) {
            EmptyState(modifier = Modifier.fillMaxSize())
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryCard(
                    totalExpense = state.totalExpense,
                    totalIncome = state.totalIncome,
                    difference = state.difference
                )
            }

            item {
                SectionCard(title = stringResource(R.string.analytics_by_category)) {
                    if (state.categorySlices.isEmpty()) {
                        Hint(text = stringResource(R.string.analytics_no_expenses))
                    } else {
                        CategoryPieChart(slices = state.categorySlices)
                    }
                }
            }

            item {
                SectionCard(
                    title = stringResource(R.string.analytics_dynamics),
                    subtitle = stringResource(
                        if (state.bucket == AnalyticsBucket.MONTH) {
                            R.string.analytics_dynamics_months
                        } else {
                            R.string.analytics_dynamics_days
                        }
                    )
                ) {
                    IncomeExpenseBarChart(points = state.points, bucket = state.bucket)
                }
            }

            if (state.walletSlices.isNotEmpty()) {
                item {
                    SectionCard(title = stringResource(R.string.analytics_by_wallet)) {
                        state.walletSlices.forEach { slice ->
                            WalletRow(slice = slice, total = state.totalExpense)
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomRangeDialog(
            initialFrom = state.selection.customFrom ?: state.range.fromDate,
            initialTo = state.selection.customTo ?: state.range.toDate,
            onApply = { from, to ->
                viewModel.onCustomRangeSelect(from, to)
                showCustomDialog = false
            },
            onDismiss = { showCustomDialog = false }
        )
    }
}

/** Переключатель периода: неделя / месяц / год / диапазон. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSwitcher(
    selected: AnalyticsPeriod,
    onSelect: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = AnalyticsPeriod.entries

    val analytics = analyticsColors()

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.analytics_period),
            style = MaterialTheme.typography.labelLarge,
            color = analytics.accent
        )

        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            periods.forEachIndexed { index, period ->
                val selected = selected == period
                SegmentedButton(
                    selected = selected,
                    onClick = { onSelect(period) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = analytics.accentContainer,
                        activeContentColor = analytics.onAccentContainer,
                        activeBorderColor = analytics.accent,
                        inactiveBorderColor = analytics.surfaceVariant
                    ),
                    icon = {
                        Icon(
                            imageVector = period.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text(
                        text = stringResource(period.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Итоги периода: расходы, доходы и разница. */
@Composable
private fun SummaryCard(
    totalExpense: Long,
    totalIncome: Long,
    difference: Long,
    modifier: Modifier = Modifier
) {
    val analytics = analyticsColors()
    val differenceColor = when {
        difference > 0 -> analytics.accent
        difference < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = analytics.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SummaryRow(
                label = stringResource(R.string.analytics_total_expense),
                amount = totalExpense,
                amountColor = MaterialTheme.colorScheme.onSurface
            )
            SummaryRow(
                label = stringResource(R.string.analytics_total_income),
                amount = totalIncome,
                amountColor = MaterialTheme.colorScheme.onSurface
            )
            SummaryRow(
                label = stringResource(R.string.analytics_difference),
                amount = difference,
                amountColor = differenceColor
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: Long,
    amountColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = MoneyFormatter.format(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

/** Карточка раздела: заголовок, пояснение и содержимое. */
@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    val analytics = analyticsColors()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = analytics.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

/** Доля расходов по кошельку с полоской заполнения. */
@Composable
private fun WalletRow(slice: WalletSlice, total: Long) {
    val fraction = if (total > 0) slice.amount.toFloat() / total else 0f

    val analytics = analyticsColors()
    val trackColor = analytics.surface
    val progressColor = analytics.accent

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = slice.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${percent(fraction)} · ${MoneyFormatter.format(slice.amount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(progressColor)
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Заглушка, когда за период операций нет. */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.analytics_empty),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.analytics_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Диалог выбора произвольного диапазона: календарь Material 3 DateRangePicker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(
    initialFrom: LocalDate,
    initialTo: LocalDate,
    onApply: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = remember {
        DateRangePickerState(
            locale = RUSSIAN_LOCALE,
            initialSelectedStartDate = initialFrom,
            initialSelectedEndDate = initialTo
        )
    }
    val start = state.getSelectedStartDate()
    val end = state.getSelectedEndDate()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (start != null) {
                        // Одна выбранная дата — диапазон из одного дня
                        onApply(start, end ?: start)
                    }
                },
                enabled = start != null
            ) {
                Text(stringResource(R.string.analytics_custom_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        DateRangePicker(
            state = state,
            showModeToggle = false,
            dateFormatter = remember { RussianDateFormatter },
            title = {
                Text(
                    text = stringResource(R.string.analytics_custom_title),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Дата в виде ДД.ММ.ГГГГ. */
private fun LocalDate.toDisplayDate(): String = format(DISPLAY_DATE_FORMAT)

private val RUSSIAN_LOCALE: Locale = Locale.forLanguageTag("ru")

private val DISPLAY_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN_LOCALE)

/** Формат дат в календаре: ДД.ММ.ГГГГ, русская локаль. */
private object RussianDateFormatter : DatePickerFormatter {
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN_LOCALE)
    private val monthYearFormat = DateTimeFormatter.ofPattern("LLLL uuuu", RUSSIAN_LOCALE)

    override fun formatMonthYear(monthMillis: Long?, locale: Locale): String? {
        if (monthMillis == null) return null
        return Instant.ofEpochMilli(monthMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(monthYearFormat)
    }

    override fun formatDate(
        dateMillis: Long?,
        locale: Locale,
        forContentDescription: Boolean
    ): String? {
        if (dateMillis == null) return null
        return Instant.ofEpochMilli(dateMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(dateFormat)
    }
}

/** Подпись периода для переключателя. */
private val AnalyticsPeriod.labelRes: Int
    get() = when (this) {
        AnalyticsPeriod.WEEK -> R.string.analytics_period_week
        AnalyticsPeriod.MONTH -> R.string.analytics_period_month
        AnalyticsPeriod.YEAR -> R.string.analytics_period_year
        AnalyticsPeriod.CUSTOM -> R.string.analytics_period_custom
    }

/** Иконка периода для переключателя. */
private val AnalyticsPeriod.icon: ImageVector
    get() = when (this) {
        AnalyticsPeriod.WEEK -> Icons.Filled.ViewWeek
        AnalyticsPeriod.MONTH -> Icons.Filled.CalendarViewMonth
        AnalyticsPeriod.YEAR -> Icons.Filled.Today
        AnalyticsPeriod.CUSTOM -> Icons.Filled.DateRange
    }