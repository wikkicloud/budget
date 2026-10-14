package com.budget.app.ui.wallets

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budget.app.R
import com.budget.app.data.local.entity.WalletEntity
import com.budget.app.data.local.model.WalletWithBalance
import com.budget.app.data.repository.WalletRepository
import com.budget.app.domain.model.WalletType
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

/** Кошелёк в том виде, в каком его показывает экран. */
data class WalletUi(
    val id: Long,
    val name: String,
    val type: WalletType,
    /** Начальный баланс в копейках — нужен для заполнения формы редактирования. */
    val initialBalance: Long,
    /** Текущий баланс в копейках: начальный + доходы − расходы. */
    val balance: Long
)

/** Состояние открытого диалога добавления или редактирования. */
data class WalletFormState(
    /** null — создание нового кошелька. */
    val id: Long? = null,
    val name: String = "",
    val type: WalletType = WalletType.CARD,
    val initialBalanceInput: String = "",
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val amountErrorRes: Int? = null
) {
    val isEditing: Boolean get() = id != null
}

/** Разовое сообщение для Snackbar. */
sealed interface Message {
    /** Кошелёк не удалось удалить: по нему есть операции. */
    data class WalletHasTransactions(val walletName: String) : Message
}

/** Единственное состояние экрана «Кошельки». */
data class WalletsUiState(
    val wallets: List<WalletUi> = emptyList(),
    /** Сумма балансов всех кошельков в копейках. */
    val total: Long = 0,
    val isLoading: Boolean = true,
    val form: WalletFormState? = null,
    /** Кошелёк, для которого открыт диалог подтверждения удаления. */
    val deleteTarget: WalletUi? = null,
    /** Разовое сообщение для Snackbar. */
    val message: Message? = null
)

@HiltViewModel
class WalletsViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    /** Состояние диалогов и разовых сообщений — меняется только событиями с экрана. */
    private val localState = MutableStateFlow(WalletsLocalState())

    val uiState: StateFlow<WalletsUiState> = combine(
        walletRepository.getWalletsWithBalance(),
        localState
    ) { wallets, local ->
        val mapped = wallets.map { it.toUi() }
        WalletsUiState(
            wallets = mapped,
            total = mapped.sumOf { it.balance },
            isLoading = false,
            form = local.form,
            deleteTarget = local.deleteTargetId?.let { id -> mapped.firstOrNull { it.id == id } },
            message = local.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletsUiState()
    )

    // --- Диалог добавления / редактирования ---

    fun onAddClick() {
        localState.update { it.copy(form = WalletFormState()) }
    }

    fun onEditClick(wallet: WalletUi) {
        localState.update {
            it.copy(
                form = WalletFormState(
                    id = wallet.id,
                    name = wallet.name,
                    type = wallet.type,
                    initialBalanceInput = MoneyFormatter.formatPlain(wallet.initialBalance)
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

    fun onFormTypeChange(type: WalletType) {
        localState.update { state ->
            state.copy(form = state.form?.copy(type = type))
        }
    }

    fun onFormAmountChange(input: String) {
        localState.update { state ->
            state.copy(form = state.form?.copy(initialBalanceInput = input, amountErrorRes = null))
        }
    }

    fun onFormSave() {
        val form = localState.value.form ?: return

        val name = form.name.trim()
        val amount = MoneyFormatter.parseToKopecks(form.initialBalanceInput)

        val nameError = R.string.wallet_error_name.takeIf { name.isEmpty() }
        val amountError = R.string.wallet_error_amount.takeIf { amount == null || amount < 0 }

        if (nameError != null || amountError != null || amount == null) {
            localState.update {
                it.copy(form = form.copy(nameErrorRes = nameError, amountErrorRes = amountError))
            }
            return
        }

        viewModelScope.launch {
            val wallet = WalletEntity(
                id = form.id ?: 0,
                name = name,
                type = form.type.name,
                initialBalance = amount
            )

            if (form.id == null) {
                walletRepository.insertWallet(wallet)
            } else {
                walletRepository.updateWallet(wallet)
            }

            localState.update { it.copy(form = null) }
        }
    }

    // --- Удаление ---

    fun onDeleteClick(wallet: WalletUi) {
        localState.update { it.copy(deleteTargetId = wallet.id, deleteTargetName = wallet.name) }
    }

    fun onDeleteDismiss() {
        localState.update { it.copy(deleteTargetId = null, deleteTargetName = null) }
    }

    fun onDeleteConfirm() {
        val id = localState.value.deleteTargetId ?: return
        val name = localState.value.deleteTargetName.orEmpty()

        viewModelScope.launch {
            val deleted = walletRepository.deleteWalletIfUnused(id)
            localState.update {
                it.copy(
                    deleteTargetId = null,
                    deleteTargetName = null,
                    message = if (deleted) null else Message.WalletHasTransactions(name)
                )
            }
        }
    }

    // --- Разовые сообщения ---

    fun onMessageShown() {
        localState.update { it.copy(message = null) }
    }

    private fun WalletWithBalance.toUi() = WalletUi(
        id = id,
        name = name,
        type = WalletType.entries.firstOrNull { it.name == type } ?: WalletType.CARD,
        initialBalance = initialBalance,
        balance = balance
    )
}

/** Часть состояния, которой управляет только сам экран. */
private data class WalletsLocalState(
    val form: WalletFormState? = null,
    val deleteTargetId: Long? = null,
    /** Имя удаляемого кошелька — его может не быть в списке, если он уже удалён. */
    val deleteTargetName: String? = null,
    val message: Message? = null
)