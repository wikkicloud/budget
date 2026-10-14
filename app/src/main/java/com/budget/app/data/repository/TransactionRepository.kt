package com.budget.app.data.repository

import com.budget.app.data.local.dao.TransactionDao
import com.budget.app.data.local.entity.TransactionEntity
import com.budget.app.data.local.model.ExpenseWithDetails
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    /** Расходы вместе с названиями кошелька и категории — для списка на экране. */
    fun getExpensesWithDetails(): Flow<List<ExpenseWithDetails>> =
        transactionDao.getExpensesWithDetails()

    fun getTransactionsByWallet(walletId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByWallet(walletId)

    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCategory(categoryId)

    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByType(type)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) = transactionDao.deleteTransactionById(id)

    suspend fun getTotalIncomeForWallet(walletId: Long): Long =
        transactionDao.getTotalIncomeForWallet(walletId) ?: 0L

    suspend fun getTotalExpenseForWallet(walletId: Long): Long =
        transactionDao.getTotalExpenseForWallet(walletId) ?: 0L
}
