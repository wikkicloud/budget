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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budget.app.R
import com.budget.app.domain.analytics.AnalyticsBucket
import com.budget.app.domain.analytics.AnalyticsPeriod
import com.budget.app.util.MoneyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Экран «Аналитика»: итоги, расходы по категориям и динамика доходов и расходов. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.analytics_period),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            periods.forEachIndexed { index, period ->
                SegmentedButton(
                    selected = selected == period,
                    onClick = { onSelect(period) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size)
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
    val differenceColor = when {
        difference > 0 -> MaterialTheme.colorScheme.primary
        difference < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(modifier = modifier.fillMaxWidth()) {
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
    Card(modifier = modifier.fillMaxWidth()) {
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
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

/** Диалог ввода произвольного диапазона в формате ДД.ММ.ГГГГ. */
@Composable
private fun CustomRangeDialog(
    initialFrom: LocalDate,
    initialTo: LocalDate,
    onApply: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var fromInput by remember { mutableStateOf(initialFrom.toDisplayDate()) }
    var toInput by remember { mutableStateOf(initialTo.toDisplayDate()) }
    var hasError by remember { mutableStateOf(false) }

    val from = parseDate(fromInput)
    val to = parseDate(toInput)
    val isReversed = from != null && to != null && from > to

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.analytics_custom_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = fromInput,
                    onValueChange = {
                        fromInput = it
                        hasError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.analytics_custom_from)) },
                    singleLine = true,
                    isError = hasError
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = toInput,
                    onValueChange = {
                        toInput = it
                        hasError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.analytics_custom_to)) },
                    singleLine = true,
                    isError = hasError || isReversed,
                    supportingText = when {
                        hasError -> {
                            { Text(stringResource(R.string.analytics_error_range)) }
                        }

                        isReversed -> {
                            { Text(stringResource(R.string.analytics_error_range_order)) }
                        }

                        else -> null
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (from != null && to != null) {
                        onApply(minOf(from, to), maxOf(from, to))
                    } else {
                        hasError = true
                    }
                }
            ) {
                Text(stringResource(R.string.analytics_custom_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Дата в виде ДД.ММ.ГГГГ. */
private fun LocalDate.toDisplayDate(): String = format(DISPLAY_DATE_FORMAT)

/** Разбор введённой даты; допускаем и "1.2.2026", и "01.02.2026". */
private fun parseDate(input: String): LocalDate? {
    val trimmed = input.trim()

    for (pattern in INPUT_DATE_PATTERNS) {
        try {
            return LocalDate.parse(trimmed, pattern)
        } catch (_: DateTimeParseException) {
            // Пробуем следующий формат
        }
    }

    return null
}

private val DISPLAY_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")

private val INPUT_DATE_PATTERNS: List<DateTimeFormatter> = listOf("dd.MM.uuuu", "d.M.uuuu")
    .map { DateTimeFormatter.ofPattern(it).withResolverStyle(ResolverStyle.STRICT) }

/** Подпись периода для переключателя. */
private val AnalyticsPeriod.labelRes: Int
    get() = when (this) {
        AnalyticsPeriod.WEEK -> R.string.analytics_period_week
        AnalyticsPeriod.MONTH -> R.string.analytics_period_month
        AnalyticsPeriod.YEAR -> R.string.analytics_period_year
        AnalyticsPeriod.CUSTOM -> R.string.analytics_period_custom
    }