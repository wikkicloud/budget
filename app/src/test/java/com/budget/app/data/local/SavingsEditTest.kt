package com.budget.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.budget.app.data.local.dao.SavingsDao
import com.budget.app.data.local.dao.TransactionDao
import com.budget.app.data.local.dao.WalletDao
import com.budget.app.data.local.entity.SavingsEntity
import com.budget.app.data.local.entity.SavingsOperationEntity
import com.budget.app.data.local.entity.WalletEntity
import com.budget.app.domain.model.SavingsProgress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Стартовая сумма копилки и редактирование копилки: стартовая сумма входит в баланс
 * (и в прогресс по цели), кошельков не касается и доходом в аналитике не считается;
 * при редактировании меняются название, цель и стартовая сумма, а дата создания остаётся прежней.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SavingsEditTest {

    private lateinit var database: BudgetDatabase
    private lateinit var walletDao: WalletDao
    private lateinit var savingsDao: SavingsDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BudgetDatabase::class.java
        ).allowMainThreadQueries().build()

        walletDao = database.walletDao()
        savingsDao = database.savingsDao()
        transactionDao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `initial amount is included in the savings balance`() = runBlocking {
        val savingsId = savings("Отпуск", initial = 50_000)

        assertEquals(50_000L, balanceOf(savingsId))
    }

    @Test
    fun `initial amount adds up with deposits`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        val savingsId = savings("Отпуск", initial = 50_000)
        savingsDao.insertOperation(operation(savingsId, walletId, 30_000, "DEPOSIT"))

        assertEquals(80_000L, balanceOf(savingsId))
    }

    @Test
    fun `initial amount is stored and returned with the savings`() = runBlocking {
        val savingsId = savings("Отпуск", initial = 50_000)

        val saved = savingsDao.getSavingsWithBalance().first().first { it.id == savingsId }

        assertEquals(50_000L, saved.initialAmount)
    }

    @Test
    fun `savings without initial amount starts from zero`() = runBlocking {
        val savingsId = savings("Отпуск")

        assertEquals(0L, balanceOf(savingsId))
        assertEquals(
            0L,
            savingsDao.getSavingsWithBalance().first().first { it.id == savingsId }.initialAmount
        )
    }

    @Test
    fun `initial amount does not touch wallets`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        savings("Отпуск", initial = 50_000)

        assertEquals(100_000L, walletBalanceOf(walletId))
    }

    @Test
    fun `initial amount is not an operation and is not income`() = runBlocking {
        savings("Отпуск", initial = 50_000)

        // Стартовая сумма — поле копилки, а не операция: ни в истории операций, ни в доходах её нет
        assertTrue(savingsDao.getOperationsWithDetails().first().isEmpty())
        assertTrue(transactionDao.getTransactionsWithDetails(0L, Long.MAX_VALUE).first().isEmpty())
        assertEquals(0L, savingsDao.getPeriodSummary(0L, Long.MAX_VALUE).first().deposited)
    }

    @Test
    fun `initial amount counts toward the goal`() = runBlocking {
        val savingsId = savings("Отпуск", target = 100_000, initial = 25_000)

        val saved = savingsDao.getSavingsWithBalance().first().first { it.id == savingsId }

        assertEquals(0.25f, SavingsProgress.fraction(saved.balance, saved.targetAmount)!!, 0.0001f)
        assertEquals(25, SavingsProgress.percent(saved.balance, saved.targetAmount))
    }

    @Test
    fun `initial amount is included in the total of all savings`() = runBlocking {
        savings("Отпуск", initial = 50_000)
        savings("Машина", initial = 25_000)

        assertEquals(75_000L, savingsDao.getSavingsWithBalance().first().sumOf { it.balance })
    }

    @Test
    fun `editing changes name, goal and initial amount`() = runBlocking {
        val savingsId = savings("Отпуск", target = 100_000, initial = 10_000)

        savingsDao.updateSavings(
            SavingsEntity(
                id = savingsId,
                name = "Море",
                createdAt = DATE,
                targetAmount = 200_000,
                initialAmount = 60_000
            )
        )

        val saved = savingsDao.getSavingsWithBalance().first().first { it.id == savingsId }
        assertEquals("Море", saved.name)
        assertEquals(200_000L, saved.targetAmount)
        assertEquals(60_000L, saved.initialAmount)
        assertEquals(60_000L, saved.balance)
    }

    @Test
    fun `editing keeps the creation date`() = runBlocking {
        val savingsId = savings("Отпуск")

        val existing = savingsDao.getSavingsById(savingsId)!!
        savingsDao.updateSavings(existing.copy(name = "Море", createdAt = existing.createdAt))

        assertEquals(DATE, savingsDao.getSavingsById(savingsId)!!.createdAt)
    }

    @Test
    fun `editing can remove the goal`() = runBlocking {
        val savingsId = savings("Отпуск", target = 100_000)

        savingsDao.updateSavings(
            SavingsEntity(id = savingsId, name = "Отпуск", createdAt = DATE, targetAmount = null)
        )

        val saved = savingsDao.getSavingsWithBalance().first().first { it.id == savingsId }
        assertNull(saved.targetAmount)
        assertNull(SavingsProgress.fraction(saved.balance, saved.targetAmount))
    }

    @Test
    fun `editing does not touch operations or wallets`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        val savingsId = savings("Отпуск")
        savingsDao.insertOperation(operation(savingsId, walletId, 30_000, "DEPOSIT"))

        savingsDao.updateSavings(
            SavingsEntity(
                id = savingsId,
                name = "Море",
                createdAt = DATE,
                targetAmount = 200_000,
                initialAmount = 20_000
            )
        )

        assertEquals(50_000L, balanceOf(savingsId))
        assertEquals(1, savingsDao.getOperationsWithDetails().first().size)
        assertEquals(70_000L, walletBalanceOf(walletId))
    }

    @Test
    fun `editing one savings does not affect another`() = runBlocking {
        val firstId = savings("Отпуск")
        val secondId = savings("Машина")

        savingsDao.updateSavings(
            SavingsEntity(id = firstId, name = "Море", createdAt = DATE, initialAmount = 40_000)
        )

        assertEquals(40_000L, balanceOf(firstId))
        assertEquals(0L, balanceOf(secondId))
    }

    @Test
    fun `initial amount can be lowered back to zero`() = runBlocking {
        val savingsId = savings("Отпуск", initial = 50_000)

        savingsDao.updateSavings(
            SavingsEntity(id = savingsId, name = "Отпуск", createdAt = DATE, initialAmount = 0)
        )

        assertEquals(0L, balanceOf(savingsId))
    }

    private suspend fun savings(name: String, target: Long? = null, initial: Long = 0): Long =
        savingsDao.insertSavings(
            SavingsEntity(
                name = name,
                createdAt = DATE,
                targetAmount = target,
                initialAmount = initial
            )
        )

    private suspend fun wallet(name: String, initialBalance: Long): Long =
        walletDao.insertWallet(
            WalletEntity(name = name, type = "CARD", initialBalance = initialBalance)
        )

    private suspend fun balanceOf(savingsId: Long): Long =
        savingsDao.getSavingsWithBalance().first().first { it.id == savingsId }.balance

    private suspend fun walletBalanceOf(walletId: Long): Long =
        walletDao.getAllWalletsWithBalance().first().first { it.id == walletId }.balance

    private fun operation(
        savingsId: Long,
        walletId: Long?,
        amount: Long,
        type: String,
        date: Long = DATE
    ) = SavingsOperationEntity(
        savingsId = savingsId,
        walletId = walletId,
        amount = amount,
        type = type,
        date = date
    )

    private companion object {
        const val DATE = 1_700_000_000_000L
    }
}