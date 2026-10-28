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
 * [WalletDao.getActiveWalletsWithBalance], а не хранится отдельно.
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

        val total = walletDao.getActiveWalletsWithBalance().first().sumOf { it.balance }

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

    // --- Архив кошелька ---

    @Test
    fun `archived wallet disappears from active list and appears in archived`() = runBlocking {
        val walletId = walletDao.insertWallet(
            WalletEntity(name = "Карта", type = "CARD", initialBalance = 10_000)
        )
        walletDao.insertWallet(WalletEntity(name = "Наличные", type = "CASH", initialBalance = 5_000))

        walletDao.setWalletArchived(walletId, archived = true)

        assertEquals(listOf("Наличные"), walletDao.getActiveWalletsWithBalance().first().map { it.name })
        assertEquals(listOf("Карта"), walletDao.getArchivedWalletsWithBalance().first().map { it.name })
        assertEquals(listOf("Наличные"), walletDao.getAllWallets().first().map { it.name })
    }

    @Test
    fun `unarchiving returns the wallet to the active list`() = runBlocking {
        val walletId = walletDao.insertWallet(
            WalletEntity(name = "Карта", type = "CARD", initialBalance = 10_000)
        )

        walletDao.setWalletArchived(walletId, archived = true)
        walletDao.setWalletArchived(walletId, archived = false)

        assertEquals(listOf("Карта"), walletDao.getActiveWalletsWithBalance().first().map { it.name })
        assertEquals(0, walletDao.getArchivedWalletsWithBalance().first().size)
    }

    @Test
    fun `two archived wallets still leave the active one in the list`() = runBlocking {
        // Сценарий бага «экран кошельков пустеет при втором архивном кошельке».
        val firstId = walletDao.insertWallet(
            WalletEntity(name = "Карта", type = "CARD", initialBalance = 10_000)
        )
        val secondId = walletDao.insertWallet(
            WalletEntity(name = "Вторая карта", type = "CARD", initialBalance = 20_000)
        )
        walletDao.insertWallet(WalletEntity(name = "Наличные", type = "CASH", initialBalance = 5_000))

        walletDao.setWalletArchived(firstId, archived = true)
        walletDao.setWalletArchived(secondId, archived = true)

        assertEquals(listOf("Наличные"), walletDao.getActiveWalletsWithBalance().first().map { it.name })
        assertEquals(
            listOf("Карта", "Вторая карта"),
            walletDao.getArchivedWalletsWithBalance().first().map { it.name }
        )
        assertEquals(5_000L, walletDao.getActiveWalletsWithBalance().first().sumOf { it.balance })
    }

    @Test
    fun `archived wallet keeps its balance and is excluded from active total`() = runBlocking {
        val walletId = walletDao.insertWallet(WalletEntity(name = "Карта", type = "CARD", initialBalance = 10_000))
        walletDao.insertWallet(WalletEntity(name = "Наличные", type = "CASH", initialBalance = 20_000))
        transactionDao.insertTransaction(income(walletId, 5_000))
        transactionDao.insertTransaction(expense(walletId, 3_000))

        walletDao.setWalletArchived(walletId, archived = true)

        assertEquals(12_000L, walletDao.getArchivedWalletsWithBalance().first().single().balance)
        assertEquals(20_000L, walletDao.getActiveWalletsWithBalance().first().sumOf { it.balance })
    }

    private suspend fun balanceOf(walletId: Long): Long =
        walletDao.getActiveWalletsWithBalance().first().first { it.id == walletId }.balance

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