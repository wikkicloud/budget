package com.budget.app.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budget.app.R
import com.budget.app.data.local.BudgetDatabase
import com.budget.app.data.local.dao.CategoryDao
import com.budget.app.data.local.dao.SavingsDao
import com.budget.app.data.local.dao.TransactionDao
import com.budget.app.data.local.dao.TransferDao
import com.budget.app.data.local.dao.WalletDao
import com.budget.app.data.local.entity.CategoryEntity
import com.budget.app.data.local.entity.WalletEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
            // До первого релиза миграции не пишем: схема меняется напрямую, старая база
            // пересоздаётся, тестовые данные стираются. Перед релизом — заменить на миграции.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Предзаполнение данных
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val categoryDao = categoryDaoProvider.get()
                        val walletDao = walletDaoProvider.get()

                        // Предзаполнение категорий
                        categoryDao.insertCategories(
                            listOf(
                                CategoryEntity(
                                    name = context.getString(R.string.category_food),
                                    icon = "🛒",
                                    color = "#4CAF50"
                                ),
                                CategoryEntity(
                                    name = context.getString(R.string.category_transport),
                                    icon = "🚗",
                                    color = "#2196F3"
                                ),
                                CategoryEntity(
                                    name = context.getString(R.string.category_housing),
                                    icon = "🏠",
                                    color = "#9C27B0"
                                ),
                                CategoryEntity(
                                    name = context.getString(R.string.category_cafe),
                                    icon = "☕",
                                    color = "#FF9800"
                                ),
                                CategoryEntity(
                                    name = context.getString(R.string.category_health),
                                    icon = "💊",
                                    color = "#F44336"
                                ),
                                CategoryEntity(
                                    name = context.getString(R.string.category_entertainment),
                                    icon = "🎮",
                                    color = "#E91E63"
                                ),
                                CategoryEntity(
                                    name = context.getString(R.string.category_shopping),
                                    icon = "🛍️",
                                    color = "#00BCD4"
                                ),
                                CategoryEntity(
                                    name = context.getString(R.string.category_other),
                                    icon = "📌",
                                    color = "#9E9E9E"
                                )
                            )
                        )

                        // Предзаполнение кошельков
                        walletDao.insertWallet(
                            WalletEntity(
                                name = context.getString(R.string.wallet_cash),
                                type = "CASH",
                                initialBalance = 0
                            )
                        )
                        walletDao.insertWallet(
                            WalletEntity(
                                name = context.getString(R.string.wallet_card),
                                type = "CARD",
                                initialBalance = 0
                            )
                        )
                    }
                }
            })
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
