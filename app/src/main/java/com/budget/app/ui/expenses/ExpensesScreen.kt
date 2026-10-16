package com.budget.app.ui.expenses

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import com.budget.app.data.local.entity.CategoryEntity
import com.budget.app.data.local.entity.WalletEntity
import com.budget.app.domain.model.WalletType
import com.budget.app.util.DateFormatter
import com.budget.app.util.MoneyFormatter

private fun CategoryEntity.iconText(): String = iconKey ?: icon

private fun categoryIcon(key: String) = when (key) {
    "restaurant" -> Icons.Filled.Restaurant
    "local_cafe" -> Icons.Filled.LocalCafe
    "shopping_cart" -> Icons.Filled.ShoppingCart
    "directions_car" -> Icons.Filled.DirectionsCar
    "home" -> Icons.Filled.Home
    "favorite" -> Icons.Filled.Favorite
    "health_and_safety" -> Icons.Filled.HealthAndSafety
    "sports_esports" -> Icons.Filled.SportsEsports
    "movie" -> Icons.Filled.Movie
    "flight" -> Icons.Filled.Flight
    "school" -> Icons.Filled.School
    "work" -> Icons.Filled.Work
    "pets" -> Icons.Filled.Pets
    "local_gas_station" -> Icons.Filled.LocalGasStation
    "build" -> Icons.Filled.Build
    "phone" -> Icons.Filled.Phone
    "wifi" -> Icons.Filled.Wifi
    "fitness_center" -> Icons.Filled.FitnessCenter
    "local_hospital" -> Icons.Filled.LocalHospital
    "local_grocery_store" -> Icons.Filled.LocalGroceryStore
    "child_friendly" -> Icons.Filled.ChildFriendly
    "music_note" -> Icons.Filled.MusicNote
    "book" -> Icons.Filled.Book
    "camera_alt" -> Icons.Filled.CameraAlt
    "cake" -> Icons.Filled.Cake
    "celebration" -> Icons.Filled.Celebration
    "payments" -> Icons.Filled.Payments
    "savings" -> Icons.Filled.Savings
    else -> Icons.Filled.MoreHoriz
}

private fun CategoryEntity.iconVector() = categoryIcon(iconKey ?: "more_horiz")

private val categoryIconKeys = CategoryIconOptions.keys

@Composable
private fun CategoryIcon(key: String) {
    Icon(categoryIcon(key), contentDescription = null)
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
    val label = selected?.let { "${it.iconText()} ${it.name}" }
        ?: stringResource(R.string.expense_category_none)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            label = { Text(stringResource(R.string.expense_category)) },
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
                    text = { Text("${category.iconText()} ${category.name}") },
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CategoryIconOptions.keys.chunked(6).forEach { rowKeys ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowKeys.forEach { key ->
                                IconButton(onClick = { onIconSelect(key) }) {
                                    CategoryIcon(key)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifBlank { stringResource(R.string.expense_no_description) },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = listOfNotNull(
                        expense.categoryLabel,
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