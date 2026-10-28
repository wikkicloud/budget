package com.budget.app.ui.savings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budget.app.R
import com.budget.app.data.local.entity.SavingsEntity
import com.budget.app.data.local.entity.SavingsOperationEntity
import com.budget.app.data.local.model.SavingsOperationWithDetails
import com.budget.app.data.local.model.SavingsWithBalance
import com.budget.app.data.local.model.WalletWithBalance
import com.budget.app.data.repository.SavingsRepository
import com.budget.app.data.repository.WalletRepository
import com.budget.app.domain.model.SavingsOperationType
import com.budget.app.domain.model.SavingsProgress
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

/** Копилка в том виде, в каком её показывает экран. */
data class SavingsUi(
    val id: Long,
    val name: String,
    val createdAt: Long,
    /** Цель в копейках; null — копилка без цели. */
    val targetAmount: Long?,
    /** Стартовая сумма в копейках. */
    val initialAmount: Long,
    /** Баланс в копейках. */
    val balance: Long
) {
    /** Доля от цели, 0..1; null — цели нет. */
    val progress: Float? get() = SavingsProgress.fraction(balance, targetAmount)

    /** Целые проценты от цели; null — цели нет. */
    val progressPercent: Int? get() = SavingsProgress.percent(balance, targetAmount)
}

/** Операция по копилке в том виде, в каком её показывает экран. */
data class SavingsOperationUi(
    val id: Long,
    val savingsName: String,
    val type: SavingsOperationType,
    /** Сумма в копейках. */
    val amount: Long,
    val date: Long,
    /** Название кошелька; null — операция не связана с кошельком (проценты). */
    val walletName: String?
)

/** Состояние диалога создания или редактирования копилки. */
data class SavingsFormState(
    /** null — создание, иначе — редактирование копилки с этим id. */
    val id: Long? = null,
    val name: String = "",
    /** Цель в виде введённой суммы («1 000 000,50»); пусто — копилка без цели. */
    val targetInput: String = "",
    /** Стартовая сумма в виде введённой суммы; пусто — ноль. */
    val initialInput: String = "",
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val targetErrorRes: Int? = null,
    @StringRes val initialErrorRes: Int? = null
) {
    /** true — правим существующую копилку, false — создаём новую. */
    val isEditing: Boolean get() = id != null
}

/** Действие с копилкой, для которого открыт диалог: пополнение, вывод или проценты. */
enum class SavingsActionKind { DEPOSIT, WITHDRAW, INTEREST }

/** Состояние диалога пополнения, вывода или начисления процентов. */
data class SavingsActionState(
    val savingsId: Long,
    val savingsName: String,
    val balance: Long,
    val kind: SavingsActionKind,
    val walletId: Long? = null,
    val amountInput: String = "",
    @StringRes val walletErrorRes: Int? = null,
    @StringRes val amountErrorRes: Int? = null
)

/** Разовое сообщение для Snackbar. */
sealed interface Message {
    /** Копилка создана. */
    data object SavingsCreated : Message

    /** Изменения копилки сохранены. */
    data object SavingsUpdated : Message

    /** Копилка удалена вместе с операциями. */
    data object SavingsDeleted : Message
}

/** Единственное состояние экрана «Копилки». */
data class SavingsUiState(
    val savings: List<SavingsUi> = emptyList(),
    /** Кошельки, доступные для пополнения и вывода. */
    val wallets: List<SavingsWalletUi> = emptyList(),
    /** Последние операции по копилкам. */
    val operations: List<SavingsOperationUi> = emptyList(),
    /** Итого во всех копилках в копейках. */
    val total: Long = 0,
    /** Копилка, раскрытая тапом: видны прогресс, стартовая сумма и кнопки действий. */
    val expandedId: Long? = null,
    val isLoading: Boolean = true,
    val form: SavingsFormState? = null,
    val action: SavingsActionState? = null,
    /** Копилка, для которой открыт диалог подтверждения удаления. */
    val deleteTarget: SavingsUi? = null,
    /** Сколько операций есть у удаляемой копилки — показывается в диалоге. */
    val deleteTargetOperationCount: Int = 0,
    /** Операция, для которой открыт диалог подтверждения удаления. */
    val deleteOperationTarget: SavingsOperationUi? = null,
    val message: Message? = null
)

