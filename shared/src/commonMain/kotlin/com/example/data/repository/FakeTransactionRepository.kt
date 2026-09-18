package com.example.data.repository

import com.example.model.Category
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.model.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class FakeTransactionRepository : TransactionRepository {

    private val _transactions = MutableStateFlow<List<Transaction>>(
        listOf(
            Transaction(
                id = "1",
                title = "Achats Supermarché",
                amount = 45000.0,
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                dateMillis = System.currentTimeMillis()
            ),
            Transaction(
                id = "2",
                title = "Transport Mensuel",
                amount = 25000.0,
                type = TransactionType.EXPENSE,
                category = Category.TRANSPORT,
                dateMillis = System.currentTimeMillis()
            )
        )
    )

    override fun getTransactions(): Flow<List<Transaction>> {
        return _transactions.asStateFlow()
    }

    override fun getTransactionsForMonth(yearMonth: YearMonth): Flow<List<Transaction>> {
        return _transactions.map { list ->
            list.filter { transaction ->
                val cal = Calendar.getInstance().apply { timeInMillis = transaction.dateMillis }
                val txYear = cal.get(Calendar.YEAR)
                val txMonth = cal.get(Calendar.MONTH) + 1 // Calendar.MONTH commence à 0
                txYear == yearMonth.year && txMonth == yearMonth.month
            }
        }
    }

    override suspend fun addTransaction(transaction: Transaction) {
        _transactions.value = _transactions.value + transaction
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        _transactions.value = _transactions.value.map {
            if (it.id == transaction.id) transaction else it
        }
    }

    override suspend fun deleteTransaction(id: String) {
        _transactions.value = _transactions.value.filterNot { it.id == id }
    }
}