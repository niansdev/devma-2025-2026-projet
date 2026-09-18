package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FakeTransactionRepository
import com.example.data.repository.TransactionRepository
import com.example.model.Category
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.model.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class EcoBudgetUiState(
    val currentMonth: YearMonth = YearMonth.current(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val monthTransactions: List<Transaction> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val selectedCategories: Set<Category> = emptySet(),
    val monthlyBudget: Double = 500000.0,
    val totalSpent: Double = 0.0,
    val categorySpent: Double = 0.0,
    val remainingBudget: Double = 500000.0,
    val isAddDialogOpen: Boolean = false,
    val editingTransaction: Transaction? = null
) {
    val isAllCategoriesSelected: Boolean
        get() = selectedCategories.isEmpty() || selectedCategories.size == Category.entries.size

    val budgetUsageRatio: Float
        get() = if (monthlyBudget > 0) (totalSpent / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f

    val budgetUsagePercentage: Int
        get() = if (monthlyBudget > 0) ((totalSpent / monthlyBudget) * 100).toInt() else 0
}

class EcoBudgetViewModel(
    private val repository: TransactionRepository = FakeTransactionRepository()
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.current())
    private val _selectedCategories = MutableStateFlow<Set<Category>>(emptySet())
    private val _isAddDialogOpen = MutableStateFlow(false)
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)
    private val _monthlyBudget = MutableStateFlow(500000.0)

    private val _monthAndCategoriesFlow = combine(
        _currentMonth,
        _selectedCategories,
        _monthlyBudget
    ) { currentMonth, selectedCategories, monthlyBudget ->
        Triple(currentMonth, selectedCategories, monthlyBudget)
    }

    private val _dialogStateFlow = combine(
        _isAddDialogOpen,
        _editingTransaction
    ) { isAddDialogOpen, editingTransaction ->
        isAddDialogOpen to editingTransaction
    }

    val uiState: StateFlow<EcoBudgetUiState> = combine(
        repository.getTransactions(),
        _monthAndCategoriesFlow,
        _dialogStateFlow
    ) { transactions: List<Transaction>, monthAndCats: Triple<YearMonth, Set<Category>, Double>, dialogs: Pair<Boolean, Transaction?> ->
        val currentMonth = monthAndCats.first
        val selectedCategories = monthAndCats.second
        val monthlyBudget = monthAndCats.third
        val isAddDialogOpen = dialogs.first
        val editingTransaction = dialogs.second

        val monthTxs = transactions.filter { currentMonth.containsTimestamp(it.dateMillis) }

        val filtered = if (selectedCategories.isEmpty() || selectedCategories.size == Category.entries.size) {
            monthTxs
        } else {
            monthTxs.filter { it.category in selectedCategories }
        }

        val total = monthTxs.sumOf { it.amount }
        val catSpent = filtered.sumOf { it.amount }
        val remaining = (monthlyBudget - total).coerceAtLeast(0.0)

        EcoBudgetUiState(
            currentMonth = currentMonth,
            filteredTransactions = filtered,
            monthTransactions = monthTxs,
            allTransactions = transactions,
            selectedCategories = selectedCategories,
            monthlyBudget = monthlyBudget,
            totalSpent = total,
            categorySpent = catSpent,
            remainingBudget = remaining,
            isAddDialogOpen = isAddDialogOpen,
            editingTransaction = editingTransaction
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = EcoBudgetUiState()
    )

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.previous()
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.next()
    }

    fun goToCurrentMonth() {
        _currentMonth.value = YearMonth.current()
    }

    fun toggleCategory(category: Category) {
        val currentSet = _selectedCategories.value
        val newSet = if (currentSet.contains(category)) {
            currentSet - category
        } else {
            currentSet + category
        }
        _selectedCategories.value = newSet
    }

    fun clearCategoryFilter() {
        _selectedCategories.value = emptySet()
    }

    fun openAddDialog() {
        _editingTransaction.value = null
        _isAddDialogOpen.value = true
    }

    fun openEditDialog(transaction: Transaction) {
        _editingTransaction.value = transaction
        _isAddDialogOpen.value = true
    }

    fun dismissDialog() {
        _isAddDialogOpen.value = false
        _editingTransaction.value = null
    }

    fun saveTransaction(title: String, amount: Double, category: Category, type: TransactionType = TransactionType.EXPENSE) {
        if (title.isBlank() || amount <= 0.0) return

        val currentEditing = _editingTransaction.value

        viewModelScope.launch {
            if (currentEditing != null) {
                val updated = currentEditing.copy(
                    title = title.trim(),
                    amount = amount,
                    category = category,
                    type = type
                )
                repository.updateTransaction(updated)
            } else {
                val currentYearMonth = _currentMonth.value
                val dateToUse = if (currentYearMonth == YearMonth.current()) {
                    System.currentTimeMillis()
                } else {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.YEAR, currentYearMonth.year)
                    cal.set(Calendar.MONTH, currentYearMonth.month - 1)
                    cal.set(Calendar.DAY_OF_MONTH, 15)
                    cal.set(Calendar.HOUR_OF_DAY, 12)
                    cal.timeInMillis
                }

                // ✅ Correction : date -> dateMillis + ajout de TransactionType
                val newTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    amount = amount,
                    type = type,
                    category = category,
                    dateMillis = dateToUse
                )
                repository.addTransaction(newTransaction)
            }
            dismissDialog()
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }
}