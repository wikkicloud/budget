package com.budget.app.ui.income

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.budget.app.data.local.entity.WalletEntity
import com.budget.app.domain.model.WalletType
import com.budget.app.util.DateFormatter
import com.budget.app.util.MoneyFormatter

@Composable
fun IncomeScreen(
    viewModel: IncomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    state.messageRes?.let { messageRes ->
        LaunchedEffect(messageRes) {
            snackbarHostState.showSnackbar(resources.getString(messageRes))
            viewModel.onMessageShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                IncomeFormCard(
                    state = state,
                    onAmountChange = viewModel::onAmountChange,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onWalletSelect = viewModel::onWalletSelect,
                    onSave = viewModel::onSave
                )
            }

            item {
                Text(
                    text = stringResource(R.string.incomes_recent),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.incomes.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        text = stringResource(R.string.incomes_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.incomes, key = { it.id }) { income ->
                    IncomeRow(
                        income = income,
                        onDeleteClick = { viewModel.onDeleteClick(income) }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    state.deleteTarget?.let { income ->
        DeleteIncomeDialog(
            income = income,
            onConfirm = viewModel::onDeleteConfirm,
            onDismiss = viewModel::onDeleteDismiss
        )
    }
}

@Composable
private fun IncomeFormCard(
    state: IncomeUiState,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onWalletSelect: (Long?) -> Unit,
    onSave: () -> Unit
) {
    val form = state.form

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.income_title),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.size(12.dp))

            OutlinedTextField(
                value = form.amountInput,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.income_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = form.amountErrorRes != null,
                supportingText = form.amountErrorRes?.let { { Text(stringResource(it)) } }
            )

            Spacer(modifier = Modifier.size(8.dp))

            WalletDropdown(
                selectedId = form.walletId,
                wallets = state.wallets,
                errorRes = form.walletErrorRes,
                onSelect = onWalletSelect
            )

            Spacer(modifier = Modifier.size(8.dp))

            OutlinedTextField(
                value = form.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.income_description)) },
                singleLine = true
            )

            Spacer(modifier = Modifier.size(16.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.income_save))
            }
        }
    }
}

@Composable
private fun WalletDropdown(
    selectedId: Long?,
    wallets: List<WalletEntity>,
    @StringRes errorRes: Int?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = wallets.firstOrNull { it.id == selectedId }

    Column {
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.income_wallet),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = selected?.let { "${it.name} · ${walletTypeLabel(it.type)}" }
                            ?: stringResource(R.string.income_wallet_none)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.income_wallet)
                )
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                wallets.forEach { wallet ->
                    DropdownMenuItem(
                        text = { Text("${wallet.name} · ${walletTypeLabel(wallet.type)}") },
                        onClick = {
                            onSelect(wallet.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        errorRes?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun IncomeRow(
    income: IncomeUi,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = income.description.ifBlank { stringResource(R.string.income_no_description) },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = listOfNotNull(
                        income.walletName,
                        DateFormatter.formatDate(income.date)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "+${MoneyFormatter.format(income.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
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
private fun DeleteIncomeDialog(
    income: IncomeUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.income_delete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.income_delete_message,
                    MoneyFormatter.format(income.amount)
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
@Composable
private fun walletTypeLabel(type: String): String {
    val walletType = WalletType.entries.firstOrNull { it.name == type } ?: WalletType.CARD
    return stringResource(
        when (walletType) {
            WalletType.CARD -> R.string.wallet_card
            WalletType.CASH -> R.string.wallet_cash
        }
    )
}