package com.google.ai.edge.gallery.ui.misfinanzas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.data.Transaction
import com.google.ai.edge.gallery.data.TransactionCategory
import com.google.ai.edge.gallery.data.TransactionType
import com.google.ai.edge.gallery.proto.Theme
import com.google.ai.edge.gallery.ui.common.NeonButton
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.common.chat.ChatView
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.theme.ThemeSettings
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisFinanzasScreen(
    viewModel: MisFinanzasViewModel,
    modelManagerViewModel: ModelManagerViewModel,
    onBackClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.OTHER) }
    var showChat by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val isDark = when (ThemeSettings.themeOverride.value) {
        Theme.THEME_DARK -> true
        Theme.THEME_LIGHT -> false
        else -> isSystemInDarkTheme()
    }

    if (showChat) {
        MisFinanzasChatView(
            modelManagerViewModel = modelManagerViewModel,
            onBackClicked = { showChat = false }
        )
    } else {
        Scaffold(
            containerColor = if (isDark) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Mis Finanzas", color = if (isDark) Color.White else Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            ReportExporter.generateFinancePdf(
                                context = context,
                                periodName = "General",
                                transactions = uiState.transactions,
                                onComplete = { file ->
                                    exportedFile = file
                                    showExportDialog = true
                                }
                            )
                        }) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Exportar PDF", tint = Color.White)
                        }
                        IconButton(onClick = { showChat = true }) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "Preguntar a IA", tint = if (isDark) Color(0xFF00FF87) else Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Resumen
                ResumenCard(uiState, isDark)

                // Resumen Visual (Gráfico)
                ResumenVisualCard(uiState.transactions)

                // Botón Exportar PDF
                NeonButton(
                    text = "📄 Exportar Informe PDF",
                    onClick = {
                        ReportExporter.generateFinancePdf(
                            context = context,
                            periodName = "General",
                            transactions = uiState.transactions,
                            onComplete = { file ->
                                exportedFile = file
                                showExportDialog = true
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.PictureAsPdf
                )

                // Botón Preguntar a la IA
                NeonButton(
                    text = "Preguntarle a la IA",
                    onClick = { showChat = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.AutoAwesome,
                    neonAccent = isDark
                )

                // Formulario
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = if (isDark) Color(0xFF00FF87) else MaterialTheme.colorScheme.primary,
                                focusedLabelColor = if (isDark) Color(0xFF00FF87) else MaterialTheme.colorScheme.primary
                            )
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                            label = { Text("Monto") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = if (isDark) Color(0xFF00FF87) else MaterialTheme.colorScheme.primary,
                                focusedLabelColor = if (isDark) Color(0xFF00FF87) else MaterialTheme.colorScheme.primary
                            )
                        )

                        Text("Categoría", style = MaterialTheme.typography.labelLarge, color = if (isDark) Color.White else Color.Unspecified)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(TransactionCategory.entries.toTypedArray()) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text("${category.icon} ${category.displayName}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isDark) Color(0xFF00FF87) else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer,
                                        labelColor = if (isDark) Color.White else Color.Unspecified
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NeonButton(
                                text = "Ingreso",
                                onClick = {
                                    val amt = amount.toDoubleOrNull() ?: 0.0
                                    if (description.isNotBlank() && amt > 0) {
                                        viewModel.addTransaction(description, amt, TransactionType.INCOME, selectedCategory)
                                        description = ""
                                        amount = ""
                                        selectedCategory = TransactionCategory.OTHER
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Add,
                                containerColor = if (isDark) Color(0xFF1B5E20) else Color(0xFF2E7D32)
                            )
                            NeonButton(
                                text = "Gasto",
                                onClick = {
                                    val amt = amount.toDoubleOrNull() ?: 0.0
                                    if (description.isNotBlank() && amt > 0) {
                                        viewModel.addTransaction(description, amt, TransactionType.EXPENSE, selectedCategory)
                                        description = ""
                                        amount = ""
                                        selectedCategory = TransactionCategory.OTHER
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Remove,
                                containerColor = if (isDark) Color(0xFFB71C1C) else Color(0xFFC62828)
                            )
                        }
                    }
                }

                // Lista de Transacciones
                Text(
                    "Historial",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.transactions.forEach { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onDelete = { viewModel.deleteTransaction(transaction) },
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }

    if (showExportDialog && exportedFile != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = if (isDark) Color(0xFF1A1A1A) else Color.White,
            title = { Text("Informe PDF Generado", color = if (isDark) Color.White else Color.Unspecified) },
            text = { Text("El informe se ha guardado correctamente. ¿Deseas compartirlo?", color = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Unspecified) },
            confirmButton = {
                NeonButton(
                    text = "📤 Compartir",
                    onClick = {
                        ReportExporter.sharePdf(context, exportedFile!!)
                        showExportDialog = false
                    },
                    neonAccent = true
                )
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("✅ Listo", color = if (isDark) Color.White else Color.Unspecified)
                }
            }
        )
    }
}

@Composable
fun ResumenVisualCard(transactions: List<Transaction>) {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    val totalExpenses = expenses.sumOf { it.amount }
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Resumen Visual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            if (expenses.isEmpty() || totalExpenses == 0.0) {
                Box(
                    modifier = Modifier.height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Agregá gastos para ver el gráfico",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val categoryData = expenses.groupBy { it.category }
                    .mapValues { it.value.sumOf { t -> t.amount } }

                val colors = mapOf(
                    TransactionCategory.FOOD to Color(0xFFFF9800),
                    TransactionCategory.TRANSPORT to Color(0xFF2196F3),
                    TransactionCategory.HEALTH to Color(0xFFF44336),
                    TransactionCategory.WORK to Color(0xFF9C27B0),
                    TransactionCategory.HOME to Color(0xFFFFC107),
                    TransactionCategory.OTHER to Color(0xFF9E9E9E)
                )

                PieChart(categoryData, totalExpenses, colors)
                Spacer(Modifier.height(24.dp))
                Legend(categoryData.keys.toList(), colors)
            }
        }
    }
}

@Composable
fun PieChart(data: Map<TransactionCategory, Double>, total: Double, colors: Map<TransactionCategory, Color>) {
    val sortedData = data.toList().sortedByDescending { it.second }
    
    Canvas(modifier = Modifier.size(180.dp)) {
        var startAngle = -90f
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)

        sortedData.forEach { (category, amount) ->
            val sweepAngle = ((amount / total) * 360f).toFloat()
            val arcColor = colors[category] ?: Color.Gray

            drawArc(
                color = arcColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )

            // Dibujar porcentaje
            val percentage = (amount / total * 100).toInt()
            if (percentage > 5) {
                val midAngle = startAngle + sweepAngle / 2
                val x = center.x + (radius * 0.7f) * cos(Math.toRadians(midAngle.toDouble())).toFloat()
                val y = center.y + (radius * 0.7f) * sin(Math.toRadians(midAngle.toDouble())).toFloat()

                drawContext.canvas.nativeCanvas.drawText(
                    "$percentage%",
                    x,
                    y + 10f,
                    android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize = 32f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                )
            }

            startAngle += sweepAngle
        }
    }
}

