package com.budget.app.ui.savings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budget.app.R
import com.budget.app.domain.model.SavingsOperationType
import com.budget.app.util.DateFormatter
import com.budget.app.util.MoneyFormatter

/** Экран «Копилки»: копилки с прогрессом по цели и операции по ним. */
@Composable
fun SavingsScreen(
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    val messageText = when (state.message) {
        null -> null
        Message.SavingsCreated -> resources.getString(R.string.savings_created)
        Message.SavingsUpdated -> resources.getString(R.string.savings_updated)
        Message.SavingsDeleted -> resources.getString(R.string.savings_deleted)
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
                    contentDescription = stringResource(R.string.savings_add)
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
                text = stringResource(R.string.screen_savings),
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

            if (state.savings.isEmpty() && !state.isLoading) {
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
                    items(state.savings, key = { it.id }) { savings ->
                        SavingsCard(
                            savings = savings,
                            expanded = state.expandedId == savings.id,
                            onToggle = { viewModel.onSavingsToggle(savings) },
                            onDepositClick = { viewModel.onDepositClick(savings) },
                            onWithdrawClick = { viewModel.onWithdrawClick(savings) },
                            onInterestClick = { viewModel.onInterestClick(savings) },
                            onEditClick = { viewModel.onEditClick(savings) },
                            onDeleteClick = { viewModel.onDeleteClick(savings) }
                        )
                    }

                    item {
                        Text(
                            text = stringResource(R.string.savings_operations_recent),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (state.operations.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.savings_operations_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(state.operations, key = { "operation-${it.id}" }) { operation ->
                            OperationRow(
                                operation = operation,
                                onDeleteClick = { viewModel.onDeleteOperationClick(operation) }
                            )
                        }
                    }
                }
            }
        }
    }

    state.form?.let { form ->
        SavingsFormDialog(
            form = form,
            onNameChange = viewModel::onFormNameChange,
            onTargetChange = viewModel::onFormTargetChange,
            onInitialChange = viewModel::onFormInitialChange,
            onSave = viewModel::onFormSave,
            onDismiss = viewModel::onFormDismiss
        )
    }

    state.action?.let { action ->
        SavingsActionDialog(
            action = action,
            wallets = state.wallets,
            onWalletSelect = viewModel::onActionWalletSelect,
            onAmountChange = viewModel::onActionAmountChange,
            onSave = viewModel::onActionSave,
            onDismiss = viewModel::onActionDismiss
        )
    }

    state.deleteTarget?.let { savings ->
        DeleteSavingsDialog(
            savings = savings,
            operationCount = state.deleteTargetOperationCount,
            onConfirm = viewModel::onDeleteConfirm,
            onDismiss = viewModel::onDeleteDismiss
        )
    }

    state.deleteOperationTarget?.let { operation ->
        DeleteOperationDialog(
            operation = operation,
            onConfirm = viewModel::onDeleteOperationConfirm,
            onDismiss = viewModel::onDeleteOperationDismiss
        )
    }
}

@Composable
private fun TotalCard(total: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.savings_total),
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

/**
 * Копилка: компактная строка как на экране «Кошельки» (иконка, название, цель, баланс),
 * тап раскрывает детали и кнопки действий — дата, стартовая сумма, прогресс,
 * «Пополнить» / «Вывести» / «Проценты» / «Редактировать» / «Удалить».
 */
