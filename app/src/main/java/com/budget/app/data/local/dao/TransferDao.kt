package com.budget.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.budget.app.data.local.entity.TransferEntity
import com.budget.app.data.local.model.TransferWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    /** Все переводы по убыванию даты вместе с названиями кошельков. */
    @Query(
        """
        SELECT tr.id, tr.fromWalletId, fw.name AS fromWalletName,
            tr.toWalletId, tw.name AS toWalletName,
            tr.amount, tr.date, tr.note
        FROM transfers tr
        INNER JOIN wallets fw ON fw.id = tr.fromWalletId
        INNER JOIN wallets tw ON tw.id = tr.toWalletId
        ORDER BY tr.date DESC, tr.id DESC
        """
    )
    fun getTransfersWithDetails(): Flow<List<TransferWithDetails>>

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getTransferById(id: Long): TransferEntity?

    @Insert
    suspend fun insertTransfer(transfer: TransferEntity): Long

    @Delete
    suspend fun deleteTransfer(transfer: TransferEntity)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteTransferById(id: Long)

    /** Сколько переводов затронут кошелёк — для запрета удаления используемого кошелька. */
    @Query("SELECT COUNT(*) FROM transfers WHERE fromWalletId = :walletId OR toWalletId = :walletId")
    suspend fun countTransfersByWallet(walletId: Long): Int

    @Query("SELECT SUM(amount) FROM transfers WHERE fromWalletId = :walletId")
    suspend fun getTotalOutgoingForWallet(walletId: Long): Long?

    @Query("SELECT SUM(amount) FROM transfers WHERE toWalletId = :walletId")
    suspend fun getTotalIncomingForWallet(walletId: Long): Long?
}