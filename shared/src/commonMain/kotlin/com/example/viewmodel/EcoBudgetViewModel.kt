package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FakeTransactionRepository
import com.example.data.repository.TransactionRepository
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.model.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EcoBudgetViewModel(
    private val repository: TransactionRepository = FakeTransactionRepository()
) : ViewModel() {

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    private val _isAddDialogOpen = MutableStateFlow(false)
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _transactions = _selectedYearMonth.flatMapLatest { yearMonth ->
        repository.getTransactionsForMonth(yearMonth)
    }

    val uiState: StateFlow<EcoBudgetUiState> = combine(
        _selectedYearMonth,
        _transactions,
        _isAddDialogOpen,
        _editingTransaction
    ) { yearMonth, transactions, isAddDialogOpen, editingTransaction ->
        val income = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val expense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        EcoBudgetUiState(
            selectedYearMonth = yearMonth,
            transactions = transactions,
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense,
            isLoading = false,
            isAddDialogOpen = isAddDialogOpen,
            editingTransaction = editingTransaction
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EcoBudgetUiState()
    )

    // --- Navigation par mois ---
    fun onPreviousMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.previous()
    }
    fun previousMonth() = onPreviousMonth() // Alias pour compatibilité UI

    fun onNextMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.next()
    }
    fun nextMonth() = onNextMonth() // Alias pour compatibilité UI

    // --- Gestion du Dialogue d'ajout / édition ---
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

    // --- Opérations CRUD ---
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }
}