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
import com.budget.app.util.CategoryIconOptions
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
    /** Открытый диалог добавления или редактирования расхода; null — диалог закрыт. */
    val form: ExpenseFormState? = null,
    /** Расход, для которого открыт диалог подтверждения удаления. */
    val deleteTarget: ExpenseUi? = null,
    /** Диалог добавления / редактирования собственной категории. */
    val isCategoryDialogVisible: Boolean = false,
    val newCategory: NewCategoryState = NewCategoryState(),
    /** Категория, для которой открыт диалог подтверждения удаления. */
    val deleteCategoryTarget: CategoryEntity? = null,
    /** Число операций deleteCategoryTarget — подгружается перед показом диалога. */
    val deleteCategoryUsageCount: Int = 0,
    /** Подсказки описания: по вводу или, если поле пустое, самые частые. */
    val suggestions: List<ExpenseSuggestion> = emptyList(),
    /** Разовое сообщение для Snackbar. */
    val messageRes: Int? = null
)

data class NewCategoryState(
    /** null — создание новой категории, иначе id редактируемой. */
    val id: Long? = null,
    val name: String = "",
    val iconKey: String = CategoryIconOptions.defaultKey,
    @StringRes val errorRes: Int? = null
) {
    val isEditing: Boolean get() = id != null
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
            isCategoryDialogVisible = local.isCategoryDialogVisible,
            newCategory = local.newCategory,
            deleteCategoryTarget = local.deleteCategoryTargetId?.let { id ->
                categories.firstOrNull { it.id == id }
            },
            deleteCategoryUsageCount = local.deleteCategoryUsageCount,
            // Подсказки нужны только в открытом диалоге: по текущему вводу или, если поле пустое, самые частые.
            suggestions = local.form?.let { ExpenseSuggestions.build(mapped, it.description) }.orEmpty(),
            messageRes = local.messageRes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpensesUiState()
    )

    // --- Форма ---

    /** Кнопка «+»: диалог добавления с пустыми полями. */
    fun onAddClick() {
        localState.update { it.copy(form = ExpenseFormState()) }
    }

    /** Закрыть диалог без сохранения. */
    fun onFormDismiss() {
        localState.update { it.copy(form = null) }
    }

    fun onAmountChange(input: String) {
        localState.update { state ->
            state.copy(form = state.form?.copy(amountInput = input, amountErrorRes = null))
        }
    }

    fun onDescriptionChange(input: String) {
        localState.update { state ->
            state.copy(form = state.form?.copy(description = input))
        }
    }

    /**
     * Выбрана подсказка описания: подставляем описание, а также категорию и кошелёк последнего
     * расхода с таким названием. Сумма подставляется только в пустое поле. Всё можно изменить.
     */
    fun onSuggestionSelect(suggestion: ExpenseSuggestion) {
        localState.update { state ->
            val form = state.form ?: return@update state
            state.copy(
                form = form.copy(
                    description = suggestion.description,
                    categoryId = suggestion.categoryId,
                    walletId = suggestion.walletId,
                    amountInput = if (form.amountInput.isBlank()) {
                        MoneyFormatter.formatPlain(suggestion.amount)
                    } else {
                        form.amountInput
                    },
                    walletErrorRes = null
                )
            )
        }
    }

    fun onCategorySelect(categoryId: Long?) {
        localState.update { state ->
            state.copy(form = state.form?.copy(categoryId = categoryId))
        }
    }

    fun onAddCategoryClick() {
        localState.update { it.copy(isCategoryDialogVisible = true, newCategory = NewCategoryState()) }
    }

    /** Открыть диалог категории в режиме редактирования. */
    fun onEditCategoryClick(category: CategoryEntity) {
        localState.update {
            it.copy(
                isCategoryDialogVisible = true,
                newCategory = NewCategoryState(
                    id = category.id,
                    name = category.name,
                    iconKey = category.iconKey ?: CategoryIconOptions.defaultKey
                )
            )
        }
    }

    fun onCategoryDialogDismiss() {
        localState.update { it.copy(isCategoryDialogVisible = false, newCategory = NewCategoryState()) }
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
            val editingId = category.id
            val resultId: Long? = if (editingId == null) {
                categoryRepository.addCategoryIfNameIsAvailable(
                    name = name,
                    iconKey = category.iconKey,
                    color = color
                )
            } else {
                val existing = categoryRepository.getCategoryById(editingId) ?: return@launch
                val updated = categoryRepository.updateCategoryIfNameIsAvailable(
                    existing.copy(name = name, iconKey = category.iconKey, color = color)
                )
                if (updated == null) null else editingId
            }
            if (resultId == null) {
                localState.update {
                    it.copy(newCategory = category.copy(errorRes = R.string.category_error_duplicate))
                }
            } else {
                localState.update {
                    it.copy(
                        isCategoryDialogVisible = false,
                        newCategory = NewCategoryState(),
                        // Новая категория сразу выбрана в форме расхода
                        form = it.form?.copy(categoryId = resultId)
                    )
                }
            }
        }
    }

    // --- Удаление собственной категории ---

    /** Открыть диалог подтверждения удаления; заодно подгрузить число операций категории. */
    fun onDeleteCategoryClick(category: CategoryEntity) {
        viewModelScope.launch {
            val count = transactionRepository.countTransactionsByCategory(category.id)
            localState.update {
                it.copy(deleteCategoryTargetId = category.id, deleteCategoryUsageCount = count)
            }
        }
    }

    fun onDeleteCategoryDismiss() {
        localState.update { it.copy(deleteCategoryTargetId = null, deleteCategoryUsageCount = 0) }
    }

    fun onDeleteCategoryConfirm() {
        val target = uiState.value.deleteCategoryTarget ?: return
        viewModelScope.launch {
            categoryRepository.deleteCategory(target)
            localState.update {
                it.copy(
                    deleteCategoryTargetId = null,
                    deleteCategoryUsageCount = 0,
                    // Категория удалена — в форме она больше не выбрана.
                    form = if (it.form?.categoryId == target.id) {
                        it.form.copy(categoryId = null)
                    } else {
                        it.form
                    },
                    messageRes = R.string.category_deleted
                )
            }
        }
    }

    fun onWalletSelect(walletId: Long?) {
        localState.update { state ->
            state.copy(form = state.form?.copy(walletId = walletId, walletErrorRes = null))
        }
    }

    fun onSave() {
        val form = localState.value.form ?: return

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
                localState.update {
                    it.copy(form = null, messageRes = R.string.expense_saved)
                }
            } else {
                transactionRepository.updateTransaction(transaction)
                localState.update { it.copy(form = null) }
            }
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
        localState.update { it.copy(form = null) }
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
    /** null — диалог формы закрыт. */
    val form: ExpenseFormState? = null,
    val deleteTargetId: Long? = null,
    val isCategoryDialogVisible: Boolean = false,
    val newCategory: NewCategoryState = NewCategoryState(),
    val deleteCategoryTargetId: Long? = null,
    val deleteCategoryUsageCount: Int = 0,
    @StringRes val messageRes: Int? = null
)