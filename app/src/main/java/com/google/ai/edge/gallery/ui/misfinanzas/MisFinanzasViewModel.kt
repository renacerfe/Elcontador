package com.google.ai.edge.gallery.ui.misfinanzas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.ResumenTarjeta
import com.google.ai.edge.gallery.data.ResumenTarjetaDao
import com.google.ai.edge.gallery.data.Transaction
import com.google.ai.edge.gallery.data.TransactionCategory
import com.google.ai.edge.gallery.data.TransactionDao
import com.google.ai.edge.gallery.data.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinanzasUiState(
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val resumenes: List<ResumenTarjeta> = emptyList()
)

@HiltViewModel
class MisFinanzasViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val resumenTarjetaDao: ResumenTarjetaDao
) : ViewModel() {

    val uiState: StateFlow<FinanzasUiState> = transactionDao.getAllTransactions()
        .map { transactions ->
            val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            
            // Note: We'll combine this with resumenes later if needed, but for now let's just use a simple approach
            FinanzasUiState(
                transactions = transactions,
                totalIncome = income,
                totalExpenses = expenses,
                balance = income - expenses
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FinanzasUiState()
        )

    val resumenes: StateFlow<List<ResumenTarjeta>> = resumenTarjetaDao.getAllResumenes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTransaction(description: String, amount: Double, type: TransactionType, category: TransactionCategory) {
        viewModelScope.launch {
            transactionDao.insertTransaction(
                Transaction(description = description, amount = amount, type = type, category = category)
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.deleteTransaction(transaction)
        }
    }

    fun saveResumen(fecha: String, total: Double, vencimiento: String, texto: String) {
        viewModelScope.launch {
            resumenTarjetaDao.insertResumen(
                ResumenTarjeta(
                    fechaResumen = fecha,
                    totalAPagar = total,
                    fechaVencimiento = vencimiento,
                    textoCompleto = texto
                )
            )
        }
    }
}
