package com.budget.app.data.repository

import com.budget.app.data.local.dao.SavingsDao
import com.budget.app.data.local.entity.SavingsEntity
import com.budget.app.data.local.entity.SavingsOperationEntity
import com.budget.app.data.local.model.SavingsOperationWithDetails
import com.budget.app.data.local.model.SavingsPeriodSummary
import com.budget.app.data.local.model.SavingsWithBalance
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsRepository @Inject constructor(
    private val savingsDao: SavingsDao
) {
    /** Копилки вместе с посчитанным балансом. */
    fun getSavingsWithBalance(): Flow<List<SavingsWithBalance>> =
        savingsDao.getSavingsWithBalance()

    suspend fun getSavingsById(id: Long): SavingsEntity? = savingsDao.getSavingsById(id)

    suspend fun insertSavings(savings: SavingsEntity): Long = savingsDao.insertSavings(savings)

    /** Меняет название, цель или стартовую сумму существующей копилки. */
    suspend fun updateSavings(savings: SavingsEntity) = savingsDao.updateSavings(savings)

    /** Удаляет копилку вместе с её операциями (внешний ключ с CASCADE). */
    suspend fun deleteSavingsById(id: Long) = savingsDao.deleteSavingsById(id)

    /** Все операции по копилкам с названиями копилки и кошелька — для истории на экране. */
    fun getOperationsWithDetails(): Flow<List<SavingsOperationWithDetails>> =
        savingsDao.getOperationsWithDetails()

    suspend fun insertOperation(operation: SavingsOperationEntity): Long =
        savingsDao.insertOperation(operation)

    suspend fun getOperationById(id: Long): SavingsOperationEntity? =
        savingsDao.getOperationById(id)

    suspend fun deleteOperationById(id: Long) = savingsDao.deleteOperationById(id)

    /** Итоги по копилкам за период [from, to): пополнено, снято, начислено процентов. */
    fun getPeriodSummary(from: Long, to: Long): Flow<SavingsPeriodSummary> =
        savingsDao.getPeriodSummary(from, to)

    /** Все операции по копилкам за период [from, to) — для аналитики. */
    fun getOperationsBetween(from: Long, to: Long): Flow<List<SavingsOperationEntity>> =
        savingsDao.getOperationsBetween(from, to)

    /** Число операций, затрагивающих кошелёк. */
    suspend fun countOperationsByWallet(walletId: Long): Int =
        savingsDao.countOperationsByWallet(walletId)

    /** Число операций у копилки. */
    suspend fun countOperationsBySavings(savingsId: Long): Int =
        savingsDao.countOperationsBySavings(savingsId)
}