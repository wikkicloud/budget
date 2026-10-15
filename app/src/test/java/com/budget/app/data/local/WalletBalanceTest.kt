package com.budget.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.budget.app.data.local.dao.TransactionDao
import com.budget.app.data.local.dao.WalletDao
import com.budget.app.data.local.entity.TransactionEntity
import com.budget.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Проверяет расчёт баланса кошелька на настоящем SQLite: баланс считается запросом
 * [WalletDao.getAllWalletsWithBalance], а не хранится отдельно.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WalletBalanceTest {

    private lateinit var database: BudgetDatabase
    private lateinit var walletDao: WalletDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BudgetDatabase::class.java
        ).allowMainThreadQueries().build()

        walletDao = database.walletDao()
        transactionDao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `balance of a wallet without transactions equals its initial balance`() = runBlocking {
        val walletId = walletDao.insertWallet(WalletEntity(name = "Карта", type = "CARD", initialBalance = 10_000))

        val balance = balanceOf(walletId)

        assertEquals(10_000L, balance)
    }

    @Test
    fun `income increases the balance`() = runBlocking {
        val walletId = walletDao.insertWallet(WalletEntity(name = "Карта", type = "CARD", initialBalance = 0))
        transactionDao.insertTransaction(income(walletId, 150_000))
        transactionDao.insertTransaction(income(walletId, 50_000))

        assertEquals(200_000L, balanceOf(walletId))
    }

    @Test
    fun `expenses decrease the balance`() = runBlocking {
        val walletId = walletDao.insertWallet(WalletEntity(name = "Карта", type = "CARD", initialBalance = 200_000))
        transactionDao.insertTransaction(expense(walletId, 30_000))
        transactionDao.insertTransaction(expense(walletId, 45_500))

        assertEquals(124_500L, balanceOf(walletId))
    }

    @Test
    fun `balance mixes initial amount, income and expenses`() = runBlocking {
        val walletId = walletDao.insertWallet(WalletEntity(name = "Наличные", type = "CASH", initialBalance = 100_000))
        transactionDao.insertTransaction(income(walletId, 25_000))
        transactionDao.insertTransaction(expense(walletId, 120_000))

        assertEquals(5_000L, balanceOf(walletId))
    }

    @Test
    fun `balance can go negative`() = runBlocking {
        val walletId = walletDao.insertWallet(WalletEntity(name = "Карта", type = "CARD", initialBalance = 1_000))
        transactionDao.insertTransaction(expense(walletId, 50_000))

        assertEquals(-49_000L, balanceOf(walletId))
    }

    @Test
    fun `operations of one wallet do not affect another`() = runBlocking {
        val cardId = walletDao.insertWallet(WalletEntity(name = "Карта", type = "CARD", initialBalance = 10_000))
        val cashId = walletDao.insertWallet(WalletEntity(name = "Наличные", type = "CASH", initialBalance = 20_000))

        transactionDao.insertTransaction(income(cardId, 5_000))
        transactionDao.insertTransaction(expense(cashId, 7_000))

        assertEquals(15_000L, balanceOf(cardId))
        assertEquals(13_000L, balanceOf(cashId))
    }

    @Test
    fun `total over all wallets is the sum of balances`() = runBlocking {
        val firstId = walletDao.insertWallet(WalletEntity(name = "Карта", type = "CARD", initialBalance = 10_000))
        walletDao.insertWallet(WalletEntity(name = "Наличные", type = "CASH", initialBalance = 20_000))
        transactionDao.insertTransaction(expense(firstId, 4_000))

        val total = walletDao.getAllWalletsWithBalance().first().sumOf { it.balance }

        assertEquals(26_000L, total)
    }

    @Test
    fun `deleting an operation returns the balance to its previous value`() = runBlocking {
        val walletId = walletDao.insertWallet(WalletEntity(name = "Карта", type = "CARD", initialBalance = 10_000))
        val expenseId = transactionDao.insertTransaction(expense(walletId, 3_000))

        assertEquals(7_000L, balanceOf(walletId))

        transactionDao.deleteTransactionById(expenseId)

        assertEquals(10_000L, balanceOf(walletId))
    }

    private suspend fun balanceOf(walletId: Long): Long =
        walletDao.getAllWalletsWithBalance().first().first { it.id == walletId }.balance

    private fun income(walletId: Long, amount: Long) = TransactionEntity(
        type = "INCOME",
        amount = amount,
        description = "",
        categoryId = null,
        walletId = walletId,
        date = DATE
    )

    private fun expense(walletId: Long, amount: Long) = TransactionEntity(
        type = "EXPENSE",
        amount = amount,
        description = "",
        categoryId = null,
        walletId = walletId,
        date = DATE
    )

    private companion object {
        const val DATE = 1_700_000_000_000L
    }
}