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
    // 7 — архив кошелька (`wallets.archived`). Миграция не пишется: до релиза схему
    // меняем напрямую (см. CLAUDE.md), тестовые данные при обновлении стираются.
    version = 7,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun savingsDao(): SavingsDao
}