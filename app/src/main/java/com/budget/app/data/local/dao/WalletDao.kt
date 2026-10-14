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
    @Query(
        """
        SELECT w.id, w.name, w.type, w.initialBalance,
            w.initialBalance
                + COALESCE((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.walletId = w.id AND t.type = 'INCOME'), 0)
                - COALESCE((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.walletId = w.id AND t.type = 'EXPENSE'), 0)
                AS balance
        FROM wallets w
        ORDER BY w.id ASC
        """
    )
    fun getAllWalletsWithBalance(): Flow<List<WalletWithBalance>>

    @Query("SELECT * FROM wallets ORDER BY id ASC")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): WalletEntity?

    @Insert
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Delete
    suspend fun deleteWallet(wallet: WalletEntity)

    @Query("DELETE FROM wallets WHERE id = :id AND NOT EXISTS (SELECT 1 FROM transactions WHERE walletId = :id)")
    suspend fun deleteWalletIfUnused(id: Long): Int
}
