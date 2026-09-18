package com.example.viewmodel

import com.example.model.Transaction
import com.example.model.YearMonth

data class EcoBudgetUiState(
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val isLoading: Boolean = true,
    // ✅ Ajoutez ces deux propriétés :
    val isAddDialogOpen: Boolean = false,
    val editingTransaction: Transaction? = null
)