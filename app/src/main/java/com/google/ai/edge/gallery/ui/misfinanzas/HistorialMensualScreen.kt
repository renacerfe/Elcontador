package com.google.ai.edge.gallery.ui.misfinanzas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.data.ResumenTarjeta
import com.google.ai.edge.gallery.data.Transaction
import com.google.ai.edge.gallery.proto.Theme
import com.google.ai.edge.gallery.ui.common.HistoryItem
import com.google.ai.edge.gallery.ui.common.TransactionRow
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.theme.ThemeSettings
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialMensualScreen(
    finanzasViewModel: MisFinanzasViewModel,
    modelManagerViewModel: ModelManagerViewModel,
    onBackClicked: () -> Unit
) {
    val uiState by finanzasViewModel.uiState.collectAsState()
    val resumenes by finanzasViewModel.resumenes.collectAsState()
    
    var selectedMonthYear by remember { mutableStateOf<String?>(null) }

    val isDark = when (ThemeSettings.themeOverride.value) {
        Theme.THEME_DARK -> true
        Theme.THEME_LIGHT -> false
        else -> isSystemInDarkTheme()
    }

    // Agrupar datos por mes/año
    val monthsWithData = remember(uiState.transactions, resumenes) {
        val months = mutableSetOf<String>()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "AR"))
        
        uiState.transactions.forEach {
            months.add(sdf.format(Date(it.date)).replaceFirstChar { it.uppercase() })
        }
        resumenes.forEach {
            months.add(it.fechaResumen)
        }
        months.toList().sortedByDescending { 
            try { sdf.parse(it.lowercase()) } catch(e: Exception) { Date(0) }
        }
    }

    if (selectedMonthYear != null) {
        DetalleMensualScreen(
            monthYear = selectedMonthYear!!,
            transactions = uiState.transactions,
            resumenes = resumenes,
            modelManagerViewModel = modelManagerViewModel,
            onBackClicked = { selectedMonthYear = null },
            isDark = isDark
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Historial Mensual") },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) Color(0xFF121212) else MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            if (monthsWithData.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("No hay datos registrados aún", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(monthsWithData) { month ->
                        MonthItem(month, isDark) { selectedMonthYear = month }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthItem(month: String, isDark: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(month, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleMensualScreen(
    monthYear: String,
    transactions: List<Transaction>,
    resumenes: List<ResumenTarjeta>,
    modelManagerViewModel: ModelManagerViewModel,
    onBackClicked: () -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "AR"))
    
    val monthTransactions = transactions.filter {
        sdf.format(Date(it.date)).equals(monthYear, ignoreCase = true)
    }
    
    val monthResumen = resumenes.find {
        it.fechaResumen.equals(monthYear, ignoreCase = true)
    }

    val totalIncome = monthTransactions.filter { it.type == com.google.ai.edge.gallery.data.TransactionType.INCOME }.sumOf { it.amount }
    val totalExpenses = monthTransactions.filter { it.type == com.google.ai.edge.gallery.data.TransactionType.EXPENSE }.sumOf { it.amount }
    
    var showChat by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    if (showChat) {
        val chatViewModel: MisFinanzasChatViewModel = hiltViewModel()
        val context = LocalContext.current
        val task = modelManagerViewModel.getTaskById("llm_chat")!!

        com.google.ai.edge.gallery.ui.common.chat.ChatView(
            task = task,
            viewModel = chatViewModel,
            modelManagerViewModel = modelManagerViewModel,
            onSendMessage = { model, messages ->
                val lastUserMessage = (messages.last() as com.google.ai.edge.gallery.ui.common.chat.ChatMessageText).content
                chatViewModel.addMessage(model = model, message = messages.last())
                
                // Preparar prompt específico del mes
                val contextPrompt = """
                    Estás analizando los datos del mes de $monthYear.
                    
                    RESUMEN DEL MES:
                    Total Ingresos: $${String.format("%.2f", totalIncome)}
                    Total Gastos Manuales: $${String.format("%.2f", totalExpenses)}
                    Balance del mes: $${String.format("%.2f", totalIncome - totalExpenses)}
                    
                    TRANSACCIONES DEL MES:
                    ${monthTransactions.joinToString("\n") { "- ${it.description}: $${it.amount} (${it.type})" }}
                    
                    ${monthResumen?.let { "RESUMEN TARJETA NARANJA:\nTotal a pagar: $${it.totalAPagar}\nVencimiento: ${it.fechaVencimiento}\nContenido:\n${it.textoCompleto}" } ?: "No hay resumen de tarjeta para este mes."}
                    
                    Responde preguntas específicas sobre este mes de manera experta.
                    
                    Pregunta: $lastUserMessage
                """.trimIndent()

                chatViewModel.generateResponse(
                    model = model,
                    input = contextPrompt,
                    onError = { chatViewModel.handleError(context, task, model, modelManagerViewModel, it) }
                )
            },
            onRunAgainClicked = { _, _ -> },
            onBenchmarkClicked = { _, _, _, _ -> },
            onResetSessionClicked = { model -> chatViewModel.resetSession(task, model) },
            navigateUp = { showChat = false },
            onStopButtonClicked = { model -> chatViewModel.stopResponse(model) }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(monthYear) },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            ReportExporter.generateFinancePdf(
                                context = context,
                                periodName = monthYear,
                                transactions = monthTransactions,
                                resumenes = monthResumen?.let { listOf(it) } ?: emptyList(),
                                onComplete = { file ->
                                    exportedFile = file
                                    showExportDialog = true
                                }
                            )
                        }) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Exportar PDF", tint = Color.White)
                        }
                        IconButton(onClick = { showChat = true }) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "Preguntar a IA", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) Color(0xFF121212) else MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp).background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Resumen Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Resumen de $monthYear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ingresos:")
                            Text("$${String.format("%.2f", totalIncome)}", color = if(isDark) Color(0xFF81C784) else Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gastos:")
                            Text("$${String.format("%.2f", totalExpenses)}", color = if(isDark) Color(0xFFEF9A9A) else Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Balance:", fontWeight = FontWeight.Bold)
                            Text("$${String.format("%.2f", totalIncome - totalExpenses)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (monthResumen != null) {
                    Text("Tarjeta Naranja", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HistoryItem(monthResumen, isDark)
                }

                Text("Transacciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(monthTransactions) { transaction ->
                        TransactionRow(transaction, isDark)
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            ReportExporter.generateFinancePdf(
                                context = context,
                                periodName = monthYear,
                                transactions = monthTransactions,
                                resumenes = monthResumen?.let { listOf(it) } ?: emptyList(),
                                onComplete = { file ->
                                    exportedFile = file
                                    showExportDialog = true
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("📄 Exportar PDF")
                    }

                    Button(
                        onClick = { showChat = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("🤖 Consultar IA")
                    }
                }
            }
        }
    }

    if (showExportDialog && exportedFile != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Informe PDF Generado") },
            text = { Text("El informe mensual se ha generado. ¿Deseas compartirlo?") },
            confirmButton = {
                Button(onClick = {
                    ReportExporter.sharePdf(context, exportedFile!!)
                    showExportDialog = false
                }) {
                    Text("📤 Compartir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("✅ Listo")
                }
            }
        )
    }
}
