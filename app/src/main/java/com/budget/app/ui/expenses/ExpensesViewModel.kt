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
    /** Название категории или null, если категории нет. */
    val categoryName: String?,
    /** Ключ или legacy-эмодзи иконки категории. */
    val categoryIconKey: String?,
    /** Hex-цвет категории для иконки. */
    val categoryColor: String?,
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
    /** Диалог добавления собственной категории. */
    val isCategoryDialogVisible: Boolean = false,
    val newCategory: NewCategoryState = NewCategoryState(),
    /** Разовое сообщение для Snackbar. */
    val messageRes: Int? = null
)

data class NewCategoryState(
    val name: String = "",
    val iconKey: String = CategoryIconOptions.defaultKey,
    @StringRes val errorRes: Int? = null
)

object CategoryIconOptions {
    const val defaultKey = "restaurant"

    /** Ключ иконки и цвет по умолчанию для неё. */
    val options: List<Pair<String, String>> = listOf(
        "restaurant" to "#FF9800",
        "local_cafe" to "#795548",
        "shopping_cart" to "#4CAF50",
        "directions_car" to "#2196F3",
        "home" to "#9C27B0",
        "favorite" to "#F44336",
        "health_and_safety" to "#F44336",
        "sports_esports" to "#E91E63",
        "movie" to "#673AB7",
        "flight" to "#03A9F4",
        "school" to "#3F51B5",
        "work" to "#607D8B",
        "pets" to "#FF5722",
        "local_gas_station" to "#607D8B",
        "build" to "#FF9800",
        "phone" to "#00BCD4",
        "wifi" to "#03A9F4",
        "fitness_center" to "#4CAF50",
        "local_hospital" to "#F44336",
        "local_grocery_store" to "#4CAF50",
        "child_friendly" to "#E91E63",
        "music_note" to "#9C27B0",
        "book" to "#3F51B5",
        "camera_alt" to "#607D8B",
        "cake" to "#EC407A",
        "celebration" to "#AB47BC",
        "payments" to "#66BB6A",
        "savings" to "#26A69A",
        "more_horiz" to "#9E9E9E"
    )

    val keys: List<String> get() = options.map { it.first }

    /** Цвет по умолчанию для ключа иконки; null — если ключ неизвестен. */
    fun colorFor(key: String?): String? =
        key?.let { k -> options.firstOrNull { it.first == k }?.second }
}

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
            isCategoryDialogVisible = local.isCategoryDialogVisible,
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

    fun onAddCategoryClick() {
        localState.update { it.copy(isCategoryDialogVisible = true, newCategory = NewCategoryState()) }
    }

    fun onCategoryDialogDismiss() {
        localState.update { it.copy(isCategoryDialogVisible = false) }
    }

    fun onNewCategoryNameChange(name: String) {
        localState.update { it.copy(newCategory = it.newCategory.copy(name = name, errorRes = null)) }
    }

    fun onNewCategoryIconSelect(iconKey: String) {
        localState.update { it.copy(newCategory = it.newCategory.copy(iconKey = iconKey)) }
    }

    fun onNewCategorySave() {
        val category = localState.value.newCategory
        val name = category.name.trim()
        if (name.isEmpty()) {
            localState.update { it.copy(newCategory = category.copy(errorRes = R.string.category_error_name)) }
            return
        }
        val color = CategoryIconOptions.colorFor(category.iconKey) ?: "#9C7CFF"
        viewModelScope.launch {
            val id = categoryRepository.addCategoryIfNameIsAvailable(
                name = name,
                iconKey = category.iconKey,
                color = color
            )
            if (id == null) {
                localState.update {
                    it.copy(newCategory = category.copy(errorRes = R.string.category_error_duplicate))
                }
            } else {
                localState.update {
                    it.copy(
                        isCategoryDialogVisible = false,
                        newCategory = NewCategoryState(),
                        form = it.form.copy(categoryId = id)
                    )
                }
            }
        }
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
        categoryName = categoryName,
        categoryIconKey = categoryIcon,
        categoryColor = categoryColor,
        walletId = walletId,
        walletName = walletName
    )
}

/** Часть состояния, которой управляет только сам экран. */
private data class ExpensesLocalState(
    val form: ExpenseFormState = ExpenseFormState(),
    val deleteTargetId: Long? = null,
    val isCategoryDialogVisible: Boolean = false,
    val newCategory: NewCategoryState = NewCategoryState(),
    @StringRes val messageRes: Int? = null
)