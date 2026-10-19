package com.budget.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.budget.app.R
import com.budget.app.data.local.entity.CategoryEntity
import com.budget.app.data.local.entity.WalletEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.inject.Provider

/**
 * Предзаполнение базы: 8 встроенных категорий и два кошелька.
 *
 * Здесь же воспроизводится баг «не заполняются предустановленные категории и кошельки»: при
 * `fallbackToDestructiveMigration` Room пересоздаёт таблицы и вызывает `onDestructiveMigration`,
 * но **не** `onCreate` — если заполнение висит только на `onCreate`, база после смены версии
 * остаётся пустой.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultDataSeederTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "seeder_test_database"

    private lateinit var database: BudgetDatabase

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `empty database gets all default categories and wallets`() = runBlocking {
        database = inMemoryDatabase()

        DefaultDataSeeder.seedIfEmpty(context, database.categoryDao(), database.walletDao())

        assertEquals(8, database.categoryDao().countCategories())
        assertEquals(2, database.walletDao().countWallets())
        val walletNames = database.walletDao().getAllWallets().first().map { it.name }
        assertEquals(
            listOf(context.getString(R.string.wallet_cash), context.getString(R.string.wallet_card)),
            walletNames
        )
        assertEquals(setOf("CASH", "CARD"), database.walletDao().getAllWallets().first().map { it.type }.toSet())
    }

    @Test
    fun `default categories have no icon key so they stay built-in`() {
        val categories = DefaultDataSeeder.defaultCategories(context)

        assertEquals(8, categories.size)
        assertTrue(categories.all { it.iconKey == null })
        assertTrue(categories.all { it.name.isNotBlank() })
        assertTrue(categories.all { it.icon.isNotBlank() })
        assertTrue(categories.all { it.color.startsWith("#") })
        assertEquals(8, categories.map { it.name }.toSet().size)
        assertNull(categories.first().iconKey)
    }

    @Test
    fun `repeated seeding does not duplicate rows`() = runBlocking {
        database = inMemoryDatabase()

        DefaultDataSeeder.seedIfEmpty(context, database.categoryDao(), database.walletDao())
        DefaultDataSeeder.seedIfEmpty(context, database.categoryDao(), database.walletDao())

        assertEquals(8, database.categoryDao().countCategories())
        assertEquals(2, database.walletDao().countWallets())
    }

    /** Заполнение идемпотентно по каждой таблице: уже непустые таблицы не трогаются. */
    @Test
    fun `non-empty tables are left as they are`() = runBlocking {
        database = inMemoryDatabase()
        database.walletDao().insertWallet(WalletEntity(name = "Своя карта", type = "CARD", initialBalance = 0))
        database.categoryDao().insertCategories(
            listOf(
                CategoryEntity(
                    name = "Своя категория",
                    icon = "restaurant",
                    color = "#FF9800",
                    iconKey = "restaurant"
                )
            )
        )

        DefaultDataSeeder.seedIfEmpty(context, database.categoryDao(), database.walletDao())

        assertEquals(1, database.walletDao().countWallets())
        assertEquals(1, database.categoryDao().countCategories())
        assertEquals("Своя карта", database.walletDao().getAllWallets().first().single().name)
        assertEquals("Своя категория", database.categoryDao().getAllCategories().first().single().name)
    }

    @Test
    fun `callback fills the database on first creation`() = runBlocking {
        database = productiveDatabase()

        awaitDefaults(database)
        assertEquals(0L, database.walletDao().getAllWallets().first().first().initialBalance)
    }

    /**
     * Баг: файл базы со старой версией схемы открывается новой версией приложения. Миграций нет
     * (до релиза их не пишем), значит Room пересоздаёт таблицы разрушающе — категории и кошельки
     * должны появиться снова.
     */
    @Test
    fun `defaults are restored after destructive migration`() = runBlocking {
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        writeLegacyDatabaseFile(file, version = 3)

        database = productiveDatabase()

        awaitDefaults(database)
        assertTrue("старая таблица должна быть удалена", !tableExists(file, "legacy_table"))
    }

    private fun inMemoryDatabase(): BudgetDatabase =
        Room.inMemoryDatabaseBuilder(context, BudgetDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    /** База с той же конфигурацией, что и в `DatabaseModule`. */
    private fun productiveDatabase(): BudgetDatabase =
        Room.databaseBuilder(context, BudgetDatabase::class.java, dbName)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(
                DefaultDataSeeder.callback(
                    context,
                    Provider { database.categoryDao() },
                    Provider { database.walletDao() }
                )
            )
            .build()

    /** Заполнение асинхронное (отдельная корутина в колбэке), поэтому его нужно дождаться. */
    private suspend fun awaitDefaults(db: BudgetDatabase) = withTimeout(5_000) {
        while (db.categoryDao().countCategories() < 8 || db.walletDao().countWallets() < 2) {
            delay(10)
        }
    }

    private fun writeLegacyDatabaseFile(file: File, version: Int) {
        val legacy = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            legacy.execSQL("CREATE TABLE IF NOT EXISTS legacy_table (id INTEGER PRIMARY KEY)")
            legacy.execSQL("PRAGMA user_version = $version")
        } finally {
            legacy.close()
        }
    }

    private fun tableExists(file: File, table: String): Boolean {
        val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        return try {
            db.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table))
                .use { it.count > 0 }
        } finally {
            db.close()
        }
    }
}