@Composable
private fun SavingsCard(
    savings: SavingsUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onInterestClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = savings.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        // Цель одной строкой — как тип кошелька на «Кошельках»
                        text = savings.targetAmount?.let {
                            stringResource(R.string.savings_goal, MoneyFormatter.format(it))
                        } ?: stringResource(R.string.savings_no_goal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = MoneyFormatter.format(savings.balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.ExpandLess
                    } else {
                        Icons.Filled.ExpandMore
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                SavingsDetails(
                    savings = savings,
                    onDepositClick = onDepositClick,
                    onWithdrawClick = onWithdrawClick,
                    onInterestClick = onInterestClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

/** Раскрытая часть копилки: дата, стартовая сумма, прогресс по цели и все действия. */
@Composable
private fun SavingsDetails(
    savings: SavingsUi,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onInterestClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
        Text(
            text = stringResource(
                R.string.savings_created_at,
                DateFormatter.formatDate(savings.createdAt)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Стартовая сумма показывается, только если она есть — у пустых копилок строки нет
        if (savings.initialAmount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.savings_initial_amount,
                    MoneyFormatter.format(savings.initialAmount)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val percent = savings.progressPercent
        val target = savings.targetAmount

        if (target != null && percent != null) {
            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { savings.progress ?: 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.savings_goal, MoneyFormatter.format(target)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.savings_progress, percent),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Три кнопки в строку не поместились бы на узком экране — переносятся
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                icon = Icons.Filled.ArrowCircleUp,
                labelRes = R.string.savings_deposit,
                contentDescriptionRes = R.string.savings_deposit,
                onClick = onDepositClick
            )
            ActionButton(
                icon = Icons.Filled.ArrowCircleDown,
                labelRes = R.string.savings_withdraw,
                contentDescriptionRes = R.string.savings_withdraw,
                onClick = onWithdrawClick
            )
            ActionButton(
                icon = Icons.Filled.Percent,
                // Короткая подпись: полная формулировка не влезает в кнопку
                labelRes = R.string.savings_interest_short,
                contentDescriptionRes = R.string.savings_interest,
                onClick = onInterestClick
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.savings_edit))
            }

            TextButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    @StringRes labelRes: Int,
    @StringRes contentDescriptionRes: Int,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(contentDescriptionRes),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

/** Строка операции по копилке: тип, кошелёк, дата и сумма. */
@Composable
private fun OperationRow(
    operation: SavingsOperationUi,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noWallet = stringResource(R.string.savings_operation_no_wallet)

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (operation.type) {
                    SavingsOperationType.DEPOSIT -> Icons.Filled.ArrowCircleUp
                    SavingsOperationType.WITHDRAW -> Icons.Filled.ArrowCircleDown
                    SavingsOperationType.INTEREST -> Icons.Filled.Percent
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = operation.savingsName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    // Тип операции, кошелёк и дата: «Пополнение · Карта · 19.10.2026»
                    text = listOf(
                        operation.typeLabel(),
                        operation.walletName ?: noWallet,
                        DateFormatter.formatDate(operation.date)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = when (operation.type) {
                    SavingsOperationType.WITHDRAW -> "−${MoneyFormatter.format(operation.amount)}"
                    else -> "+${MoneyFormatter.format(operation.amount)}"
                },
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
            text = stringResource(R.string.savings_empty),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.savings_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Диалог создания и редактирования копилки: название, необязательная цель
 * и необязательная стартовая сумма. Дата создания ставится при создании и при правке не меняется.
 */
@Composable
private fun SavingsFormDialog(
    form: SavingsFormState,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onInitialChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (form.isEditing) R.string.savings_edit_title else R.string.savings_new_title
                )
            )
        },
        text = {
            // Три поля с подсказками не влезают в диалог на узком экране — прокручиваем
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.savings_name)) },
                    singleLine = true,
                    isError = form.nameErrorRes != null,
                    supportingText = form.nameErrorRes?.let { { Text(stringResource(it)) } }
                )

                Spacer(modifier = Modifier.size(16.dp))

                // Стартовая сумма: деньги, уже лежащие в копилке; кошельки не меняются
                GroupedAmountField(
                    value = form.initialInput,
                    onValueChange = onInitialChange,
                    labelRes = R.string.savings_initial,
                    errorRes = form.initialErrorRes,
                    hintRes = R.string.savings_initial_hint
                )

                Spacer(modifier = Modifier.size(16.dp))

                // Цель — сумма: разряды уже разделены в состоянии формы, пробелы разбирает parseToKopecks
                GroupedAmountField(
                    value = form.targetInput,
                    onValueChange = onTargetChange,
                    labelRes = R.string.savings_target,
                    errorRes = form.targetErrorRes
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

/**
 * Поле суммы, разделяющее разряды прямо при наборе («1000» → «1 000»).
 * Значение держится как `TextFieldValue`, а позиция курсора пересчитывается после группировки:
 * когда текст удлиняется разделителем, обычный `TextField` оставил бы курсор на символ левее
 * конца, и следующие цифры попадали бы в середину числа.
 */
@Composable
private fun GroupedAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    @StringRes errorRes: Int?,
    @StringRes hintRes: Int? = null
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }

    // Значение из состояния формы (например, предзаполнение при редактировании) важнее
    // локального — синхронизируем поле и ставим курсор в конец
    if (fieldValue.text != value) {
        fieldValue = TextFieldValue(value, TextRange(value.length))
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { newValue ->
            val grouped = MoneyFormatter.groupAmountInput(newValue.text)
            val cursor = MoneyFormatter.mapCursorAfterGrouping(
                rawInput = newValue.text,
                rawCursor = newValue.selection.end,
                groupedInput = grouped
            )
            fieldValue = TextFieldValue(grouped, TextRange(cursor))
            onValueChange(grouped)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(labelRes)) },
        placeholder = { Text(stringResource(R.string.savings_target_optional)) },
        // Валюта, как в остальных суммах: цифры вводит пользователь
        suffix = { Text(MoneyFormatter.CURRENCY) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = errorRes != null,
        supportingText = when {
            errorRes != null -> {
                { Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error) }
            }

            hintRes != null -> {
                { Text(stringResource(hintRes), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            else -> null
        }
    )
}

/** Диалог пополнения, вывода или начисления процентов. */
@Composable
private fun SavingsActionDialog(
    action: SavingsActionState,
    wallets: List<SavingsWalletUi>,
    onWalletSelect: (Long) -> Unit,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val selected = wallets.firstOrNull { it.id == action.walletId }
    val needsWallet = action.kind != SavingsActionKind.INTEREST

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    when (action.kind) {
                        SavingsActionKind.DEPOSIT -> R.string.savings_deposit_title
                        SavingsActionKind.WITHDRAW -> R.string.savings_withdraw_title
                        SavingsActionKind.INTEREST -> R.string.savings_interest_title
                    }
                )
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.savings_balance,
                        MoneyFormatter.format(action.balance)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.size(12.dp))

                if (needsWallet) {
                    WalletPicker(
                        labelRes = if (action.kind == SavingsActionKind.DEPOSIT) {
                            R.string.savings_wallet_from
                        } else {
                            R.string.savings_wallet_to
                        },
                        selectedName = selected?.let {
                            "${it.name} · ${MoneyFormatter.format(it.balance)}"
                        },
                        wallets = wallets,
                        onSelect = onWalletSelect
                    )

                    action.walletErrorRes?.let {
                        Text(
                            text = stringResource(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        text = stringResource(R.string.savings_interest_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                }

                OutlinedTextField(
                    value = action.amountInput,
                    onValueChange = onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.savings_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = action.amountErrorRes != null,
                    supportingText = action.amountErrorRes?.let { { Text(stringResource(it)) } }
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

/** Выбор кошелька кнопкой с выпадающим списком — как в переводе между кошельками. */
@Composable
private fun WalletPicker(
    @StringRes labelRes: Int,
    selectedName: String?,
    wallets: List<SavingsWalletUi>,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                Text(text = selectedName ?: stringResource(R.string.transfer_none))
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

@Composable
private fun DeleteSavingsDialog(
    savings: SavingsUi,
    operationCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.savings_delete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.savings_delete_message,
                    savings.name,
                    operationCount
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

@Composable
private fun DeleteOperationDialog(
    operation: SavingsOperationUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.savings_operation_delete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.savings_operation_delete_message,
                    MoneyFormatter.format(operation.amount),
                    operation.savingsName
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

/** Подпись типа операции по копилке. */
@Composable
private fun SavingsOperationUi.typeLabel(): String = when (type) {
    SavingsOperationType.DEPOSIT -> stringResource(R.string.savings_operation_deposit)
    SavingsOperationType.WITHDRAW -> stringResource(R.string.savings_operation_withdraw)
    SavingsOperationType.INTEREST -> stringResource(R.string.savings_operation_interest)
}