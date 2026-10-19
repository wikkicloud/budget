package com.budget.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.budget.app.data.local.entity.SavingsEntity
import com.budget.app.data.local.entity.SavingsOperationEntity
import com.budget.app.data.local.model.SavingsOperationWithDetails
import com.budget.app.data.local.model.SavingsPeriodSummary
import com.budget.app.data.local.model.SavingsWithBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {
    /**
     * Копилки вместе с балансом: пополнения − выводы + проценты.
     * Баланс не хранится в таблице, а считается здесь же.
     */
    @Query(
        """
        SELECT s.id, s.name, s.createdAt, s.targetAmount,
            COALESCE((SELECT SUM(o.amount) FROM savings_operations o
                WHERE o.savingsId = s.id AND o.type = 'DEPOSIT'), 0)
            - COALESCE((SELECT SUM(o.amount) FROM savings_operations o
                WHERE o.savingsId = s.id AND o.type = 'WITHDRAW'), 0)
            + COALESCE((SELECT SUM(o.amount) FROM savings_operations o
                WHERE o.savingsId = s.id AND o.type = 'INTEREST'), 0)
            AS balance
        FROM savings s
        ORDER BY s.id ASC
        """
    )
    fun getSavingsWithBalance(): Flow<List<SavingsWithBalance>>

    @Query("SELECT * FROM savings WHERE id = :id")
    suspend fun getSavingsById(id: Long): SavingsEntity?

    @Insert
    suspend fun insertSavings(savings: SavingsEntity): Long

    @Query("DELETE FROM savings WHERE id = :id")
    suspend fun deleteSavingsById(id: Long)

    // --- Операции по копилкам ---

    /** Все операции по копилкам вместе с названием кошелька — для списка на экране. */
    @Query(
        """
        SELECT o.id, o.savingsId, s.name AS savingsName,
            o.walletId, w.name AS walletName,
            o.amount, o.type, o.date
        FROM savings_operations o
        INNER JOIN savings s ON s.id = o.savingsId
        LEFT JOIN wallets w ON w.id = o.walletId
        ORDER BY o.date DESC, o.id DESC
        """
    )
    fun getOperationsWithDetails(): Flow<List<SavingsOperationWithDetails>>

    /** Все операции по копилкам за период [from, to) — для блока «Копилки» в аналитике. */
    @Query(
        """
        SELECT * FROM savings_operations
        WHERE date >= :from AND date < :to
        ORDER BY date ASC
        """
    )
    fun getOperationsBetween(from: Long, to: Long): Flow<List<SavingsOperationEntity>>

    @Insert
    suspend fun insertOperation(operation: SavingsOperationEntity): Long

    @Query("SELECT * FROM savings_operations WHERE id = :id")
    suspend fun getOperationById(id: Long): SavingsOperationEntity?

    @Query("DELETE FROM savings_operations WHERE id = :id")
    suspend fun deleteOperationById(id: Long)

    /** Итоги по копилкам за период [from, to): пополнено, снято и начислено процентов. */
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'DEPOSIT' THEN amount ELSE 0 END), 0) AS deposited,
            COALESCE(SUM(CASE WHEN type = 'WITHDRAW' THEN amount ELSE 0 END), 0) AS withdrawn,
            COALESCE(SUM(CASE WHEN type = 'INTEREST' THEN amount ELSE 0 END), 0) AS interest
        FROM savings_operations
        WHERE date >= :from AND date < :to
        """
    )
    fun getPeriodSummary(from: Long, to: Long): Flow<SavingsPeriodSummary>

    /** Сколько операций затрагивают кошелёк — для запрета удаления используемого кошелька. */
    @Query("SELECT COUNT(*) FROM savings_operations WHERE walletId = :walletId")
    suspend fun countOperationsByWallet(walletId: Long): Int

    /** Сколько операций у копилки — для диалога подтверждения удаления. */
    @Query("SELECT COUNT(*) FROM savings_operations WHERE savingsId = :savingsId")
    suspend fun countOperationsBySavings(savingsId: Long): Int

    @Delete
    suspend fun deleteOperation(operation: SavingsOperationEntity)
}