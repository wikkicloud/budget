package com.budget.app.ui.income

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budget.app.R
import com.budget.app.data.local.entity.TransactionEntity
import com.budget.app.data.local.entity.WalletEntity
import com.budget.app.data.local.model.IncomeWithDetails
import com.budget.app.data.repository.TransactionRepository
import com.budget.app.data.repository.WalletRepository
import com.budget.app.domain.model.TransactionType
import com.budget.app.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Приход в том виде, в каком его показывает экран. */
data class IncomeUi(
    val id: Long,
    /** Сумма в копейках. */
    val amount: Long,
    val description: String,
    val date: Long,
    val walletId: Long,
    val walletName: String
)

/** Состояние формы добавления прихода. */
data class IncomeFormState(
    val amountInput: String = "",
    val description: String = "",
    /** null — кошелёк не выбран. */
    val walletId: Long? = null,
    /** Дата операции, timestamp. */
    val date: Long = System.currentTimeMillis(),
    @StringRes val amountErrorRes: Int? = null,
    @StringRes val walletErrorRes: Int? = null
)

/** Единственное состояние экрана «Приход». */
data class IncomeUiState(
    val incomes: List<IncomeUi> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val isLoading: Boolean = true,
    val form: IncomeFormState = IncomeFormState(),
    /** Приход, для которого открыт диалог подтверждения удаления. */
    val deleteTarget: IncomeUi? = null,
    /** Разовое сообщение для Snackbar. */
    val messageRes: Int? = null
)

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    /** Состояние формы и диалогов — меняется только событиями с экрана. */
    private val localState = MutableStateFlow(IncomeLocalState())

    val uiState: StateFlow<IncomeUiState> = combine(
        transactionRepository.getIncomesWithDetails(),
        walletRepository.getAllWallets(),
        localState
    ) { incomes, wallets, local ->
        val mapped = incomes.map { it.toUi() }
        IncomeUiState(
            incomes = mapped,
            wallets = wallets,
            isLoading = false,
            form = local.form,
            deleteTarget = local.deleteTargetId?.let { id -> mapped.firstOrNull { it.id == id } },
            messageRes = local.messageRes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = IncomeUiState()
    )

    // --- Форма ---

    fun onAmountChange(input: String) {
        localState.update { it.copy(form = it.form.copy(amountInput = input, amountErrorRes = null)) }
    }

    fun onDescriptionChange(input: String) {
        localState.update { it.copy(form = it.form.copy(description = input)) }
    }

    fun onWalletSelect(walletId: Long?) {
        localState.update { it.copy(form = it.form.copy(walletId = walletId, walletErrorRes = null)) }
    }

    fun onSave() {
        val form = localState.value.form

        val amount = MoneyFormatter.parseToKopecks(form.amountInput)

        val amountError = R.string.income_error_amount.takeIf { amount == null || amount <= 0 }
        val walletError = R.string.income_error_wallet.takeIf { form.walletId == null }

        if (amountError != null || walletError != null || amount == null) {
            localState.update {
                it.copy(form = form.copy(amountErrorRes = amountError, walletErrorRes = walletError))
            }
            return
        }

        viewModelScope.launch {
            transactionRepository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.INCOME.name,
                    amount = amount,
                    description = form.description.trim(),
                    categoryId = null,
                    walletId = form.walletId!!,
                    date = form.date
                )
            )

            localState.update {
                it.copy(form = IncomeFormState(), messageRes = R.string.income_saved)
            }
        }
    }

    // --- Удаление ---

    fun onDeleteClick(income: IncomeUi) {
        localState.update { it.copy(deleteTargetId = income.id) }
    }

    fun onDeleteDismiss() {
        localState.update { it.copy(deleteTargetId = null) }
    }

    fun onDeleteConfirm() {
        val id = localState.value.deleteTargetId ?: return

        viewModelScope.launch {
            transactionRepository.deleteTransactionById(id)
            localState.update {
                it.copy(deleteTargetId = null, messageRes = R.string.income_deleted)
            }
        }
    }

    // --- Разовые сообщения ---

    fun onMessageShown() {
        localState.update { it.copy(messageRes = null) }
    }

    private fun IncomeWithDetails.toUi() = IncomeUi(
        id = id,
        amount = amount,
        description = description,
        date = date,
        walletId = walletId,
        walletName = walletName
    )
}

/** Часть состояния, которой управляет только сам экран. */
private data class IncomeLocalState(
    val form: IncomeFormState = IncomeFormState(),
    val deleteTargetId: Long? = null,
    @StringRes val messageRes: Int? = null
)