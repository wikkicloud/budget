package com.budget.app.ui.expenses

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budget.app.R
import com.budget.app.data.local.entity.CategoryEntity
import com.budget.app.data.local.entity.WalletEntity
import com.budget.app.domain.model.WalletType
import com.budget.app.util.CategoryIconOptions
import com.budget.app.util.categoryIconVector
import com.budget.app.util.ColorFormatter
import com.budget.app.util.DateFormatter
import com.budget.app.util.MoneyFormatter

/** Цвет иконки по умолчанию, если у категории цвет не задан или не разобран. */
private val DefaultCategoryIconColor = Color(0xFF9C7CFF)

private fun CategoryEntity.iconVector(): ImageVector = categoryIconVector(iconKey ?: icon)

/** Цвет иконки категории: цвет по умолчанию для ключа, иначе сохранённый color. */
private fun CategoryEntity.iconColor(): Color {
    CategoryIconOptions.colorFor(iconKey)?.let { return ColorFormatter.parse(it, DefaultCategoryIconColor) }
    return ColorFormatter.parse(color, DefaultCategoryIconColor)
}

/** Кружок с цветной иконкой категории. */
@Composable
private fun CategoryIconBadge(
    vector: ImageVector,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vector,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}


@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel = hiltViewModel()
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

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.screen_expenses),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ExpenseFormCard(
                        state = state,
                        onAmountChange = viewModel::onAmountChange,
                        onDescriptionChange = viewModel::onDescriptionChange,
                        onCategorySelect = viewModel::onCategorySelect,
                        onAddCategoryClick = viewModel::onAddCategoryClick,
                        onWalletSelect = viewModel::onWalletSelect,
                        onSave = viewModel::onSave,
                        onCancelEdit = viewModel::onCancelEdit
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.expenses_recent),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (state.expenses.isEmpty() && !state.isLoading) {
                    item {
                        Text(
                            text = stringResource(R.string.expenses_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(state.expenses, key = { it.id }) { expense ->
                        ExpenseRow(
                            expense = expense,
                            onEditClick = { viewModel.onEditClick(expense) },
                            onDeleteClick = { viewModel.onDeleteClick(expense) }
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    state.deleteTarget?.let { expense ->
        DeleteExpenseDialog(
            expense = expense,
            onConfirm = viewModel::onDeleteConfirm,
            onDismiss = viewModel::onDeleteDismiss
        )
    }

    if (state.isCategoryDialogVisible) {
        NewCategoryDialog(
            state = state,
            onNameChange = viewModel::onNewCategoryNameChange,
            onIconSelect = viewModel::onNewCategoryIconSelect,
            onSave = viewModel::onNewCategorySave,
            onDismiss = viewModel::onCategoryDialogDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormCard(
    state: ExpensesUiState,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    onAddCategoryClick: () -> Unit,
    onWalletSelect: (Long?) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit
) {
    val form = state.form

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    if (form.isEditing) R.string.expense_edit_title else R.string.expense_title
                ),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.size(12.dp))

            OutlinedTextField(
                value = form.amountInput,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.expense_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = form.amountErrorRes != null,
                supportingText = form.amountErrorRes?.let { { Text(stringResource(it)) } }
            )

            Spacer(modifier = Modifier.size(8.dp))

            OutlinedTextField(
                value = form.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.expense_description)) },
                singleLine = true
            )

            Spacer(modifier = Modifier.size(8.dp))

            CategoryDropdown(
                selectedId = form.categoryId,
                categories = state.categories,
                onSelect = onCategorySelect,
                onAddCategoryClick = onAddCategoryClick
            )

            Spacer(modifier = Modifier.size(8.dp))

            WalletDropdown(
                selectedId = form.walletId,
                wallets = state.wallets,
                errorRes = form.walletErrorRes,
                onSelect = onWalletSelect
            )

            Spacer(modifier = Modifier.size(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.expense_save))
                }

                if (form.isEditing) {
                    OutlinedButton(onClick = onCancelEdit) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedId: Long?,
    categories: List<CategoryEntity>,
    onSelect: (Long?) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            label = { Text(stringResource(R.string.expense_category)) },
            placeholder = { Text(stringResource(R.string.expense_category_none)) },
            leadingIcon = selected?.let { category ->
                {
                    CategoryIconBadge(
                        vector = category.iconVector(),
                        color = category.iconColor(),
                        size = 32.dp
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.expense_category_none)) },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    leadingIcon = {
                        CategoryIconBadge(
                            vector = category.iconVector(),
                            color = category.iconColor(),
                            size = 32.dp
                        )
                    },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.category_add)) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    onAddCategoryClick()
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun NewCategoryDialog(
    state: ExpensesUiState,
    onNameChange: (String) -> Unit,
    onIconSelect: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var categoryName by remember(state.isCategoryDialogVisible) {
        mutableStateOf("")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {
                        categoryName = it
                        onNameChange(it)
                    },
                    label = { Text(stringResource(R.string.wallet_name)) },
                    singleLine = true,
                    isError = state.newCategory.errorRes != null,
                    supportingText = state.newCategory.errorRes?.let { { Text(stringResource(it)) } }
                )
                Text(stringResource(R.string.category_icon), style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconOptions.options.forEach { (key, colorHex) ->
                        val selected = state.newCategory.iconKey == key
                        val color = ColorFormatter.parse(colorHex, DefaultCategoryIconColor)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (selected) color.copy(alpha = 0.25f)
                                    else Color.Transparent,
                                    CircleShape
                                )
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) color else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onIconSelect(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIconVector(key),
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(24.dp)
                            )
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(14.dp)
                                        .background(color, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
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
                        text = stringResource(R.string.expense_wallet),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = selected?.let { "${it.name} · ${walletTypeLabel(it.type)}" }
                            ?: stringResource(R.string.expense_wallet_none)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.expense_wallet)
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
private fun ExpenseRow(
    expense: ExpenseUi,
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
            CategoryIconBadge(
                vector = categoryIconVector(expense.categoryIconKey),
                color = ColorFormatter.parse(expense.categoryColor, DefaultCategoryIconColor),
                size = 40.dp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifBlank { stringResource(R.string.expense_no_description) },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = listOfNotNull(
                        expense.categoryName,
                        expense.walletName,
                        DateFormatter.formatDate(expense.date)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = MoneyFormatter.format(expense.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 4.dp)
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
private fun DeleteExpenseDialog(
    expense: ExpenseUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.expense_delete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.expense_delete_message,
                    MoneyFormatter.format(expense.amount)
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