package com.budget.app.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.budget.app.R
import com.budget.app.domain.analytics.AnalyticsBucket
import com.budget.app.ui.theme.analyticsColors

/** Диаграмма «доходы vs расходы»: пара столбцов на каждый интервал периода. */
@Composable
fun IncomeExpenseBarChart(
    points: List<PeriodPoint>,
    bucket: AnalyticsBucket,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val analytics = analyticsColors()
    val incomeColor = analytics.income
    val expenseColor = analytics.expense
    val interestColor = analytics.interest
    val axisColor = analytics.surfaceVariant

    // Проценты по копилкам — часть столбца доходов, поэтому он может быть выше самих доходов
    val maxValue = points
        .maxOf { maxOf(it.income + it.interest, it.expense) }
        .coerceAtLeast(1L)

    val hasInterest = points.any { it.interest > 0 }

    // При большом числе интервалов подписываем только каждый N-й столбец — иначе подписи сливаются
    val labelStep = (points.size / MAX_LABELS).coerceAtLeast(1)

    val listState = rememberLazyListState()
    // Показываем конец периода — свежие интервалы информативнее
    LaunchedEffect(points) {
        listState.scrollToItem(points.lastIndex)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ChartLegend(
            incomeColor = incomeColor,
            expenseColor = expenseColor,
            interestColor = interestColor,
            showInterest = hasInterest
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(points) { index, point ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(BAR_GROUP_WIDTH)
                ) {
                    BarGroup(
                        income = point.income,
                        expense = point.expense,
                        interest = point.interest,
                        maxValue = maxValue,
                        incomeColor = incomeColor,
                        expenseColor = expenseColor,
                        interestColor = interestColor,
                        axisColor = axisColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (index % labelStep == 0) bucketLabel(point, bucket) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Столбцы дохода и расхода с общей базовой линией. */
@Composable
private fun BarGroup(
    income: Long,
    expense: Long,
    interest: Long,
    maxValue: Long,
    incomeColor: Color,
    expenseColor: Color,
    interestColor: Color,
    axisColor: Color
) {
    Canvas(
        modifier = Modifier
            .width(BAR_GROUP_WIDTH)
            .height(CHART_HEIGHT)
    ) {
        val barWidth = size.width * 0.4f
        val gap = size.width * 0.1f
        val corner = CornerRadius(barWidth / 3f, barWidth / 3f)

        drawLine(
            color = axisColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1f
        )

        // Столбец доходов: сначала проценты по копилкам, затем сами доходы
        val incomeHeight = barHeight(income, maxValue)
        val interestHeight = barHeight(interest, maxValue)

        if (interest > 0) {
            drawRoundRect(
                color = interestColor,
                topLeft = Offset(gap, size.height - incomeHeight - interestHeight),
                size = Size(barWidth, interestHeight),
                cornerRadius = corner
            )
        }

        if (income > 0) {
            drawRoundRect(
                color = incomeColor,
                topLeft = Offset(gap, size.height - incomeHeight),
                size = Size(barWidth, incomeHeight),
                cornerRadius = corner
            )
        }

        drawBar(
            left = gap * 2f + barWidth,
            width = barWidth,
            value = expense,
            maxValue = maxValue,
            color = expenseColor,
            corner = corner
        )
    }
}

/** Высота столбца для суммы [value] при максимуме [maxValue]. */
private fun DrawScope.barHeight(value: Long, maxValue: Long): Float =
    (value.toFloat() / maxValue * size.height).coerceAtLeast(0f)

private fun DrawScope.drawBar(
    left: Float,
    width: Float,
    value: Long,
    maxValue: Long,
    color: Color,
    corner: CornerRadius
) {
    val height = barHeight(value, maxValue)
    if (height <= 0f) return

    drawRoundRect(
        color = color,
        topLeft = Offset(left, size.height - height),
        size = Size(width, height),
        cornerRadius = corner
    )
}

@Composable
private fun ChartLegend(
    incomeColor: Color,
    expenseColor: Color,
    interestColor: Color,
    showInterest: Boolean
) {
    // Легенда без переноса: столпотворение подписей обрезало бы её на узком экране
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendMark(color = incomeColor, label = stringResource(R.string.analytics_legend_income))
            LegendMark(color = expenseColor, label = stringResource(R.string.analytics_legend_expense))
        }

        if (showInterest) {
            LegendMark(
                color = interestColor,
                label = stringResource(R.string.analytics_interest_category)
            )
        }
    }
}

@Composable
private fun LegendMark(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = color) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

/** Подпись под столбцом: день или месяц. */
@Composable
private fun bucketLabel(point: PeriodPoint, bucket: AnalyticsBucket): String = when (bucket) {
    AnalyticsBucket.DAY -> "%02d.%02d".format(point.date.dayOfMonth, point.date.monthValue)
    AnalyticsBucket.MONTH -> stringResource(MONTHS_SHORT[point.date.monthValue - 1])
}

private val BAR_GROUP_WIDTH: Dp = 44.dp
private val CHART_HEIGHT: Dp = 140.dp

/** Сколько подписей по оси показываем одновременно. */
private const val MAX_LABELS = 12

private val MONTHS_SHORT = listOf(
    R.string.month_jan, R.string.month_feb, R.string.month_mar, R.string.month_apr,
    R.string.month_may, R.string.month_jun, R.string.month_jul, R.string.month_aug,
    R.string.month_sep, R.string.month_oct, R.string.month_nov, R.string.month_dec
)