/** Кошелёк для выбора в диалоге пополнения или вывода. */
data class SavingsWalletUi(
    val id: Long,
    val name: String,
    /** Баланс в копейках. */
    val balance: Long
)

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    /** Состояние диалогов и разовых сообщений — меняется только событиями с экрана. */
    private val localState = MutableStateFlow(SavingsLocalState())

    val uiState: StateFlow<SavingsUiState> = combine(
        savingsRepository.getSavingsWithBalance(),
        walletRepository.getActiveWalletsWithBalance(),
        savingsRepository.getOperationsWithDetails(),
        localState
    ) { savings, wallets, operations, local ->
        val mapped = savings.map { it.toUi() }
        val mappedOperations = operations.map { it.toUi() }
        val mappedWallets = wallets.map { SavingsWalletUi(it.id, it.name, it.balance) }

        SavingsUiState(
            savings = mapped,
            wallets = mappedWallets,
            operations = mappedOperations.take(MAX_VISIBLE_OPERATIONS),
            total = mapped.sumOf { it.balance },
            // Раскрытая копилка могла быть удалена — тогда панель закрывается сама
            expandedId = local.expandedId?.takeIf { id -> mapped.any { it.id == id } },
            isLoading = false,
            form = local.form,
            action = local.action,
            deleteTarget = local.deleteSavingsId?.let { id -> mapped.firstOrNull { it.id == id } },
            deleteTargetOperationCount = local.deleteOperationCount,
            deleteOperationTarget = local.deleteOperationId?.let { id ->
                mappedOperations.firstOrNull { it.id == id }
            },
            message = local.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavingsUiState()
    )

    // --- Раскрытие копилки ---

    /** Тап по копилке раскрывает детали и действия; повторный тап сворачивает. */
    fun onSavingsToggle(savings: SavingsUi) {
        localState.update {
            it.copy(expandedId = if (it.expandedId == savings.id) null else savings.id)
        }
    }

    // --- Создание и редактирование копилки ---

    fun onAddClick() {
        localState.update { it.copy(form = SavingsFormState()) }
    }

    /** Открывает диалог с данными существующей копилки. */
    fun onEditClick(savings: SavingsUi) {
        localState.update {
            it.copy(
                form = SavingsFormState(
                    id = savings.id,
                    name = savings.name,
                    // Цель и стартовая сумма показываются так же, как вводятся: с разрядами
                    targetInput = savings.targetAmount?.let { target ->
                        MoneyFormatter.groupAmountInput(MoneyFormatter.formatPlain(target))
                    }.orEmpty(),
                    initialInput = savings.initialAmount
                        .takeIf { it > 0 }
                        ?.let { amount ->
                            MoneyFormatter.groupAmountInput(MoneyFormatter.formatPlain(amount))
                        }
                        .orEmpty()
                )
            )
        }
    }

    fun onFormDismiss() {
        localState.update { it.copy(form = null) }
    }

    fun onFormNameChange(name: String) {
        localState.update { state ->
            state.copy(form = state.form?.copy(name = name, nameErrorRes = null))
        }
    }

    fun onFormTargetChange(input: String) {
        // Цель — сумма: разряды разделяются прямо при наборе («1 000 000»)
        val filtered = MoneyFormatter.groupAmountInput(input)
        localState.update { state ->
            state.copy(form = state.form?.copy(targetInput = filtered, targetErrorRes = null))
        }
    }

    fun onFormInitialChange(input: String) {
        // Стартовая сумма — тоже сумма: разряды разделяются при наборе
        val filtered = MoneyFormatter.groupAmountInput(input)
        localState.update { state ->
            state.copy(form = state.form?.copy(initialInput = filtered, initialErrorRes = null))
        }
    }

    fun onFormSave() {
        val form = localState.value.form ?: return

        val name = form.name.trim()
        // Цель необязательна: пустое поле — копилка без цели
        val target = if (form.targetInput.isBlank()) null else MoneyFormatter.parseToKopecks(form.targetInput)
        // Стартовая сумма необязательна: пустое поле — ноль
        val initial = if (form.initialInput.isBlank()) 0L else MoneyFormatter.parseToKopecks(form.initialInput)

        val nameError = R.string.savings_error_name.takeIf { name.isEmpty() }
        val targetError = R.string.savings_error_target.takeIf {
            form.targetInput.isNotBlank() && (target == null || target <= 0)
        }
        // Ноль допустим (пустая копилка), отрицательных сумм не бывает — минус отбрасывает сам фильтр ввода
        val initialError = R.string.savings_error_initial.takeIf {
            form.initialInput.isNotBlank() && initial == null
        }

        if (nameError != null || targetError != null || initialError != null || initial == null) {
            localState.update {
                it.copy(
                    form = form.copy(
                        nameErrorRes = nameError,
                        targetErrorRes = targetError,
                        initialErrorRes = initialError
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            if (form.id == null) {
                savingsRepository.insertSavings(
                    SavingsEntity(
                        name = name,
                        // Дата создания ставится автоматически
                        createdAt = System.currentTimeMillis(),
                        targetAmount = target,
                        initialAmount = initial
                    )
                )

                localState.update { it.copy(form = null, message = Message.SavingsCreated) }
            } else {
                // Дата создания при редактировании не меняется — берём её из сохранённой копилки
                val existing = savingsRepository.getSavingsById(form.id)
                savingsRepository.updateSavings(
                    SavingsEntity(
                        id = form.id,
                        name = name,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        targetAmount = target,
                        initialAmount = initial
                    )
                )

                localState.update { it.copy(form = null, message = Message.SavingsUpdated) }
            }
        }
    }

    // --- Пополнение, вывод и проценты ---

    fun onDepositClick(savings: SavingsUi) = openAction(savings, SavingsActionKind.DEPOSIT)

    fun onWithdrawClick(savings: SavingsUi) = openAction(savings, SavingsActionKind.WITHDRAW)

    fun onInterestClick(savings: SavingsUi) = openAction(savings, SavingsActionKind.INTEREST)

    private fun openAction(savings: SavingsUi, kind: SavingsActionKind) {
        localState.update {
            it.copy(
                action = SavingsActionState(
                    savingsId = savings.id,
                    savingsName = savings.name,
                    balance = savings.balance,
                    kind = kind,
                    // Для пополнения нужен кошелёк-источник, для вывода — кошелёк-получатель
                    walletId = if (kind == SavingsActionKind.INTEREST) {
                        null
                    } else {
                        uiState.value.wallets.firstOrNull()?.id
                    }
                )
            )
        }
    }

    fun onActionDismiss() {
        localState.update { it.copy(action = null) }
    }

    fun onActionWalletSelect(walletId: Long) {
        localState.update { state ->
            state.copy(
                action = state.action?.copy(
                    walletId = walletId,
                    amountErrorRes = null,
                    walletErrorRes = null
                )
            )
        }
    }

    fun onActionAmountChange(input: String) {
        val filtered = MoneyFormatter.filterAmountInput(input)
        localState.update { state ->
            state.copy(action = state.action?.copy(amountInput = filtered, amountErrorRes = null))
        }
    }

    fun onActionSave() {
        val action = localState.value.action ?: return

        val amount = MoneyFormatter.parseToKopecks(action.amountInput)
        val needsWallet = action.kind != SavingsActionKind.INTEREST

        val walletError = R.string.savings_error_wallet.takeIf { needsWallet && action.walletId == null }
        val amountError = R.string.savings_error_amount.takeIf { amount == null || amount <= 0 }

        if (walletError != null || amountError != null || amount == null) {
            localState.update {
                it.copy(
                    action = action.copy(walletErrorRes = walletError, amountErrorRes = amountError)
                )
            }
            return
        }

        when {
            // Пополнение: денег на кошельке должно хватать — как при переводе
            action.kind == SavingsActionKind.DEPOSIT -> {
                val wallet = uiState.value.wallets.firstOrNull { it.id == action.walletId }
                if (wallet != null && amount > wallet.balance) {
                    localState.update {
                        it.copy(action = action.copy(amountErrorRes = R.string.savings_error_not_enough))
                    }
                    return
                }
            }

            // Из копилки нельзя вернуть больше, чем в ней есть
            action.kind == SavingsActionKind.WITHDRAW && amount > action.balance -> {
                localState.update {
                    it.copy(action = action.copy(amountErrorRes = R.string.savings_error_not_enough_savings))
                }
                return
            }
        }

        viewModelScope.launch {
            savingsRepository.insertOperation(
                SavingsOperationEntity(
                    savingsId = action.savingsId,
                    walletId = action.walletId.takeIf { needsWallet },
                    amount = amount,
                    type = action.kind.name,
                    date = System.currentTimeMillis()
                )
            )

            localState.update { it.copy(action = null) }
        }
    }

    // --- Удаление ---

    fun onDeleteClick(savings: SavingsUi) {
        localState.update { it.copy(deleteSavingsId = savings.id) }
        // Число операций показывается в диалоге — считаем его запросом, а не по видимому списку
        viewModelScope.launch {
            val count = savingsRepository.countOperationsBySavings(savings.id)
            localState.update { state ->
                if (state.deleteSavingsId == savings.id) {
                    state.copy(deleteOperationCount = count)
                } else {
                    state
                }
            }
        }
    }

    fun onDeleteDismiss() {
        localState.update { it.copy(deleteSavingsId = null, deleteOperationCount = 0) }
    }

    fun onDeleteConfirm() {
        val id = localState.value.deleteSavingsId ?: return

        viewModelScope.launch {
            // Операции уходят вместе с копилкой (внешний ключ с CASCADE)
            savingsRepository.deleteSavingsById(id)
            localState.update {
                it.copy(
                    deleteSavingsId = null,
                    deleteOperationCount = 0,
                    message = Message.SavingsDeleted
                )
            }
        }
    }

    fun onDeleteOperationClick(operation: SavingsOperationUi) {
        localState.update { it.copy(deleteOperationId = operation.id) }
    }

    fun onDeleteOperationDismiss() {
        localState.update { it.copy(deleteOperationId = null) }
    }

    fun onDeleteOperationConfirm() {
        val id = localState.value.deleteOperationId ?: return

        viewModelScope.launch {
            savingsRepository.deleteOperationById(id)
            localState.update { it.copy(deleteOperationId = null) }
        }
    }

    // --- Разовые сообщения ---

    fun onMessageShown() {
        localState.update { it.copy(message = null) }
    }

    private fun SavingsWithBalance.toUi() = SavingsUi(
        id = id,
        name = name,
        createdAt = createdAt,
        targetAmount = targetAmount,
        initialAmount = initialAmount,
        balance = balance
    )

    private fun SavingsOperationWithDetails.toUi() = SavingsOperationUi(
        id = id,
        savingsName = savingsName,
        type = SavingsOperationType.entries.firstOrNull { it.name == type }
            ?: SavingsOperationType.DEPOSIT,
        amount = amount,
        date = date,
        walletName = walletName
    )

    private companion object {
        /** Больше операций на экране не нужно — история ведётся по копилкам целиком. */
        const val MAX_VISIBLE_OPERATIONS = 50
    }
}

/** Часть состояния, которой управляет только сам экран. */
private data class SavingsLocalState(
    /** id раскрытой копилки; одновременно раскрыта только одна. */
    val expandedId: Long? = null,
    val form: SavingsFormState? = null,
    val action: SavingsActionState? = null,
    val deleteSavingsId: Long? = null,
    /** Число операций удаляемой копилки — известно только после запроса. */
    val deleteOperationCount: Int = 0,
    val deleteOperationId: Long? = null,
    val message: Message? = null
)