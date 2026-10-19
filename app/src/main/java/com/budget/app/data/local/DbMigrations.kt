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

    /** 3 → 4: таблицы копилок и операций по ним. Существующие данные не меняются. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `savings` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `targetAmount` INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `savings_operations` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `savingsId` INTEGER NOT NULL,
                    `walletId` INTEGER,
                    `amount` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `date` INTEGER NOT NULL,
                    FOREIGN KEY(`savingsId`) REFERENCES `savings`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_savings_operations_savingsId` " +
                    "ON `savings_operations` (`savingsId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_savings_operations_walletId` " +
                    "ON `savings_operations` (`walletId`)"
            )
        }
    }

    /** Все миграции по порядку — их подключает [com.budget.app.data.di.DatabaseModule]. */
    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}