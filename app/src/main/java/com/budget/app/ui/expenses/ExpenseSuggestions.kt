package com.budget.app.ui.expenses

import java.util.Locale

/** Подсказка описания расхода, собранная из уже сохранённых расходов. */
data class ExpenseSuggestion(
    val description: String,
    /** Категория последнего расхода с таким описанием; null — расход был без категории. */
    val categoryId: Long?,
    /** Кошелёк последнего расхода с таким описанием. */
    val walletId: Long,
    /** Сумма последнего расхода с таким описанием, в копейках. */
    val amount: Long
)

/**
 * Подсказки описания строятся по уже загруженному списку расходов: отдельная таблица,
 * запрос в базу и индекс по описанию не нужны.
 */
object ExpenseSuggestions {

    /** Сколько подсказок показывать. */
    const val LIMIT = 5

    /** Слова описания — последовательности букв и цифр, всё остальное разделитель. */
    private val WORD_SEPARATORS = Regex("[^\\p{L}\\p{N}]+")

    /**
     * @param expenses все расходы; порядок в списке не важен.
     * @param query текущий ввод описания; пустой — вернуть до [LIMIT] самых частых описаний.
     * @return до [LIMIT] подсказок: сначала самые частые, при равной частоте — самые свежие.
     */
    fun build(expenses: List<ExpenseUi>, query: String): List<ExpenseSuggestion> {
        val trimmedQuery = query.trim()

        // Ключ — описание без учёта регистра; значение — последний расход с таким описанием и частота.
        val groups = LinkedHashMap<String, Group>()

        expenses
            .filter { it.description.isNotBlank() }
            .sortedWith(compareByDescending<ExpenseUi> { it.date }.thenByDescending { it.id })
            .forEach { expense ->
                val key = expense.description.trim().lowercase(Locale.ROOT)
                val group = groups[key]
                if (group == null) {
                    groups[key] = Group(newest = expense)
                } else {
                    group.count++
                }
            }

        return groups.values
            .filter { group ->
                val newest = group.newest.description.trim()
                trimmedQuery.isEmpty() ||
                    (!newest.equals(trimmedQuery, ignoreCase = true) && matchesByWords(newest, trimmedQuery))
            }
            .sortedWith(
                compareByDescending<Group> { it.count }
                    .thenByDescending { it.newest.date }
                    .thenByDescending { it.newest.id }
            )
            .take(LIMIT)
            .map { group ->
                ExpenseSuggestion(
                    description = group.newest.description.trim(),
                    categoryId = group.newest.categoryId,
                    walletId = group.newest.walletId,
                    amount = group.newest.amount
                )
            }
    }

    /**
     * Слова запроса подряд совпадают с началом слов описания — так «кофе» находит «Утренний кофе»,
     * а «кофе с» — «Кофе с собой». Совпадение — без учёта регистра.
     */
    private fun matchesByWords(description: String, query: String): Boolean {
        val descriptionWords = splitWords(description)
        val queryWords = splitWords(query)
        if (queryWords.isEmpty()) return true
        if (queryWords.size > descriptionWords.size) return false

        for (start in 0..descriptionWords.size - queryWords.size) {
            val matched = queryWords.indices.all { offset ->
                descriptionWords[start + offset].startsWith(queryWords[offset], ignoreCase = true)
            }
            if (matched) return true
        }
        return false
    }

    private fun splitWords(value: String): List<String> =
        value.trim().split(WORD_SEPARATORS).filter { it.isNotEmpty() }

    /** Описание со статистикой: самая свежая запись и число использований. */
    private class Group(val newest: ExpenseUi, var count: Int = 1)
}