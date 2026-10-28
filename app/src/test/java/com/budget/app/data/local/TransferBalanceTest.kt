package com.budget.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.budget.app.data.local.dao.TransactionDao
import com.budget.app.data.local.dao.TransferDao
import com.budget.app.data.local.dao.WalletDao
import com.budget.app.data.local.entity.TransactionEntity
import com.budget.app.data.local.entity.TransferEntity
import com.budget.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Переводы между кошельками: балансы считаются запросом [WalletDao.getActiveWalletsWithBalance],
 * итог по всем кошелькам при переводе не меняется, а доходы и расходы перевод не затрагивают.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TransferBalanceTest {

    private lateinit var database: BudgetDatabase
    private lateinit var walletDao: WalletDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var transferDao: TransferDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BudgetDatabase::class.java
        ).allowMainThreadQueries().build()

        walletDao = database.walletDao()
        transactionDao = database.transactionDao()
        transferDao = database.transferDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `transfer moves money from source to target`() = runBlocking {
        val cardId = wallet("Карта", 100_000)
        val cashId = wallet("Наличные", 20_000)

        transferDao.insertTransfer(transfer(cardId, cashId, 30_000))

        assertEquals(70_000L, balanceOf(cardId))
        assertEquals(50_000L, balanceOf(cashId))
    }

    @Test
    fun `total over all wallets does not change after a transfer`() = runBlocking {
        val cardId = wallet("Карта", 100_000)
        val cashId = wallet("Наличные", 20_000)

        val before = total()
        transferDao.insertTransfer(transfer(cardId, cashId, 30_000))
        val after = total()

        assertEquals(before, after)
        assertEquals(120_000L, after)
    }

    @Test
    fun `turning a transfer back returns both balances`() = runBlocking {
        val cardId = wallet("Карта", 100_000)
        val cashId = wallet("Наличные", 20_000)
        val transferId = transferDao.insertTransfer(transfer(cardId, cashId, 30_000))

        transferDao.deleteTransferById(transferId)

        assertEquals(100_000L, balanceOf(cardId))
        assertEquals(20_000L, balanceOf(cashId))
    }

    @Test
    fun `transfers and operations are counted together in the balance`() = runBlocking {
        val cardId = wallet("Карта", 100_000)
        val cashId = wallet("Наличные", 0)

        transactionDao.insertTransaction(expense(cardId, 15_000))
        transferDao.insertTransfer(transfer(cardId, cashId, 40_000))

        assertEquals(45_000L, balanceOf(cardId))
        assertEquals(40_000L, balanceOf(cashId))
    }

    @Test
    fun `transfer does not appear among incomes or expenses`() = runBlocking {
        val cardId = wallet("Карта", 100_000)
        val cashId = wallet("Наличные", 0)
        transferDao.insertTransfer(transfer(cardId, cashId, 40_000))

        // Аналитика читает только таблицу transactions за период — переводов там нет
        val operations = transactionDao.getTransactionsWithDetails(0L, Long.MAX_VALUE).first()

        assertTrue(operations.isEmpty())
    }

    @Test
    fun `wallet involved in a transfer cannot be deleted`() = runBlocking {
        val cardId = wallet("Карта", 100_000)
        val cashId = wallet("Наличные", 0)
        transferDao.insertTransfer(transfer(cardId, cashId, 40_000))

        assertFalse(walletDao.deleteWalletIfUnused(cardId) > 0)
        assertFalse(walletDao.deleteWalletIfUnused(cashId) > 0)
    }

    @Test
    fun `wallet without transfers and operations is still deleted`() = runBlocking {
        val cardId = wallet("Карта", 100_000)
        val cashId = wallet("Наличные", 0)
        transferDao.insertTransfer(transfer(cardId, cashId, 40_000))
        val spareId = wallet("Запасной", 0)

        assertTrue(walletDao.deleteWalletIfUnused(spareId) > 0)
    }

    @Test
    fun `transfer is kept together with wallet names`() = runBlocking {
        val cardId = wallet("Карта", 100_000)
        val cashId = wallet("Наличные", 0)
        transferDao.insertTransfer(transfer(cardId, cashId, 40_000, note = "На такси"))

        val saved = transferDao.getTransfersWithDetails().first().single()

        assertEquals("Карта", saved.fromWalletName)
        assertEquals("Наличные", saved.toWalletName)
        assertEquals(40_000L, saved.amount)
        assertEquals("На такси", saved.note)
    }

    @Test
    fun `note is optional`() = runBlocking {
        val cardId = wallet("Карта", 100_000)
        val cashId = wallet("Наличные", 0)
        transferDao.insertTransfer(transfer(cardId, cashId, 40_000))

        assertEquals(null, transferDao.getTransfersWithDetails().first().single().note)
    }

    private suspend fun wallet(name: String, initialBalance: Long): Long =
        walletDao.insertWallet(
            WalletEntity(name = name, type = "CARD", initialBalance = initialBalance)
        )

    private suspend fun balanceOf(walletId: Long): Long =
        walletDao.getActiveWalletsWithBalance().first().first { it.id == walletId }.balance

    private suspend fun total(): Long =
        walletDao.getActiveWalletsWithBalance().first().sumOf { it.balance }

    private fun transfer(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Long,
        note: String? = null
    ) = TransferEntity(
        fromWalletId = fromWalletId,
        toWalletId = toWalletId,
        amount = amount,
        date = DATE,
        note = note
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