package com.budget.app.ui.wallets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budget.app.R
import com.budget.app.domain.model.WalletType
import com.budget.app.util.DateFormatter
import com.budget.app.util.MoneyFormatter

@Composable
fun WalletsScreen(
    viewModel: WalletsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    val messageText = when (val message = state.message) {
        null -> null
        is Message.WalletHasTransactions ->
            resources.getString(R.string.wallet_delete_has_transactions, message.walletName)
        Message.TransferSaved -> resources.getString(R.string.transfer_saved)
        Message.TransferDeleted -> resources.getString(R.string.transfer_deleted)
    }

    messageText?.let { text ->
        LaunchedEffect(text) {
            snackbarHostState.showSnackbar(text)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAddClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.wallet_add)
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Text(
                text = stringResource(R.string.screen_wallets),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TotalCard(
                total = state.total,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // Переводить можно только между двумя разными кошельками.
            if (state.wallets.size >= 2) {
                OutlinedButton(
                    onClick = viewModel::onTransferClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.transfer_button))
                }
            }

            if (state.wallets.isEmpty() && !state.isLoading) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.wallets, key = { it.id }) { wallet ->
                        WalletRow(
                            wallet = wallet,
                            onEditClick = { viewModel.onEditClick(wallet) },
                            onDeleteClick = { viewModel.onDeleteClick(wallet) }
                        )
                    }

                    item {
                        Text(
                            text = stringResource(R.string.transfers_recent),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (state.transfers.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.transfers_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(state.transfers, key = { "transfer-${it.id}" }) { transfer ->
                            TransferRow(
                                transfer = transfer,
                                onDeleteClick = { viewModel.onDeleteTransferClick(transfer) }
                            )
                        }
                    }
                }
            }
        }
    }

    state.form?.let { form ->
        WalletFormDialog(
            form = form,
            onNameChange = viewModel::onFormNameChange,
            onTypeChange = viewModel::onFormTypeChange,
            onAmountChange = viewModel::onFormAmountChange,
            onSave = viewModel::onFormSave,
            onDismiss = viewModel::onFormDismiss
        )
    }

    state.transferForm?.let { form ->
        TransferDialog(
            form = form,
            wallets = state.wallets,
            onFromSelect = viewModel::onTransferFromSelect,
            onToSelect = viewModel::onTransferToSelect,
            onAmountChange = viewModel::onTransferAmountChange,
            onNoteChange = viewModel::onTransferNoteChange,
            onSave = viewModel::onTransferSave,
            onDismiss = viewModel::onTransferDismiss
        )
    }

    state.deleteTarget?.let { wallet ->
        DeleteWalletDialog(
            wallet = wallet,
            onConfirm = viewModel::onDeleteConfirm,
            onDismiss = viewModel::onDeleteDismiss
        )
    }

    state.deleteTransferTarget?.let { transfer ->
        DeleteTransferDialog(
            transfer = transfer,
            onConfirm = viewModel::onDeleteTransferConfirm,
            onDismiss = viewModel::onDeleteTransferDismiss
        )
    }
}

@Composable
private fun TotalCard(
    total: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.wallets_total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = MoneyFormatter.format(total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun WalletRow(
    wallet: WalletUi,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (wallet.type) {
                    WalletType.CARD -> Icons.Filled.AccountBalanceWallet
                    WalletType.CASH -> Icons.Filled.Payments
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(wallet.type.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = MoneyFormatter.format(wallet.balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.action_edit)
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete)
                )
            }
        }
    }
}

@Composable
private fun TransferRow(
    transfer: TransferUi,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.transfer_row,
                        transfer.fromWalletName,
                        transfer.toWalletName
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = listOfNotNull(
                        transfer.note?.takeIf { it.isNotBlank() },
                        DateFormatter.formatDate(transfer.date)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = MoneyFormatter.format(transfer.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.wallets_empty),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.wallets_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WalletFormDialog(
    form: WalletFormState,
    onNameChange: (String) -> Unit,
    onTypeChange: (WalletType) -> Unit,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (form.isEditing) R.string.wallet_edit_title else R.string.wallet_new_title
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.wallet_name)) },
                    singleLine = true,
                    isError = form.nameErrorRes != null,
                    supportingText = form.nameErrorRes?.let { { Text(stringResource(it)) } }
                )

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = stringResource(R.string.wallet_type),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.size(8.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    WalletType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = form.type == type,
                            onClick = { onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = WalletType.entries.size
                            )
                        ) {
                            Text(stringResource(type.labelRes))
                        }
                    }
                }

                Spacer(modifier = Modifier.size(16.dp))

                OutlinedTextField(
                    value = form.initialBalanceInput,
                    onValueChange = onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.wallet_initial_balance)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = form.amountErrorRes != null,
                    supportingText = form.amountErrorRes?.let { { Text(stringResource(it)) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Диалог перевода: откуда, куда, сумма и необязательный комментарий. */
@Composable
private fun TransferDialog(
    form: TransferFormState,
    wallets: List<WalletUi>,
    onFromSelect: (Long) -> Unit,
    onToSelect: (Long) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val from = wallets.firstOrNull { it.id == form.fromWalletId }
    val to = wallets.firstOrNull { it.id == form.toWalletId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.transfer_title)) },
        text = {
            Column {
                WalletPicker(
                    labelRes = R.string.transfer_from,
                    selectedName = from?.let { "${it.name} · ${MoneyFormatter.format(it.balance)}" },
                    wallets = wallets,
                    onSelect = onFromSelect
                )

                Spacer(modifier = Modifier.size(12.dp))

                WalletPicker(
                    labelRes = R.string.transfer_to,
                    selectedName = to?.let { "${it.name} · ${MoneyFormatter.format(it.balance)}" },
                    wallets = wallets,
                    onSelect = onToSelect
                )

                form.walletsErrorRes?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                OutlinedTextField(
                    value = form.amountInput,
                    onValueChange = onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.transfer_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = form.amountErrorRes != null,
                    supportingText = form.amountErrorRes?.let { { Text(stringResource(it)) } }
                )

                Spacer(modifier = Modifier.size(8.dp))

                OutlinedTextField(
                    value = form.note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.transfer_note)) },
                    placeholder = { Text(stringResource(R.string.transfer_note_optional)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.transfer_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Выбор кошелька кнопкой с выпадающим списком — как в форме расхода. */
@Composable
private fun WalletPicker(
    @StringRes labelRes: Int,
    selectedName: String?,
    wallets: List<WalletUi>,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = selectedName ?: stringResource(R.string.transfer_none)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(labelRes)
                )
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                wallets.forEach { wallet ->
                    DropdownMenuItem(
                        text = { Text(wallet.name) },
                        onClick = {
                            onSelect(wallet.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteWalletDialog(
    wallet: WalletUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wallet_delete_title)) },
        text = { Text(stringResource(R.string.wallet_delete_message, wallet.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun DeleteTransferDialog(
    transfer: TransferUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.transfer_delete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.transfer_delete_message,
                    MoneyFormatter.format(transfer.amount),
                    transfer.fromWalletName,
                    transfer.toWalletName
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Подпись типа кошелька для интерфейса. */
private val WalletType.labelRes: Int
    get() = when (this) {
        WalletType.CARD -> R.string.wallet_card
        WalletType.CASH -> R.string.wallet_cash
    }