package com.budget.app.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budget.app.data.local.BudgetDatabase
import com.budget.app.data.local.dao.CategoryDao
import com.budget.app.data.local.dao.TransactionDao
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
                                CategoryEntity(name = "Продукты", icon = "🛒", color = "#4CAF50"),
                                CategoryEntity(name = "Транспорт", icon = "🚗", color = "#2196F3"),
                                CategoryEntity(name = "Жильё", icon = "🏠", color = "#9C27B0"),
                                CategoryEntity(name = "Кафе", icon = "☕", color = "#FF9800"),
                                CategoryEntity(name = "Здоровье", icon = "💊", color = "#F44336"),
                                CategoryEntity(name = "Развлечения", icon = "🎮", color = "#E91E63"),
                                CategoryEntity(name = "Покупки", icon = "🛍️", color = "#00BCD4"),
                                CategoryEntity(name = "Прочее", icon = "📌", color = "#9E9E9E")
                            )
                        )

                        // Предзаполнение кошельков
                        walletDao.insertWallet(
                            WalletEntity(name = "Наличные", type = "CASH", initialBalance = 0)
                        )
                        walletDao.insertWallet(
                            WalletEntity(name = "Карта", type = "CARD", initialBalance = 0)
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
}
