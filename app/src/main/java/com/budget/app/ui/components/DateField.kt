package com.budget.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.budget.app.R
import com.budget.app.util.DateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Поле выбора даты операции: кнопка с текущей датой, по нажатию — календарь Material 3.
 * Выбранная дата сохраняется как начало дня в системной зоне (как хранят дату операции).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: Long,
    onDateChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerVisible by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateFormatter.toPickerMillis(DateFormatter.toLocalDate(date))
    )

    OutlinedButton(
        onClick = { pickerVisible = true },
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null
        )
        Text(
            text = DateFormatter.formatDate(date),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    if (pickerVisible) {
        DatePickerDialog(
            onDismissRequest = { pickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerVisible = false
                        pickerState.selectedDateMillis?.let { millis ->
                            // Дата из календаря — UTC-полночь; операция хранит начало дня в системной зоне
                            val localDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onDateChange(DateFormatter.toTimestamp(localDate))
                        }
                    }
                ) {
                    Text(stringResource(R.string.date_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickerVisible = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                showModeToggle = false,
                dateFormatter = remember { RussianDateFormatter },
                title = {
                    Text(
                        text = stringResource(R.string.date_title),
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val RUSSIAN_LOCALE: Locale = Locale.forLanguageTag("ru")

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