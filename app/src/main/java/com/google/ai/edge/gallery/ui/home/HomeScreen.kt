/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.GalleryTopAppBar
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.AppBarAction
import com.google.ai.edge.gallery.data.AppBarActionType
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.data.Transaction
import com.google.ai.edge.gallery.data.TransactionType
import com.google.ai.edge.gallery.proto.Theme
import com.google.ai.edge.gallery.ui.common.NeonButton
import com.google.ai.edge.gallery.ui.common.TransactionRow
import com.google.ai.edge.gallery.ui.common.rememberDelayedAnimationProgress
import com.google.ai.edge.gallery.ui.common.tos.AppTosDialog
import com.google.ai.edge.gallery.ui.common.tos.TosViewModel
import com.google.ai.edge.gallery.ui.misfinanzas.MisFinanzasViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.theme.ThemeSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

private const val TAG = "AGHomeScreen"
private const val ANIMATION_INIT_DELAY = 0L
private const val TOP_APP_BAR_ANIMATION_DURATION = 600

/** Navigation destination data */
private object HomeScreenDestination {
    @StringRes
    val titleRes = R.string.app_name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modelManagerViewModel: ModelManagerViewModel,
    tosViewModel: TosViewModel,
    navigateToTaskScreen: (Task) -> Unit,
    onModelsClicked: () -> Unit,
    onFinancesClicked: () -> Unit,
    onTarjetaNaranjaClicked: () -> Unit,
    onHistorialClicked: () -> Unit,
    onScanReceiptClicked: () -> Unit,
    enableAnimation: Boolean,
    modifier: Modifier = Modifier,
    gm4: Boolean = false,
    finanzasViewModel: MisFinanzasViewModel = hiltViewModel()
) {
    val uiState by modelManagerViewModel.uiState.collectAsState()
    val finanzasUiState by finanzasViewModel.uiState.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 6..12 -> "Buenos días 👋"
            in 13..20 -> "Buenas tardes 👋"
            else -> "Buenas noches 👋"
        }
    }

    val consejos = remember {
        listOf(
            "Ahorrar una parte de tus ingresos es el primer paso hacia la libertad financiera.",
            "Evita las compras por impulso; espera 24 horas antes de decidir.",
            "Pequeños gastos diarios pueden sumar grandes cantidades al mes. ¡Cuida los gastos hormiga!",
            "Invierte en tu educación financiera; es la mejor inversión que puedes hacer.",
            "Tener un fondo de emergencia te da tranquilidad ante imprevistos."
        )
    }
    val consejoDelDia = remember { consejos.random() }

    val isDark = when (ThemeSettings.themeOverride.value) {
        Theme.THEME_DARK -> true
        Theme.THEME_LIGHT -> false
        else -> isSystemInDarkTheme()
    }

    if (!showTosDialog) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val requestPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> }

        LaunchedEffect(Unit) {
            delay(2000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = if (isDark) Color(0xFF0A0A0A) else Color(0xFF1B5E20),
                    drawerContentColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(24.dp)
                    ) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF00FF87)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "El Contador",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Asistente financiero personal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(Modifier.height(24.dp))

                        // Menu Options
                        NavigationDrawerItem(
                            label = { Text("🏠 Inicio", fontSize = 18.sp) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() } },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        NavigationDrawerItem(
                            label = { Text("📸 Escanear Boleta", fontSize = 18.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onScanReceiptClicked()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        NavigationDrawerItem(
                            label = { Text("📅 Historial", fontSize = 18.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onHistorialClicked()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        NavigationDrawerItem(
                            label = { Text("💰 Mis Finanzas", fontSize = 18.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onFinancesClicked()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        NavigationDrawerItem(
                            label = { Text("🟠 Tarjeta Naranja", fontSize = 18.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onTarjetaNaranjaClicked()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        NavigationDrawerItem(
                            label = { Text("🤖 Chat con IA", fontSize = 18.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                val chatTask = uiState.tasks.find { it.id == BuiltInTaskId.LLM_CHAT }
                                chatTask?.let { navigateToTaskScreen(it) }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        NavigationDrawerItem(
                            label = { Text("ℹ️ Acerca de", fontSize = 18.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showAboutDialog = true
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = Color.White
                            )
                        )
                    }
                }
            },
            gesturesEnabled = drawerState.isOpen,
        ) {
            Scaffold(
                containerColor = if (isDark) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.background,
                topBar = {
                    val progress = if (!enableAnimation) 1f
                    else rememberDelayedAnimationProgress(
                        initialDelay = ANIMATION_INIT_DELAY - 50,
                        animationDurationMs = TOP_APP_BAR_ANIMATION_DURATION,
                        animationLabel = "top bar",
                    )
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = progress
                            translationY = ((-16).dp * (1 - progress)).toPx()
                        }
                    ) {
                        GalleryTopAppBar(
                            title = stringResource(HomeScreenDestination.titleRes),
                            leftAction = AppBarAction(
                                actionType = AppBarActionType.MENU,
                                actionFn = {
                                    scope.launch { drawerState.apply { if (isClosed) open() else close() } }
                                },
                            ),
                        )
                    }
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .background(
                            if (isDark) {
                                Brush.verticalGradient(listOf(Color(0xFF0A0A0A), Color(0xFF1A1A1A)))
                            } else {
                                Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color.White))
                            }
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Saludo Dinámico
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onHistorialClicked) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = "Historial", tint = if (isDark) Color(0xFF00FF87) else MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Título y Subtítulo
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "El Contador",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF00FF87) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tu asistente financiero personal con IA local",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Tarjeta de Balance
                    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1A1A1A).copy(alpha = 0.8f) else MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Balance del Mes", style = MaterialTheme.typography.labelLarge, color = if (isDark) Color.White else Color.Unspecified)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currencyFormatter.format(finanzasUiState.balance),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (finanzasUiState.balance >= 0) {
                                        if (isDark) Color(0xFF00FF87) else Color(0xFF1B5E20)
                                    } else Color(0xFFB71C1C)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (finanzasUiState.balance >= 0) "✅" else "⚠️",
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }

                    // Sección Últimos Movimientos
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Últimos movimientos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Unspecified
                        )
                        
                        val latestTransactions = finanzasUiState.transactions.take(3)
                        if (latestTransactions.isEmpty()) {
                            Text(
                                "Aún no hay movimientos — tocá Mis Finanzas para empezar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            latestTransactions.forEach { transaction ->
                                TransactionRow(transaction, isDark)
                            }
                        }
                    }

                    // Botones de Acción
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón de Escaneo Destacado
                        NeonButton(
                            text = "📸 Escanear Boleta",
                            onClick = onScanReceiptClicked,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.ReceiptLong,
                            neonAccent = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NeonButton(
                                text = "Mis Finanzas",
                                onClick = onFinancesClicked,
                                modifier = Modifier.weight(1.2f),
                                neonAccent = isDark
                            )

                            NeonButton(
                                text = "Chat IA",
                                onClick = {
                                    val chatTask = uiState.tasks.find { it.id == BuiltInTaskId.LLM_CHAT }
                                    chatTask?.let { navigateToTaskScreen(it) }
                                },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.AutoAwesome
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NeonButton(
                                text = "🟠 Tarjeta",
                                onClick = onTarjetaNaranjaClicked,
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.CreditCard,
                                containerColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFFF6D00)
                            )
                            
                            NeonButton(
                                text = "📅 Historial",
                                onClick = { onHistorialClicked() },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.CalendarMonth
                            )
                        }
                    }

                    // Tarjeta Motivacional
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1A1A1A).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF00FF87) else Color(0xFFFBC02D)
                            )
                            Text(
                                text = consejoDelDia,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showTosDialog) {
        AppTosDialog(onTosAccepted = {
            showTosDialog = false
            tosViewModel.acceptTos()
        })
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Cerrar")
                }
            },
            title = { Text("Acerca de El Contador") },
            text = { Text("El Contador v1.0 — Tu asistente financiero personal con IA local. Desarrollado con Google AI Edge Gallery.") },
            icon = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null) }
        )
    }

    if (uiState.loadingModelAllowlistError.isNotEmpty()) {
        AlertDialog(
            icon = {
                Icon(
                    Icons.Rounded.Error,
                    contentDescription = stringResource(R.string.cd_error),
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(uiState.loadingModelAllowlistError) },
            text = { Text("Please check your internet connection and try again later.") },
            onDismissRequest = { modelManagerViewModel.loadModelAllowlist() },
            confirmButton = {
                TextButton(onClick = { modelManagerViewModel.loadModelAllowlist() }) { Text("Retry") }
            },
            dismissButton = {
                TextButton(onClick = { modelManagerViewModel.clearLoadModelAllowlistError() }) {
                    Text("Cancel")
                }
            },
        )
    }
}
