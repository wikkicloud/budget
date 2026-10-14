package com.budget.app.ui.expenses

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budget.app.R
import com.budget.app.data.local.entity.CategoryEntity
import com.budget.app.data.local.entity.TransactionEntity
import com.budget.app.data.local.entity.WalletEntity
import com.budget.app.data.local.model.ExpenseWithDetails
import com.budget.app.data.repository.CategoryRepository
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

/** Расход в том виде, в каком его показывает экран. */
data class ExpenseUi(
    val id: Long,
    /** Сумма в копейках. */
    val amount: Long,
    val description: String,
    val date: Long,
    /** null — расход без категории. */
    val categoryId: Long?,
    /** Подпись категории (имя + иконка) или null, если категории нет. */
    val categoryLabel: String?,
    val walletId: Long,
    val walletName: String
)

/** Состояние формы добавления / редактирования расхода. */
data class ExpenseFormState(
    /** null — создание нового расхода. */
    val id: Long? = null,
    val amountInput: String = "",
    val description: String = "",
    /** null — без категории. */
    val categoryId: Long? = null,
    /** null — кошелёк не выбран. */
    val walletId: Long? = null,
    /** Дата операции, timestamp. Для нового расхода — сейчас. */
    val date: Long = System.currentTimeMillis(),
    @StringRes val amountErrorRes: Int? = null,
    @StringRes val walletErrorRes: Int? = null
) {
    val isEditing: Boolean get() = id != null
}

/** Единственное состояние экрана «Расходы». */
data class ExpensesUiState(
    val expenses: List<ExpenseUi> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val isLoading: Boolean = true,
    val form: ExpenseFormState = ExpenseFormState(),
    /** Расход, для которого открыт диалог подтверждения удаления. */
    val deleteTarget: ExpenseUi? = null,
    /** true — форма показывает редактирование существующего расхода. */
    val isEditing: Boolean = false,
    /** Разовое сообщение для Snackbar. */
    val messageRes: Int? = null
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    /** Состояние формы и диалогов — меняется только событиями с экрана. */
    private val localState = MutableStateFlow(ExpensesLocalState())

    val uiState: StateFlow<ExpensesUiState> = combine(
        transactionRepository.getExpensesWithDetails(),
        categoryRepository.getAllCategories(),
        walletRepository.getAllWallets(),
        localState
    ) { expenses, categories, wallets, local ->
        val mapped = expenses.map { it.toUi() }
        ExpensesUiState(
            expenses = mapped,
            categories = categories,
            wallets = wallets,
            isLoading = false,
            form = local.form,
            deleteTarget = local.deleteTargetId?.let { id -> mapped.firstOrNull { it.id == id } },
            isEditing = local.form.isEditing,
            messageRes = local.messageRes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpensesUiState()
    )

    // --- Форма ---

    fun onAmountChange(input: String) {
        localState.update { it.copy(form = it.form.copy(amountInput = input, amountErrorRes = null)) }
    }

    fun onDescriptionChange(input: String) {
        localState.update { it.copy(form = it.form.copy(description = input)) }
    }

    fun onCategorySelect(categoryId: Long?) {
        localState.update { it.copy(form = it.form.copy(categoryId = categoryId)) }
    }

    fun onWalletSelect(walletId: Long?) {
        localState.update { it.copy(form = it.form.copy(walletId = walletId, walletErrorRes = null)) }
    }

    fun onSave() {
        val form = localState.value.form

        val amount = MoneyFormatter.parseToKopecks(form.amountInput)

        val amountError = R.string.expense_error_amount.takeIf { amount == null || amount <= 0 }
        val walletError = R.string.expense_error_wallet.takeIf { form.walletId == null }

        if (amountError != null || walletError != null || amount == null) {
            localState.update {
                it.copy(form = form.copy(amountErrorRes = amountError, walletErrorRes = walletError))
            }
            return
        }

        viewModelScope.launch {
            val transaction = TransactionEntity(
                id = form.id ?: 0,
                type = TransactionType.EXPENSE.name,
                amount = amount,
                description = form.description.trim(),
                categoryId = form.categoryId,
                walletId = form.walletId!!,
                date = form.date
            )

            if (form.id == null) {
                transactionRepository.insertTransaction(transaction)
            } else {
                transactionRepository.updateTransaction(transaction)
            }

            localState.update { it.copy(form = ExpenseFormState()) }
        }
    }

    // --- Удаление ---

    fun onDeleteClick(expense: ExpenseUi) {
        localState.update { it.copy(deleteTargetId = expense.id) }
    }

    fun onDeleteDismiss() {
        localState.update { it.copy(deleteTargetId = null) }
    }

    fun onDeleteConfirm() {
        val id = localState.value.deleteTargetId ?: return

        viewModelScope.launch {
            transactionRepository.deleteTransactionById(id)
            localState.update {
                it.copy(deleteTargetId = null, messageRes = R.string.expense_deleted)
            }
        }
    }

    // --- Редактирование ---

    fun onEditClick(expense: ExpenseUi) {
        localState.update {
            it.copy(
                form = ExpenseFormState(
                    id = expense.id,
                    amountInput = MoneyFormatter.formatPlain(expense.amount),
                    description = expense.description,
                    categoryId = expense.categoryId,
                    walletId = expense.walletId,
                    date = expense.date
                )
            )
        }
    }

    fun onCancelEdit() {
        localState.update { it.copy(form = ExpenseFormState()) }
    }

    // --- Разовые сообщения ---

    fun onMessageShown() {
        localState.update { it.copy(messageRes = null) }
    }

    private fun ExpenseWithDetails.toUi() = ExpenseUi(
        id = id,
        amount = amount,
        description = description,
        date = date,
        categoryId = categoryId,
        categoryLabel = categoryName?.let { name -> "$categoryIcon $name" },
        walletId = walletId,
        walletName = walletName
    )
}

/** Часть состояния, которой управляет только сам экран. */
private data class ExpensesLocalState(
    val form: ExpenseFormState = ExpenseFormState(),
    val deleteTargetId: Long? = null,
    @StringRes val messageRes: Int? = null
)