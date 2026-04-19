package com.google.ai.edge.gallery.ui.misfinanzas

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ResumenTarjetaDao
import com.google.ai.edge.gallery.data.TransactionCategory
import com.google.ai.edge.gallery.data.TransactionDao
import com.google.ai.edge.gallery.data.TransactionType
import com.google.ai.edge.gallery.ui.llmchat.LlmChatViewModelBase
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MisFinanzasChatViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val resumenTarjetaDao: ResumenTarjetaDao
) : LlmChatViewModelBase() {

    fun generateFinanceResponse(
        context: Context,
        model: Model,
        input: String,
        modelManagerViewModel: ModelManagerViewModel
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val transactions = transactionDao.getAllTransactions().first()
            val resumenes = resumenTarjetaDao.getAllResumenes().first()
            
            val incomeList = transactions.filter { it.type == TransactionType.INCOME }
            val expenseList = transactions.filter { it.type == TransactionType.EXPENSE }
            
            val totalIncome = incomeList.sumOf { it.amount }
            val totalExpenses = expenseList.sumOf { it.amount }
            val balance = totalIncome - totalExpenses

            val incomeText = incomeList.joinToString("\n") { 
                "- ${it.description} (${it.category.displayName}): $${it.amount}"
            }.ifEmpty { "No hay ingresos registrados." }

            val expenseText = expenseList.joinToString("\n") { 
                "- ${it.description} (${it.category.displayName}): $${it.amount}"
            }.ifEmpty { "No hay gastos registrados." }

            val resumenesText = resumenes.joinToString("\n\n") { 
                "Resumen ${it.fechaResumen}:\nTotal: $${it.totalAPagar}\nVencimiento: ${it.fechaVencimiento}\nContenido extraído:\n${it.textoCompleto}"
            }.ifEmpty { "No hay resúmenes de tarjeta guardados." }
            
            val prompt = """
                Sos un asistente financiero personal experto. Estos son los datos financieros del usuario, incluyendo transacciones manuales y resúmenes de tarjeta cargados.
                
                HISTORIAL DE RESÚMENES DE TARJETA:
                $resumenesText
                
                TRANSACCIONES (INGRESOS):
                $incomeText
                
                TRANSACCIONES (GASTOS):
                $expenseText
                
                RESUMEN GENERAL DE TRANSACCIONES:
                Balance actual: $${String.format("%.2f", balance)}
                Total ingresos: $${String.format("%.2f", totalIncome)}
                Total gastos: $${String.format("%.2f", totalExpenses)}
                
                INSTRUCCIONES:
                1. Respondé en español.
                2. Compará meses si hay varios resúmenes.
                3. Identificá si los gastos están subiendo o bajando.
                4. Proyectá el gasto del próximo mes basándote en las cuotas futuras mencionadas en los textos de los resúmenes.
                5. Sé preciso y basate únicamente en estos datos.
                
                Pregunta del usuario: $input
            """.trimIndent()

            launch(Dispatchers.Main) {
                generateResponse(
                    model = model,
                    input = prompt,
                    onError = { errorMessage ->
                        handleError(
                            context = context,
                            task = modelManagerViewModel.getTaskById("llm_chat")!!,
                            model = model,
                            modelManagerViewModel = modelManagerViewModel,
                            errorMessage = errorMessage
                        )
                    }
                )
            }
        }
    }
}
