package com.budget.app.data.di

import android.content.Context
import androidx.room.Room
import com.budget.app.data.local.BudgetDatabase
import com.budget.app.data.local.DefaultDataSeeder
import com.budget.app.data.local.dao.CategoryDao
import com.budget.app.data.local.dao.SavingsDao
import com.budget.app.data.local.dao.TransactionDao
import com.budget.app.data.local.dao.TransferDao
import com.budget.app.data.local.dao.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBudgetDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>,
        walletDaoProvider: Provider<WalletDao>
    ): BudgetDatabase {
        return Room.databaseBuilder(
            context,
            BudgetDatabase::class.java,
            "budget_database"
        )
            // Схема зафиксирована версией 1 (первый релиз). Деструктивного fallback больше
            // нет: любое изменение сущностей требует миграции, иначе открытие базы упадёт
            // с исключением — данные пользователей не стираются молча.
            .addCallback(DefaultDataSeeder.callback(context, categoryDaoProvider, walletDaoProvider))
            .build()
    }

    @Provides
    @Singleton
    fun provideWalletDao(database: BudgetDatabase): WalletDao {
        return database.walletDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: BudgetDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: BudgetDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideTransferDao(database: BudgetDatabase): TransferDao {
        return database.transferDao()
    }

    @Provides
    @Singleton
    fun provideSavingsDao(database: BudgetDatabase): SavingsDao {
        return database.savingsDao()
    }
}
