package com.budget.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.budget.app.data.local.dao.CategoryDao
import com.budget.app.data.local.dao.SavingsDao
import com.budget.app.data.local.dao.TransactionDao
import com.budget.app.data.local.dao.TransferDao
import com.budget.app.data.local.dao.WalletDao
import com.budget.app.data.local.entity.CategoryEntity
import com.budget.app.data.local.entity.SavingsEntity
import com.budget.app.data.local.entity.SavingsOperationEntity
import com.budget.app.data.local.entity.TransactionEntity
import com.budget.app.data.local.entity.TransferEntity
import com.budget.app.data.local.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransferEntity::class,
        SavingsEntity::class,
        SavingsOperationEntity::class
    ],
    // 5 — не смена схемы, а разовая починка данных: на устройствах уже лежит пустая база версии 4
    // (её пересоздала разрушающая миграция, а предзаполнение тогда не вызывалось), и без повышения
    // версии предустановленные категории и кошельки в ней не появятся. Тестовые данные стираются.
    version = 5,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun savingsDao(): SavingsDao
}