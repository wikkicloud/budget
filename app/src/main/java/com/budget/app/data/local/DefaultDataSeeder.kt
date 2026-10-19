package com.budget.app.data.local

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budget.app.R
import com.budget.app.data.local.dao.CategoryDao
import com.budget.app.data.local.dao.WalletDao
import com.budget.app.data.local.entity.CategoryEntity
import com.budget.app.data.local.entity.WalletEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider

/**
 * Предзаполнение базы: 8 встроенных категорий расходов и два кошелька («Наличные», «Карта»).
 *
 * Заполнение висит и на `onCreate` (первый запуск), и на `onDestructiveMigration`. Второе
 * обязательно, пока в `DatabaseModule` стоит `fallbackToDestructiveMigration`: при смене версии
 * базы Room пересоздаёт таблицы в `onMigrate` (drop + create) и `onCreate` при этом не вызывает —
 * без обработчика `onDestructiveMigration` база оставалась пустой (баг «не заполняются
 * предустановленные категории и кошельки»).
 */
object DefaultDataSeeder {

    /** Колбэк Room, наполняющий базу значениями по умолчанию. */
    fun callback(
        context: Context,
        categoryDaoProvider: Provider<CategoryDao>,
        walletDaoProvider: Provider<WalletDao>
    ): RoomDatabase.Callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            seed()
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            seed()
        }

        /**
         * Заполнение идёт в отдельной корутине: колбэк выполняется внутри открытия базы, на том же
         * соединении и в открытой транзакции, поэтому обращаться к DAO прямо здесь нельзя.
         * (`onDestructiveMigration` вообще вызывается до `createAllTables`, так что запрос обязан
         * дождаться конца открытия — это и делает ленивое соединение Room.)
         */
        private fun seed() {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                seedIfEmpty(context, categoryDaoProvider.get(), walletDaoProvider.get())
            }
        }
    }

    /**
     * Заполняет только пустые таблицы, поэтому повторный вызов (в том числе при каждом открытии
     * уже готовой базы) ничего не портит: удалённые пользователем кошельки не вернутся.
     */
    suspend fun seedIfEmpty(context: Context, categoryDao: CategoryDao, walletDao: WalletDao) {
        if (categoryDao.countCategories() == 0) {
            categoryDao.insertCategories(defaultCategories(context))
        }

        if (walletDao.countWallets() == 0) {
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

    /**
     * Встроенные категории. `iconKey` намеренно null: по нему UI отличает собственные категории
     * (`CategoryEntity::isCustom`), и у встроенных нет меню «Редактировать/Удалить».
     * Эмодзи в `icon` остаются legacy-ключом для сопоставления иконок в [com.budget.app.util.CategoryIcons].
     */
    fun defaultCategories(context: Context): List<CategoryEntity> = listOf(
        CategoryEntity(name = context.getString(R.string.category_food), icon = "🛒", color = "#4CAF50"),
        CategoryEntity(name = context.getString(R.string.category_transport), icon = "🚗", color = "#2196F3"),
        CategoryEntity(name = context.getString(R.string.category_housing), icon = "🏠", color = "#9C27B0"),
        CategoryEntity(name = context.getString(R.string.category_cafe), icon = "☕", color = "#FF9800"),
        CategoryEntity(name = context.getString(R.string.category_health), icon = "💊", color = "#F44336"),
        CategoryEntity(name = context.getString(R.string.category_entertainment), icon = "🎮", color = "#E91E63"),
        CategoryEntity(name = context.getString(R.string.category_shopping), icon = "🛍️", color = "#00BCD4"),
        CategoryEntity(name = context.getString(R.string.category_other), icon = "📌", color = "#9E9E9E")
    )
}