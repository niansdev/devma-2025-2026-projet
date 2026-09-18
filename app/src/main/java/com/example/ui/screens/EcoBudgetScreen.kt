package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Category
import com.example.model.Transaction
import com.example.viewmodel.EcoBudgetUiState
import com.example.viewmodel.EcoBudgetViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoBudgetScreen(
    viewModel: EcoBudgetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "EcoBudget",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Ajouter une transaction")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Barres de navigation des mois
            MonthSelectorSection(
                displayLabel = uiState.currentMonth.displayLabel,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onCurrentMonth = { viewModel.goToCurrentMonth() }
            )

            // 2. Carte de résumé du budget
            BudgetSummaryCard(uiState = uiState)

            // 3. Barre de filtres par catégories
            CategoryFilterBar(
                selectedCategories = uiState.selectedCategories,
                onCategoryToggle = { category -> viewModel.toggleCategory(category) },
                onClearFilter = { viewModel.clearCategoryFilter() }
            )

            // 4. Liste des transactions
            TransactionListSection(
                transactions = uiState.filteredTransactions,
                onEditTransaction = { viewModel.openEditDialog(it) },
                onDeleteTransaction = { viewModel.deleteTransaction(it.id) }
            )
        }

        // Dialogue d'enregistrement / d'édition
        if (uiState.isAddDialogOpen) {
            AddEditTransactionDialog(
                editingTransaction = uiState.editingTransaction,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { title, amount, category ->
                    viewModel.saveTransaction(title, amount, category)
                }
            )
        }
    }
}

@Composable
private fun MonthSelectorSection(
    displayLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Mois précédent")
            }

            Text(
                text = displayLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCurrentMonth() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            IconButton(onClick = onNextMonth) {
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Mois suivant")
            }
        }
    }
}

@Composable
private fun BudgetSummaryCard(uiState: EcoBudgetUiState) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.FRANCE) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Budget Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormatter.format(uiState.monthlyBudget),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Reste à dépenser",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormatter.format(uiState.remainingBudget),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.remainingBudget > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { uiState.budgetUsageRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (uiState.budgetUsageRatio >= 1.0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dépensé: ${currencyFormatter.format(uiState.totalSpent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${uiState.budgetUsagePercentage}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bandeau informatif si des filtres par catégorie sont appliqués
            AnimatedVisibility(visible = !uiState.isAllCategoriesSelected) {
                val categoryNames = uiState.selectedCategories.joinToString(", ") { "${it.emoji} ${it.displayName}" }
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtre ($categoryNames):",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currencyFormatter.format(uiState.categorySpent),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterBar(
    selectedCategories: Set<Category>,
    onCategoryToggle: (Category) -> Unit,
    onClearFilter: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategories.isEmpty(),
                onClick = onClearFilter,
                label = { Text("Tous") },
                leadingIcon = if (selectedCategories.isEmpty()) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        items(Category.entries.toTypedArray()) { category ->
            val isSelected = selectedCategories.contains(category)
            FilterChip(
                selected = isSelected,
                onClick = { onCategoryToggle(category) },
                label = { Text("${category.emoji} ${category.displayName}") },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun TransactionListSection(
    transactions: List<Transaction>,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aucune transaction pour ce filtre.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(transactions, key = { it.id }) { transaction ->
                TransactionItemRow(
                    transaction = transaction,
                    onEdit = { onEditTransaction(transaction) },
                    onDelete = { onDeleteTransaction(transaction) }
                )
            }
        }
    }
}

@Composable
private fun TransactionItemRow(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.FRANCE) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.FRANCE) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône de la catégorie
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = transaction.category.emoji, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Détails du titre et de la date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.category.displayName} • ${transaction.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Montant
            Text(
                text = currencyFormatter.format(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Actions Édition & Suppression
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Modifier",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditTransactionDialog(
    editingTransaction: Transaction?,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: Category) -> Unit
) {
    var title by remember { mutableStateOf(editingTransaction?.title ?: "") }
    var amountText by remember { mutableStateOf(editingTransaction?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(editingTransaction?.category ?: Category.FOOD) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val isEditing = editingTransaction != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Modifier la transaction" else "Nouvelle transaction")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre / Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Montant") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Menu déroulant des catégories
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = "${selectedCategory.emoji} ${selectedCategory.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        Category.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.emoji} ${category.displayName}") },
                                onClick = {
                                    selectedCategory = category
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0.0) {
                        onSave(title, amount, selectedCategory)
                    }
                }
            ) {
                Text(if (isEditing) "Mettre à jour" else "Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}