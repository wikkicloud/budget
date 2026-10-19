package com.budget.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Операция по копилке: пополнение, вывод или начисление процентов.
 *
 * Пополнения и выводы НЕ являются расходом или доходом — в таблице `transactions`
 * их нет (как и переводов между кошельками). Проценты в аналитике считаются доходом.
 */
@Entity(
    tableName = "savings_operations",
    foreignKeys = [
        ForeignKey(
            entity = SavingsEntity::class,
            parentColumns = ["id"],
            childColumns = ["savingsId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("savingsId"),
        Index("walletId")
    ]
)
data class SavingsOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val savingsId: Long,
    /** Кошелёк, с которого пришли деньги или на который вернулись; null — проценты. */
    val walletId: Long? = null,
    val amount: Long, // в копейках
    /** "DEPOSIT", "WITHDRAW" или "INTEREST". */
    val type: String,
    val date: Long // timestamp
)