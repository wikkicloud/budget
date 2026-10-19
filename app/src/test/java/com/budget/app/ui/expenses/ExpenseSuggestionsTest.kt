package com.budget.app.ui.expenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseSuggestionsTest {

    private var nextId = 1L

    private fun expense(
        description: String,
        date: Long,
        categoryId: Long? = 1L,
        walletId: Long = 1L,
        amount: Long = 10_000
    ) = ExpenseUi(
        id = nextId++,
        amount = amount,
        description = description,
        date = date,
        categoryId = categoryId,
        categoryName = null,
        categoryIconKey = null,
        categoryColor = null,
        walletId = walletId,
        walletName = "Карта"
    )

    @Test
    fun `empty query returns most frequent descriptions`() {
        val expenses = listOf(
            expense("Кофе", 100),
            expense("Кофе", 200),
            expense("Обед", 300)
        )

        val result = ExpenseSuggestions.build(expenses, "")

        assertEquals(listOf("Кофе", "Обед"), result.map { it.description })
    }

    @Test
    fun `query matches words without case`() {
        val expenses = listOf(
            expense("Утренний кофе", 100),
            expense("Обед", 200)
        )

        val result = ExpenseSuggestions.build(expenses, "кофе")

        assertEquals(listOf("Утренний кофе"), result.map { it.description })
    }

    @Test
    fun `query matches consecutive word prefixes`() {
        val expenses = listOf(expense("Кофе с собой", 100))

        assertEquals(
            listOf("Кофе с собой"),
            ExpenseSuggestions.build(expenses, "кофе с").map { it.description }
        )
    }

    @Test
    fun `exact match with different case is not suggested`() {
        val expenses = listOf(expense("Кофе", 100))

        assertTrue(ExpenseSuggestions.build(expenses, "кофе").isEmpty())
    }

    @Test
    fun `fewer than five suggestions are returned`() {
        val expenses = (1..4).map { expense("Покупка $it", it.toLong()) }

        assertEquals(4, ExpenseSuggestions.build(expenses, "").size)
    }

    @Test
    fun `no more than five suggestions are returned`() {
        val expenses = (1..9).map { expense("Покупка $it", it.toLong()) }

        assertEquals(ExpenseSuggestions.LIMIT, ExpenseSuggestions.build(expenses, "").size)
    }

    @Test
    fun `suggestion carries category wallet and amount of the latest expense`() {
        val expenses = listOf(
            expense("Кофе", 100, categoryId = 5L, walletId = 7L, amount = 25_000),
            expense("Кофе", 200, categoryId = 9L, walletId = 3L, amount = 30_000)
        )

        val suggestion = ExpenseSuggestions.build(expenses, "").single()

        assertEquals(9L, suggestion.categoryId)
        assertEquals(3L, suggestion.walletId)
        assertEquals(30_000L, suggestion.amount)
    }

    @Test
    fun `expense without category keeps null category`() {
        val expenses = listOf(expense("Прочее", 100, categoryId = null))

        assertEquals(null, ExpenseSuggestions.build(expenses, "").single().categoryId)
    }

    @Test
    fun `blank descriptions are skipped`() {
        val expenses = listOf(expense("   ", 100), expense("Кофе", 200))

        assertEquals(listOf("Кофе"), ExpenseSuggestions.build(expenses, "").map { it.description })
    }

    @Test
    fun `equal frequency is ordered by newest expense`() {
        val expenses = listOf(
            expense("Старое", 100),
            expense("Новое", 500)
        )

        assertEquals(
            listOf("Новое", "Старое"),
            ExpenseSuggestions.build(expenses, "").map { it.description }
        )
    }

    @Test
    fun `query ignores punctuation and digits boundaries`() {
        val expenses = listOf(expense("Кофе, чай", 100))

        assertEquals(
            listOf("Кофе, чай"),
            ExpenseSuggestions.build(expenses, "чай").map { it.description }
        )
    }
}