@Composable
fun Legend(categories: List<TransactionCategory>, colors: Map<TransactionCategory, Color>) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowCategories.forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(colors[category] ?: Color.Gray, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${category.icon} ${category.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MisFinanzasChatView(
    modelManagerViewModel: ModelManagerViewModel,
    onBackClicked: () -> Unit,
    viewModel: MisFinanzasChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val task = modelManagerViewModel.getTaskById("llm_chat")!!

    ChatView(
        task = task,
        viewModel = viewModel,
        modelManagerViewModel = modelManagerViewModel,
        onSendMessage = { model, messages ->
            val lastMessage = messages.lastOrNull()
            val lastUserMessage = if (lastMessage is ChatMessageText) lastMessage.content else ""
            viewModel.addMessage(model = model, message = messages.last())
            viewModel.generateFinanceResponse(
                context = context,
                model = model,
                input = lastUserMessage,
                modelManagerViewModel = modelManagerViewModel
            )
        },
        onRunAgainClicked = { model, message ->
            // Implementación simplificada para reintentar
        },
        onBenchmarkClicked = { _, _, _, _ -> },
        onResetSessionClicked = { model ->
            viewModel.resetSession(task = task, model = model)
        },
        navigateUp = onBackClicked,
        showStopButtonInInputWhenInProgress = true,
        onStopButtonClicked = { model -> viewModel.stopResponse(model) }
    )
}

@Composable
fun ResumenCard(uiState: FinanzasUiState, isDark: Boolean = false) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
    val isPositive = uiState.balance >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Balance Actual", 
                style = MaterialTheme.typography.labelLarge,
                color = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    currencyFormatter.format(uiState.balance),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) {
                        if (isDark) Color(0xFF00FF87) else Color(0xFF1B5E20)
                    } else {
                        if (isDark) Color(0xFFEF9A9A) else Color(0xFFB71C1C)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isPositive) "✅" else "⚠️",
                    fontSize = 28.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResumenItem(
                    "Ingresos", 
                    currencyFormatter.format(uiState.totalIncome), 
                    if (isDark) Color(0xFF00FF87) else Color(0xFF2E7D32),
                    isDark
                )
                ResumenItem(
                    "Gastos", 
                    currencyFormatter.format(uiState.totalExpenses), 
                    if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828),
                    isDark
                )
            }
        }
    }
}

@Composable
fun ResumenItem(label: String, value: String, color: Color, isDark: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label, 
            style = MaterialTheme.typography.labelMedium,
            color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Unspecified
        )
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onDelete: () -> Unit, isDark: Boolean = false) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
    val dateFormatter = SimpleDateFormat("d 'de' MMMM 'de' yyyy HH:mm", Locale("es", "AR"))
    val isIncome = transaction.type == TransactionType.INCOME

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de categoría
            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(transaction.category.icon, fontSize = 20.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.description, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    dateFormatter.format(Date(transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (isIncome) "+ " else "- ") + currencyFormatter.format(transaction.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) {
                        if (isDark) Color(0xFF00FF87) else Color(0xFF2E7D32)
                    } else {
                        if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
                    }
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
