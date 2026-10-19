package com.budget.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.budget.app.data.local.dao.CategoryDao
import com.budget.app.data.local.dao.TransactionDao
import com.budget.app.data.local.dao.TransferDao
import com.budget.app.data.local.dao.WalletDao
import com.budget.app.data.local.entity.CategoryEntity
import com.budget.app.data.local.entity.TransactionEntity
import com.budget.app.data.local.entity.TransferEntity
import com.budget.app.data.local.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransferEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
}