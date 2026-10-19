package com.budget.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.budget.app.data.local.entity.SavingsEntity
import com.budget.app.data.local.entity.SavingsOperationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Миграция Room 3→4 (копилки): старые данные — кошельки, категории, операции и переводы —
 * должны остаться на месте, а новые таблицы появиться.
 *
 * Файл базы версии 3 создаётся вручную по её настоящей схеме: KSP не собирает
 * отдельную legacy-базу в тестовом сорсете.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationV3ToV4Test {

    private lateinit var context: Context
    private var database: BudgetDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getDatabasePath(DB_NAME).also { file ->
            File(file.parent.orEmpty()).mkdirs()
            file.delete()
        }
    }

    @After
    fun tearDown() {
        database?.close()
        context.getDatabasePath(DB_NAME).delete()
    }

    @Test
    fun `migration 3 to 4 keeps user data and adds savings tables`() = runBlocking {
        createVersion3Database()

        // Открываем ту же базу настоящей версией — сработает миграция 3→4
        val migrated = Room.databaseBuilder(context, BudgetDatabase::class.java, DB_NAME)
            .addMigrations(*DbMigrations.ALL)
            .build()
        database = migrated

        val wallets = migrated.walletDao().getAllWalletsWithBalance().first()
        assertEquals(2, wallets.size)
        // 100 000 − 5 000 расход − 30 000 перевод = 65 000
        assertEquals(65_000L, wallets.first { it.name == "Карта" }.balance)
        assertEquals(50_000L, wallets.first { it.name == "Наличные" }.balance)

        assertEquals(
            1,
            migrated.transactionDao().getTransactionsWithDetails(0L, Long.MAX_VALUE).first().size
        )
        assertEquals(1, migrated.transferDao().getTransfersWithDetails().first().size)
        assertEquals(1, migrated.categoryDao().getAllCategories().first().size)

        // Новые таблицы появились и пусты
        assertTrue(migrated.savingsDao().getSavingsWithBalance().first().isEmpty())

        // Копилки можно создавать и пополнять, а баланс кошелька это учитывает
        val savingsId = migrated.savingsDao().insertSavings(
            SavingsEntity(name = "Отпуск", createdAt = DATE, targetAmount = 50_000)
        )
        migrated.savingsDao().insertOperation(
            SavingsOperationEntity(
                savingsId = savingsId,
                walletId = 1L,
                amount = 15_000,
                type = "DEPOSIT",
                date = DATE
            )
        )

        val savings = migrated.savingsDao().getSavingsWithBalance().first().single()
        assertEquals(15_000L, savings.balance)
        assertEquals(50_000L, savings.targetAmount)

        val afterDeposit = migrated.walletDao().getAllWalletsWithBalance().first()
        assertEquals(50_000L, afterDeposit.first { it.name == "Карта" }.balance)
    }

    /** Создаёт файл базы ровно той схемы, что была до задачи №7 (версия 3), и наполняет его данными. */
    private fun createVersion3Database() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    VERSION_3_SCHEMA.forEach(db::execSQL)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO wallets (name, type, initialBalance) VALUES ('Карта', 'CARD', 100000)")
            db.execSQL("INSERT INTO wallets (name, type, initialBalance) VALUES ('Наличные', 'CASH', 20000)")
            db.execSQL("INSERT INTO categories (name, icon, color, iconKey) VALUES ('Продукты', '', '#4CAF50', NULL)")
            db.execSQL("INSERT INTO transactions (type, amount, description, categoryId, walletId, date) VALUES ('EXPENSE', 5000, 'Кофе', 1, 1, $DATE)")
            db.execSQL("INSERT INTO transfers (fromWalletId, toWalletId, amount, date, note) VALUES (1, 2, 30000, $DATE, 'На такси')")
        }
    }

    private companion object {
        const val DB_NAME = "migration_v3_v4_test.db"
        const val DATE = 1_700_000_000_000L

        /** Схема базы версии 3 — та же, что и в [DbMigrations] до миграции 3→4. */
        val VERSION_3_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `initialBalance` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` TEXT NOT NULL, `iconKey` TEXT)",
            "CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL, `description` TEXT NOT NULL, `categoryId` INTEGER, `walletId` INTEGER NOT NULL, `date` INTEGER NOT NULL, FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_walletId` ON `transactions` (`walletId`)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)",
            "CREATE TABLE IF NOT EXISTS `transfers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fromWalletId` INTEGER NOT NULL, `toWalletId` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `date` INTEGER NOT NULL, `note` TEXT, FOREIGN KEY(`fromWalletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`toWalletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            "CREATE INDEX IF NOT EXISTS `index_transfers_fromWalletId` ON `transfers` (`fromWalletId`)",
            "CREATE INDEX IF NOT EXISTS `index_transfers_toWalletId` ON `transfers` (`toWalletId`)"
        )
    }
}