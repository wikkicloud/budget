package com.budget.app.data.repository

import com.budget.app.data.local.dao.TransferDao
import com.budget.app.data.local.entity.TransferEntity
import com.budget.app.data.local.model.TransferWithDetails
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepository @Inject constructor(
    private val transferDao: TransferDao
) {
    /** Переводы вместе с названиями кошельков — для списка на экране «Кошельки». */
    fun getTransfersWithDetails(): Flow<List<TransferWithDetails>> =
        transferDao.getTransfersWithDetails()

    suspend fun getTransferById(id: Long): TransferEntity? = transferDao.getTransferById(id)

    suspend fun insertTransfer(transfer: TransferEntity): Long =
        transferDao.insertTransfer(transfer)

    suspend fun deleteTransfer(transfer: TransferEntity) = transferDao.deleteTransfer(transfer)

    suspend fun deleteTransferById(id: Long) = transferDao.deleteTransferById(id)

    /** Число переводов, затрагивающих кошелёк. */
    suspend fun countTransfersByWallet(walletId: Long): Int =
        transferDao.countTransfersByWallet(walletId)

    suspend fun getTotalOutgoingForWallet(walletId: Long): Long =
        transferDao.getTotalOutgoingForWallet(walletId) ?: 0L

    suspend fun getTotalIncomingForWallet(walletId: Long): Long =
        transferDao.getTotalIncomingForWallet(walletId) ?: 0L
}