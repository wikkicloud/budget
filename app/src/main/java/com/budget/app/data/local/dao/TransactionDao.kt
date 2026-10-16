package com.budget.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.budget.app.data.local.entity.TransactionEntity
import com.budget.app.data.local.model.ExpenseWithDetails
import com.budget.app.data.local.model.IncomeWithDetails
import com.budget.app.data.local.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT t.id, t.amount, t.description, t.date,
            t.categoryId, c.name AS categoryName, COALESCE(c.iconKey, c.icon) AS categoryIcon,
            c.color AS categoryColor,
            t.walletId, w.name AS walletName, w.type AS walletType
        FROM transactions t
        INNER JOIN wallets w ON w.id = t.walletId
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE t.type = 'EXPENSE'
        ORDER BY t.date DESC, t.id DESC
        """
    )
    fun getExpensesWithDetails(): Flow<List<ExpenseWithDetails>>

    @Query(
        """
        SELECT t.id, t.amount, t.description, t.date,
            t.walletId, w.name AS walletName, w.type AS walletType
        FROM transactions t
        INNER JOIN wallets w ON w.id = t.walletId
        WHERE t.type = 'INCOME'
        ORDER BY t.date DESC, t.id DESC
        """
    )
    fun getIncomesWithDetails(): Flow<List<IncomeWithDetails>>

    /** Все операции за период вместе с кошельком и категорией — для аналитики. */
    @Query(
        """
        SELECT t.id, t.type, t.amount, t.date,
            t.categoryId, c.name AS categoryName, COALESCE(c.iconKey, c.icon) AS categoryIcon,
            c.color AS categoryColor,
            t.walletId, w.name AS walletName
        FROM transactions t
        INNER JOIN wallets w ON w.id = t.walletId
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE t.date >= :from AND t.date < :to
        ORDER BY t.date DESC, t.id DESC
        """
    )
    fun getTransactionsWithDetails(from: Long, to: Long): Flow<List<TransactionWithDetails>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE walletId = :walletId ORDER BY date DESC")
    fun getTransactionsByWallet(walletId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT SUM(amount) FROM transactions WHERE walletId = :walletId AND type = 'INCOME'")
    suspend fun getTotalIncomeForWallet(walletId: Long): Long?

    @Query("SELECT SUM(amount) FROM transactions WHERE walletId = :walletId AND type = 'EXPENSE'")
    suspend fun getTotalExpenseForWallet(walletId: Long): Long?
}
