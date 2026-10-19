package com.budget.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Миграции схемы. Данные пользователя терять нельзя — только ALTER/CREATE. */
object DbMigrations {

    /** 1 → 2: у категорий появился строковый ключ иконки для собственных категорий. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN iconKey TEXT")
        }
    }

    /** 2 → 3: таблица переводов между кошельками. Существующие данные не меняются. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transfers` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `fromWalletId` INTEGER NOT NULL,
                    `toWalletId` INTEGER NOT NULL,
                    `amount` INTEGER NOT NULL,
                    `date` INTEGER NOT NULL,
                    `note` TEXT,
                    FOREIGN KEY(`fromWalletId`) REFERENCES `wallets`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`toWalletId`) REFERENCES `wallets`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transfers_fromWalletId` " +
                    "ON `transfers` (`fromWalletId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transfers_toWalletId` " +
                    "ON `transfers` (`toWalletId`)"
            )
        }
    }

    /** Все миграции по порядку — их подключает [com.budget.app.data.di.DatabaseModule]. */
    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}