package com.budget.app.ui.wallets

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budget.app.R
import com.budget.app.data.local.entity.TransferEntity
import com.budget.app.data.local.entity.WalletEntity
import com.budget.app.data.local.model.TransferWithDetails
import com.budget.app.data.local.model.WalletWithBalance
import com.budget.app.data.repository.TransferRepository
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
    /** Архивный кошелёк скрыт в основном списке и недоступен для выбора в операциях. */
    val archived: Boolean = false,
    /** Текущий баланс в копейках: начальный + доходы − расходы + переводы. */
    val balance: Long
)

/** Перевод в том виде, в каком его показывает экран. */
data class TransferUi(
    val id: Long,
    val fromWalletName: String,
    val toWalletName: String,
    /** Сумма в копейках. */
    val amount: Long,
    val date: Long,
    val note: String?
)

/** Состояние открытого диалога добавления или редактирования кошелька. */
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

/** Состояние диалога перевода между кошельками. */
data class TransferFormState(
    val fromWalletId: Long? = null,
    val toWalletId: Long? = null,
    val amountInput: String = "",
    val note: String = "",
    @StringRes val walletsErrorRes: Int? = null,
    @StringRes val amountErrorRes: Int? = null
)

/** Разовое сообщение для Snackbar. */
sealed interface Message {
    /** Кошелёк не удалось удалить: по нему есть операции или переводы. */
    data class WalletHasTransactions(val walletName: String) : Message

    /** Перевод сохранён. */
    data object TransferSaved : Message

    /** Перевод удалён. */
    data object TransferDeleted : Message

    /** Кошелёк убран в архив. */
    data object WalletArchived : Message

    /** Кошелёк возвращён из архива. */
    data object WalletUnarchived : Message
}

/** Единственное состояние экрана «Кошельки». */
data class WalletsUiState(
    val wallets: List<WalletUi> = emptyList(),
    /** Архивные кошельки — отдельный раздел внизу экрана. */
    val archivedWallets: List<WalletUi> = emptyList(),
    /** Последние переводы между кошельками. */
    val transfers: List<TransferUi> = emptyList(),
    /** Сумма балансов активных кошельков в копейках. */
    val total: Long = 0,
    val isLoading: Boolean = true,
    val form: WalletFormState? = null,
    /** Открытый диалог перевода; null — диалог закрыт. */
    val transferForm: TransferFormState? = null,
    /** Кошелёк, для которого открыт диалог подтверждения удаления. */
    val deleteTarget: WalletUi? = null,
    /** Перевод, для которого открыт диалог подтверждения удаления. */
    val deleteTransferTarget: TransferUi? = null,
    /** Разовое сообщение для Snackbar. */
    val message: Message? = null
)

