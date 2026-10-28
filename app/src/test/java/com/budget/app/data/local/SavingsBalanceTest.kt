package com.budget.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.budget.app.data.local.dao.SavingsDao
import com.budget.app.data.local.dao.TransactionDao
import com.budget.app.data.local.dao.WalletDao
import com.budget.app.data.local.entity.SavingsEntity
import com.budget.app.data.local.entity.SavingsOperationEntity
import com.budget.app.data.local.entity.TransactionEntity
import com.budget.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Копилки: баланс считается запросом [SavingsDao.getSavingsWithBalance],
 * пополнение уменьшает кошелёк, вывод — увеличивает, проценты кошельков не касаются,
 * а операции по копилкам не попадают в доходы и расходы.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SavingsBalanceTest {

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
    fun `new savings has zero balance`() = runBlocking {
        val savingsId = savings("Отпуск")

        assertEquals(0L, balanceOf(savingsId))
    }

    @Test
    fun `deposit increases savings and decreases the wallet`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        val savingsId = savings("Отпуск")

        savingsDao.insertOperation(operation(savingsId, walletId, 30_000, "DEPOSIT"))

        assertEquals(30_000L, balanceOf(savingsId))
        assertEquals(70_000L, walletBalanceOf(walletId))
    }

    @Test
    fun `withdraw returns money from savings to the wallet`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        val savingsId = savings("Отпуск")
        savingsDao.insertOperation(operation(savingsId, walletId, 30_000, "DEPOSIT"))
        savingsDao.insertOperation(operation(savingsId, walletId, 12_000, "WITHDRAW"))

        assertEquals(18_000L, balanceOf(savingsId))
        assertEquals(82_000L, walletBalanceOf(walletId))
    }

    @Test
    fun `withdrawing everything empties the savings`() = runBlocking {
        val walletId = wallet("Карта", 50_000)
        val savingsId = savings("Отпуск")
        savingsDao.insertOperation(operation(savingsId, walletId, 20_000, "DEPOSIT"))
        savingsDao.insertOperation(operation(savingsId, walletId, 20_000, "WITHDRAW"))

        assertEquals(0L, balanceOf(savingsId))
        assertEquals(50_000L, walletBalanceOf(walletId))
    }

    @Test
    fun `interest increases only the savings`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        val savingsId = savings("Вклад")
        savingsDao.insertOperation(operation(savingsId, walletId, 40_000, "DEPOSIT"))
        savingsDao.insertOperation(operation(savingsId, null, 1_500, "INTEREST"))

        assertEquals(41_500L, balanceOf(savingsId))
        // Кошелёк не тронут: у процентов walletId = NULL
        assertEquals(60_000L, walletBalanceOf(walletId))
    }

    @Test
    fun `interest counts as income once and does not double-count the wallet`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        val savingsId = savings("Вклад")
        savingsDao.insertOperation(operation(savingsId, walletId, 40_000, "DEPOSIT"))
        savingsDao.insertOperation(operation(savingsId, null, 1_500, "INTEREST"))

        val summary = savingsDao.getPeriodSummary(0L, Long.MAX_VALUE).first()
        val operations = transactionDao.getTransactionsWithDetails(0L, Long.MAX_VALUE).first()

        // В аналитике проценты берутся из savings_operations и ровно один раз
        assertEquals(1_500L, summary.interest)
        // В transactions процентов нет — двойного счёта быть не может
        assertTrue(operations.isEmpty())
    }

    @Test
    fun `deposit and withdraw are not treated as expense or income`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        val savingsId = savings("Отпуск")
        savingsDao.insertOperation(operation(savingsId, walletId, 30_000, "DEPOSIT"))
        savingsDao.insertOperation(operation(savingsId, walletId, 5_000, "WITHDRAW"))

        val operations = transactionDao.getTransactionsWithDetails(0L, Long.MAX_VALUE).first()

        assertTrue(operations.isEmpty())
    }

    @Test
    fun `period summary counts deposits, withdrawals and interest separately`() = runBlocking {
        val walletId = wallet("Карта", 200_000)
        val savingsId = savings("Отпуск")
        savingsDao.insertOperation(operation(savingsId, walletId, 30_000, "DEPOSIT"))
        savingsDao.insertOperation(operation(savingsId, walletId, 10_000, "DEPOSIT"))
        savingsDao.insertOperation(operation(savingsId, walletId, 7_000, "WITHDRAW"))
        savingsDao.insertOperation(operation(savingsId, null, 900, "INTEREST"))

        val summary = savingsDao.getPeriodSummary(0L, Long.MAX_VALUE).first()

        assertEquals(40_000L, summary.deposited)
        assertEquals(7_000L, summary.withdrawn)
        assertEquals(900L, summary.interest)
    }

    @Test
    fun `period summary respects period bounds`() = runBlocking {
        val savingsId = savings("Отпуск")
        savingsDao.insertOperation(operation(savingsId, null, 1_000, "DEPOSIT", date = DAY_ONE))
        savingsDao.insertOperation(operation(savingsId, null, 2_000, "DEPOSIT", date = DAY_THREE))

        val summary = savingsDao.getPeriodSummary(DAY_TWO, DAY_FOUR).first()

        assertEquals(2_000L, summary.deposited)
    }

    @Test
    fun `savings without goal has no target`() = runBlocking {
        val savingsId = savings("На чёрный день")

        assertNull(savingsDao.getSavingsWithBalance().first().first { it.id == savingsId }.targetAmount)
    }

    @Test
    fun `savings keeps its goal and creation date`() = runBlocking {
        val savingsId = savings("Отпуск", target = 150_000)
        savingsDao.insertOperation(operation(savingsId, null, 50_000, "DEPOSIT"))

        val saved = savingsDao.getSavingsWithBalance().first().first { it.id == savingsId }

        assertEquals("Отпуск", saved.name)
        assertEquals(150_000L, saved.targetAmount)
        assertEquals(DATE, saved.createdAt)
        assertEquals(50_000L, saved.balance)
    }

    @Test
    fun `several savings keep their own balances`() = runBlocking {
        val firstId = savings("Отпуск")
        val secondId = savings("Машина")
        savingsDao.insertOperation(operation(firstId, null, 10_000, "DEPOSIT"))
        savingsDao.insertOperation(operation(secondId, null, 25_000, "DEPOSIT"))

        assertEquals(10_000L, balanceOf(firstId))
        assertEquals(25_000L, balanceOf(secondId))
        assertEquals(35_000L, savingsDao.getSavingsWithBalance().first().sumOf { it.balance })
    }

    @Test
    fun `operations are stored together with savings and wallet names`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        val savingsId = savings("Отпуск")
        savingsDao.insertOperation(operation(savingsId, walletId, 30_000, "DEPOSIT"))

        val saved = savingsDao.getOperationsWithDetails().first().single()

        assertEquals("Отпуск", saved.savingsName)
        assertEquals("Карта", saved.walletName)
        assertEquals(30_000L, saved.amount)
        assertEquals("DEPOSIT", saved.type)
    }

    @Test
    fun `deleting a savings removes its operations`() = runBlocking {
        val savingsId = savings("Отпуск")
        savingsDao.insertOperation(operation(savingsId, null, 10_000, "DEPOSIT"))

        savingsDao.deleteSavingsById(savingsId)

        assertTrue(savingsDao.getSavingsWithBalance().first().isEmpty())
        assertTrue(savingsDao.getOperationsWithDetails().first().isEmpty())
    }

    @Test
    fun `deleting an operation returns the money to the wallet`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        val savingsId = savings("Отпуск")
        val operationId = savingsDao.insertOperation(operation(savingsId, walletId, 30_000, "DEPOSIT"))

        savingsDao.deleteOperationById(operationId)

        assertEquals(0L, balanceOf(savingsId))
        assertEquals(100_000L, walletBalanceOf(walletId))
    }

    @Test
    fun `wallet used by savings operations cannot be deleted`() = runBlocking {
        val walletId = wallet("Карта", 100_000)
        val savingsId = savings("Отпуск")
        savingsDao.insertOperation(operation(savingsId, walletId, 30_000, "DEPOSIT"))

        assertFalse(walletDao.deleteWalletIfUnused(walletId) > 0)
    }

    @Test
    fun `wallet without operations and savings is still deleted`() = runBlocking {
        wallet("Карта", 100_000)
        val spareId = wallet("Запасной", 0)

        assertTrue(walletDao.deleteWalletIfUnused(spareId) > 0)
    }

    @Test
    fun `savings operations are counted together with transfers and expenses in the wallet balance`() =
        runBlocking {
            val walletId = wallet("Карта", 100_000)
            val savingsId = savings("Отпуск")
            transactionDao.insertTransaction(expense(walletId, 10_000))
            savingsDao.insertOperation(operation(savingsId, walletId, 25_000, "DEPOSIT"))
            savingsDao.insertOperation(operation(savingsId, walletId, 5_000, "WITHDRAW"))

            assertEquals(70_000L, walletBalanceOf(walletId))
        }

    @Test
    fun `operations of one savings do not affect another`() = runBlocking {
        val firstId = savings("Отпуск")
        val secondId = savings("Машина")
        savingsDao.insertOperation(operation(firstId, null, 10_000, "DEPOSIT"))

        assertEquals(10_000L, balanceOf(firstId))
        assertEquals(0L, balanceOf(secondId))
    }

    private suspend fun savings(name: String, target: Long? = null): Long =
        savingsDao.insertSavings(
            SavingsEntity(name = name, createdAt = DATE, targetAmount = target)
        )

    private suspend fun wallet(name: String, initialBalance: Long): Long =
        walletDao.insertWallet(
            WalletEntity(name = name, type = "CARD", initialBalance = initialBalance)
        )

    private suspend fun balanceOf(savingsId: Long): Long =
        savingsDao.getSavingsWithBalance().first().first { it.id == savingsId }.balance

    private suspend fun walletBalanceOf(walletId: Long): Long =
        walletDao.getActiveWalletsWithBalance().first().first { it.id == walletId }.balance

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
        const val DAY_ONE = 1_700_000_000_000L
        const val DAY_TWO = 1_700_000_000_000L + 86_400_000L
        const val DAY_THREE = 1_700_000_000_000L + 2 * 86_400_000L
        const val DAY_FOUR = 1_700_000_000_000L + 3 * 86_400_000L
    }
}