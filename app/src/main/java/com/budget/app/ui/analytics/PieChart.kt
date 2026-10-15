package com.budget.app.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.budget.app.R
import com.budget.app.util.ColorFormatter
import com.budget.app.util.MoneyFormatter
import androidx.compose.ui.text.style.TextAlign

/** Палитра для долей, у которых нет своего цвета категории. */
private val FALLBACK_COLORS = listOf(
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFFF9800),
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF00BCD4), Color(0xFF9E9E9E)
)

/** Кольцевая диаграмма расходов по категориям с легендой и суммами. */
@Composable
fun CategoryPieChart(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val colors = slices.mapIndexed { index, slice ->
        ColorFormatter.parse(slice.colorHex, FALLBACK_COLORS[index % FALLBACK_COLORS.size])
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val stroke = size.minDimension * 0.22f
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)

                // Пустое кольцо под долями — чтобы диаграмма читалась и при малых долях
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )

                var startAngle = -90f
                slices.forEachIndexed { index, slice ->
                    val sweep = slice.fraction * 360f
                    drawArc(
                        color = colors[index],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.analytics_total_expense),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = MoneyFormatter.format(slices.sumOf { it.amount }),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        slices.forEachIndexed { index, slice ->
            LegendRow(
                color = colors[index],
                label = slice.label(),
                amount = MoneyFormatter.format(slice.amount),
                fraction = slice.fraction
            )
        }
    }
}

/** Строка легенды: цвет, подпись, сумма и доля. */
@Composable
private fun LegendRow(
    color: Color,
    label: String,
    amount: String,
    fraction: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = color
        ) {}

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${percent(fraction)} · $amount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Подпись доли в процентах, например "42 %". */
internal fun percent(fraction: Float): String {
    val whole = (fraction * 100f).toInt()
    return "$whole %"
}

/** Имя категории для легенды; для операций без категории — строка из ресурсов. */
@Composable
private fun CategorySlice.label(): String =
    if (name == null) {
        stringResource(R.string.analytics_no_category)
    } else {
        listOfNotNull(icon, name).joinToString(" ")
    }