@HiltViewModel
class WalletsViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transferRepository: TransferRepository
) : ViewModel() {

    /** Состояние диалогов и разовых сообщений — меняется только событиями с экрана. */
    private val localState = MutableStateFlow(WalletsLocalState())

    val uiState: StateFlow<WalletsUiState> = combine(
        walletRepository.getActiveWalletsWithBalance(),
        walletRepository.getArchivedWalletsWithBalance(),
        transferRepository.getTransfersWithDetails(),
        localState
    ) { active, archived, transfers, local ->
        val mapped = active.map { it.toUi() }
        val mappedArchived = archived.map { it.toUi() }
        val mappedTransfers = transfers.map { it.toUi() }
        WalletsUiState(
            wallets = mapped,
            archivedWallets = mappedArchived,
            transfers = mappedTransfers.take(MAX_VISIBLE_TRANSFERS),
            total = mapped.sumOf { it.balance },
            isLoading = false,
            form = local.form,
            transferForm = local.transferForm,
            deleteTarget = local.deleteTargetId?.let { id -> mapped.firstOrNull { it.id == id } },
            deleteTransferTarget = local.deleteTransferId?.let { id ->
                mappedTransfers.firstOrNull { it.id == id }
            },
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

    // --- Перевод между кошельками ---

    fun onTransferClick() {
        val wallets = uiState.value.wallets
        localState.update {
            it.copy(
                transferForm = TransferFormState(
                    // Кошельки по умолчанию разные: переводить в тот же нельзя.
                    fromWalletId = wallets.firstOrNull()?.id,
                    toWalletId = wallets.drop(1).firstOrNull()?.id
                )
            )
        }
    }

    fun onTransferDismiss() {
        localState.update { it.copy(transferForm = null) }
    }

    fun onTransferFromSelect(walletId: Long) {
        localState.update { state ->
            // Если «откуда» совпало с «куда», сбрасываем второе — иначе перевод невозможен.
            val form = state.transferForm ?: return@update state
            state.copy(
                transferForm = form.copy(
                    fromWalletId = walletId,
                    toWalletId = form.toWalletId.takeIf { it != walletId },
                    walletsErrorRes = null,
                    amountErrorRes = null
                )
            )
        }
    }

    fun onTransferToSelect(walletId: Long) {
        localState.update { state ->
            val form = state.transferForm ?: return@update state
            state.copy(
                transferForm = form.copy(
                    toWalletId = walletId,
                    walletsErrorRes = null,
                    amountErrorRes = null
                )
            )
        }
    }

    /** В поле суммы попадают только цифры и разделитель — буквы не вводятся вовсе. */
    fun onTransferAmountChange(input: String) {
        val filtered = MoneyFormatter.filterAmountInput(input)
        localState.update { state ->
            state.copy(
                transferForm = state.transferForm?.copy(
                    amountInput = filtered,
                    amountErrorRes = null
                )
            )
        }
    }

    fun onTransferNoteChange(note: String) {
        localState.update { state ->
            state.copy(transferForm = state.transferForm?.copy(note = note))
        }
    }

    fun onTransferSave() {
        val form = localState.value.transferForm ?: return

        val fromId = form.fromWalletId
        val toId = form.toWalletId
        val amount = MoneyFormatter.parseToKopecks(form.amountInput)

        val walletsError = R.string.transfer_error_wallets.takeIf { fromId == null || toId == null || fromId == toId }
        val amountError = R.string.transfer_error_amount.takeIf { amount == null || amount <= 0 }

        if (walletsError != null || amountError != null || fromId == null || toId == null || amount == null) {
            localState.update {
                it.copy(
                    transferForm = form.copy(
                        walletsErrorRes = walletsError,
                        amountErrorRes = amountError
                    )
                )
            }
            return
        }

        // Нельзя перевести больше остатка кошелька-источника.
        val source = uiState.value.wallets.firstOrNull { it.id == fromId }
        if (source != null && amount > source.balance) {
            localState.update {
                it.copy(transferForm = form.copy(amountErrorRes = R.string.transfer_error_not_enough))
            }
            return
        }

        viewModelScope.launch {
            transferRepository.insertTransfer(
                TransferEntity(
                    fromWalletId = fromId,
                    toWalletId = toId,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    note = form.note.trim().ifBlank { null }
                )
            )

            localState.update { it.copy(transferForm = null, message = Message.TransferSaved) }
        }
    }

    // --- Архив ---

    /** Убирает кошелёк с экрана и из списков выбора в расходах/доходах/копилках. */
    fun onArchiveClick(wallet: WalletUi) {
        viewModelScope.launch {
            walletRepository.setWalletArchived(wallet.id, archived = true)
            localState.update { it.copy(message = Message.WalletArchived) }
        }
    }

    /** Возвращает кошелёк из архива — операция обратима. */
    fun onUnarchiveClick(wallet: WalletUi) {
        viewModelScope.launch {
            walletRepository.setWalletArchived(wallet.id, archived = false)
            localState.update { it.copy(message = Message.WalletUnarchived) }
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

    fun onDeleteTransferClick(transfer: TransferUi) {
        localState.update { it.copy(deleteTransferId = transfer.id) }
    }

    fun onDeleteTransferDismiss() {
        localState.update { it.copy(deleteTransferId = null) }
    }

    fun onDeleteTransferConfirm() {
        val id = localState.value.deleteTransferId ?: return

        viewModelScope.launch {
            transferRepository.deleteTransferById(id)
            localState.update {
                it.copy(deleteTransferId = null, message = Message.TransferDeleted)
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
        archived = archived,
        balance = balance
    )

    private fun TransferWithDetails.toUi() = TransferUi(
        id = id,
        fromWalletName = fromWalletName,
        toWalletName = toWalletName,
        amount = amount,
        date = date,
        note = note
    )

    private companion object {
        /** Больше переводов на экране «Кошельки» не нужно — список операций ведётся отдельно. */
        const val MAX_VISIBLE_TRANSFERS = 20
    }
}

/** Часть состояния, которой управляет только сам экран. */
private data class WalletsLocalState(
    val form: WalletFormState? = null,
    val transferForm: TransferFormState? = null,
    val deleteTargetId: Long? = null,
    /** Имя удаляемого кошелька — его может не быть в списке, если он уже удалён. */
    val deleteTargetName: String? = null,
    val deleteTransferId: Long? = null,
    val message: Message? = null
)