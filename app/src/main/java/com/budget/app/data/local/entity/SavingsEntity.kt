package com.budget.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Копилка. Баланс хранится не здесь, а считается запросом по [SavingsOperationEntity]:
 * стартовая сумма плюс пополнения минус выводы плюс проценты.
 */
@Entity(tableName = "savings")
data class SavingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long, // timestamp создания
    /** Цель в копейках; null — копилка без цели. */
    val targetAmount: Long? = null,
    /**
     * Стартовая сумма в копейках — деньги, которые уже лежат в копилке при её создании.
     * Кошельков не касается (как `initialBalance` у кошелька) и в доходах аналитики не участвует.
     */
    val initialAmount: Long = 0
)