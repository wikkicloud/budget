package com.budget.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Перевод между кошельками. Не является ни доходом, ни расходом:
 * в таблице `transactions` не хранится и в аналитике не участвует.
 */
@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromWalletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["toWalletId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("fromWalletId"),
        Index("toWalletId")
    ]
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromWalletId: Long,
    val toWalletId: Long,
    val amount: Long, // в копейках
    val date: Long, // timestamp
    val note: String? = null
)