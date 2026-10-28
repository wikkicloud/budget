package com.budget.app.data.repository

import com.budget.app.data.local.dao.WalletDao
import com.budget.app.data.local.entity.WalletEntity
import com.budget.app.data.local.model.WalletWithBalance
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val walletDao: WalletDao
) {
    fun getAllWallets(): Flow<List<WalletEntity>> = walletDao.getAllWallets()

    /** Активные (неархивные) кошельки вместе с посчитанным балансом. */
    fun getActiveWalletsWithBalance(): Flow<List<WalletWithBalance>> =
        walletDao.getActiveWalletsWithBalance()

    /** Архивные кошельки вместе с балансом. */
    fun getArchivedWalletsWithBalance(): Flow<List<WalletWithBalance>> =
        walletDao.getArchivedWalletsWithBalance()

    /** Архивирует кошелёк или возвращает его из архива — операция обратима. */
    suspend fun setWalletArchived(id: Long, archived: Boolean) =
        walletDao.setWalletArchived(id, archived)

    suspend fun getWalletById(id: Long): WalletEntity? = walletDao.getWalletById(id)

    suspend fun insertWallet(wallet: WalletEntity): Long = walletDao.insertWallet(wallet)

    suspend fun updateWallet(wallet: WalletEntity) = walletDao.updateWallet(wallet)

    suspend fun deleteWallet(wallet: WalletEntity) = walletDao.deleteWallet(wallet)

    /**
     * Удаляет кошелёк, только если по нему нет операций.
     * @return true — кошелёк удалён, false — у кошелька есть операции
     */
    suspend fun deleteWalletIfUnused(id: Long): Boolean = walletDao.deleteWalletIfUnused(id) > 0
}
