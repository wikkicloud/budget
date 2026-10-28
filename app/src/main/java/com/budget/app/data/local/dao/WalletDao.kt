package com.budget.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.budget.app.data.local.entity.WalletEntity
import com.budget.app.data.local.model.WalletWithBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    /**
     * Кошельки вместе с балансом: начальная сумма + доходы − расходы,
     * − исходящие и + входящие переводы, − пополнения копилок и + возвраты из них.
     * Проценты по копилкам кошельков не касаются (у них `walletId` = NULL).
     */
    @Query(
        """
        SELECT w.id, w.name, w.type, w.initialBalance, w.archived,
            w.initialBalance
                + COALESCE((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.walletId = w.id AND t.type = 'INCOME'), 0)
                - COALESCE((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.walletId = w.id AND t.type = 'EXPENSE'), 0)
                - COALESCE((SELECT SUM(tr.amount) FROM transfers tr
                    WHERE tr.fromWalletId = w.id), 0)
                + COALESCE((SELECT SUM(tr.amount) FROM transfers tr
                    WHERE tr.toWalletId = w.id), 0)
                - COALESCE((SELECT SUM(so.amount) FROM savings_operations so
                    WHERE so.walletId = w.id AND so.type = 'DEPOSIT'), 0)
                + COALESCE((SELECT SUM(so.amount) FROM savings_operations so
                    WHERE so.walletId = w.id AND so.type = 'WITHDRAW'), 0)
                AS balance
        FROM wallets w
        WHERE w.archived = 0
        ORDER BY w.id ASC
        """
    )
    fun getActiveWalletsWithBalance(): Flow<List<WalletWithBalance>>

    /** Архивные кошельки с балансом — для раздела «Архив» на экране «Кошельки». */
    @Query(
        """
        SELECT w.id, w.name, w.type, w.initialBalance, w.archived,
            w.initialBalance
                + COALESCE((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.walletId = w.id AND t.type = 'INCOME'), 0)
                - COALESCE((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.walletId = w.id AND t.type = 'EXPENSE'), 0)
                - COALESCE((SELECT SUM(tr.amount) FROM transfers tr
                    WHERE tr.fromWalletId = w.id), 0)
                + COALESCE((SELECT SUM(tr.amount) FROM transfers tr
                    WHERE tr.toWalletId = w.id), 0)
                - COALESCE((SELECT SUM(so.amount) FROM savings_operations so
                    WHERE so.walletId = w.id AND so.type = 'DEPOSIT'), 0)
                + COALESCE((SELECT SUM(so.amount) FROM savings_operations so
                    WHERE so.walletId = w.id AND so.type = 'WITHDRAW'), 0)
                AS balance
        FROM wallets w
        WHERE w.archived = 1
        ORDER BY w.id ASC
        """
    )
    fun getArchivedWalletsWithBalance(): Flow<List<WalletWithBalance>>

    @Query("SELECT * FROM wallets WHERE archived = 0 ORDER BY id ASC")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): WalletEntity?

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun countWallets(): Int

    @Insert
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    /** Архивация обратима: тот же запрос возвращает кошелёк из архива. */
    @Query("UPDATE wallets SET archived = :archived WHERE id = :id")
    suspend fun setWalletArchived(id: Long, archived: Boolean)

    @Delete
    suspend fun deleteWallet(wallet: WalletEntity)

    /**
     * Удаляет кошелёк, только если он не используется ни в операциях, ни в переводах,
     * ни в операциях по копилкам (у переводов внешний ключ с CASCADE, а у операций
     * по копилкам — SET_NULL: без проверки история молча потерялась бы или обнулилась).
     */
    @Query(
        """
        DELETE FROM wallets WHERE id = :id
            AND NOT EXISTS (SELECT 1 FROM transactions WHERE walletId = :id)
            AND NOT EXISTS (
                SELECT 1 FROM transfers WHERE fromWalletId = :id OR toWalletId = :id
            )
            AND NOT EXISTS (
                SELECT 1 FROM savings_operations WHERE walletId = :id
            )
        """
    )
    suspend fun deleteWalletIfUnused(id: Long): Int
}