package com.google.ai.edge.gallery.ui.misfinanzas

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.TransactionCategory
import com.google.ai.edge.gallery.data.TransactionType
import com.google.ai.edge.gallery.ui.common.NeonButton
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.llmchat.LlmChatViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    modelManagerViewModel: ModelManagerViewModel,
    finanzasViewModel: MisFinanzasViewModel,
    onBackClicked: () -> Unit
) {
    val chatViewModel: LlmChatViewModel = hiltViewModel()

    var hasCameraPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Detected fields (parsed from AI response)
    var merchantName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf(0.0) }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.OTHER) }

    if (hasCameraPermission) {
        if (capturedBitmap == null) {
            CameraCaptureView(
                onImageCaptured = { bitmap ->
                    capturedBitmap = bitmap
                    isProcessing = true
                    processImage(bitmap, onTextExtracted = { text ->
                        val task = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_CHAT)
                        val model = modelManagerViewModel.uiState.value.selectedModel
                        
                        if (task != null && model != null) {
                            val prompt = """
                                Analizá esta boleta o factura y extraé: nombre del comercio, fecha, total a pagar o monto total, y si hay detalle de productos listá los más importantes con sus precios. Respondé en español de forma ordenada.
                                
                                Texto extraído:
                                $text
                            """.trimIndent()
                            
                            chatViewModel.generateResponse(
                                model = model,
                                input = prompt,
                                onError = { 
                                    aiResponse = "Error: $it"
                                    isProcessing = false
                                },
                                onDone = {
                                    val lastMsg = chatViewModel.getLastMessage(model)
                                    if (lastMsg is ChatMessageText) {
                                        aiResponse = lastMsg.content
                                        // Simple parsing attempt
                                        merchantName = extractMerchant(lastMsg.content)
                                        totalAmount = extractAmount(lastMsg.content)
                                    }
                                    isProcessing = false
                                }
                            )
                        } else {
                            aiResponse = "Modelo no disponible."
                            isProcessing = false
                        }
                    })
                },
                onBackClicked = onBackClicked
            )
        } else {
            ResultView(
                bitmap = capturedBitmap!!,
                aiResponse = aiResponse,
                isProcessing = isProcessing,
                merchantName = merchantName,
                totalAmount = totalAmount,
                selectedCategory = selectedCategory,
                onMerchantChange = { merchantName = it },
                onAmountChange = { totalAmount = it },
                onCategoryChange = { selectedCategory = it },
                onSave = {
                    finanzasViewModel.addTransaction(
                        description = merchantName.ifEmpty { "Gasto de Boleta" },
                        amount = totalAmount,
                        type = TransactionType.EXPENSE,
                        category = selectedCategory
                    )
                    onBackClicked()
                },
                onRetake = {
                    capturedBitmap = null
                    aiResponse = null
                    merchantName = ""
                    totalAmount = 0.0
                },
                onBackClicked = onBackClicked
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
            Text("Se requiere permiso de cámara", color = Color.White)
        }
    }
}

@Composable
fun CameraCaptureView(onImageCaptured: (Bitmap) -> Unit, onBackClicked: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(cameraProviderFuture) {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("ScanReceipt", "Use case binding failed", e)
            }
        }

        // UI Controls
        IconButton(
            onClick = onBackClicked,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
        ) {
            NeonButton(
                text = "TOMAR FOTO",
                onClick = {
                    imageCapture.takePicture(
                        cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                image.close()
                                onImageCaptured(bitmap)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                Log.e("ScanReceipt", "Photo capture failed", exception)
                            }
                        }
                    )
                },
                icon = Icons.Rounded.CameraAlt,
                neonAccent = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultView(
    bitmap: Bitmap,
    aiResponse: String?,
    isProcessing: Boolean,
    merchantName: String,
    totalAmount: Double,
    selectedCategory: TransactionCategory,
    onMerchantChange: (String) -> Unit,
    onAmountChange: (Double) -> Unit,
    onCategoryChange: (TransactionCategory) -> Unit,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onBackClicked: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            TopAppBar(
                title = { Text("Análisis Holográfico", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Recibo capturado",
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            )

            if (isProcessing) {
                CircularProgressIndicator(color = Color(0xFF00FF87))
                Text("Escaneando datos con IA local...", color = Color.White)
            } else if (aiResponse != null) {
                // Large Merchant Name
                Text(
                    text = merchantName.ifEmpty { "Comercio no detectado" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF87)
                )

                // Total Amount Highlighted
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Monto Detectado", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        Text(
                            text = "$${String.format("%.2f", totalAmount)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00FF87)
                        )
                    }
                }

                // AI Analysis Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ANÁLISIS DE IA:", fontWeight = FontWeight.Bold, color = Color(0xFF00FF87), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(aiResponse, color = Color.White)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Edit Fields
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = onMerchantChange,
                    label = { Text("Nombre del Comercio") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FF87),
                        focusedLabelColor = Color(0xFF00FF87)
                    )
                )

                OutlinedTextField(
                    value = if (totalAmount == 0.0) "" else totalAmount.toString(),
                    onValueChange = { onAmountChange(it.toDoubleOrNull() ?: 0.0) },
                    label = { Text("Monto Total") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FF87),
                        focusedLabelColor = Color(0xFF00FF87)
                    )
                )

                Spacer(Modifier.height(16.dp))

                NeonButton(
                    text = "GUARDAR GASTO",
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Save,
                    neonAccent = true
                )

                NeonButton(
                    text = "REINTENTAR ESCANEO",
                    onClick = onRetake,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.CameraAlt,
                    containerColor = Color.Transparent
                )
            }
        }
    }
}

private fun processImage(bitmap: Bitmap, onTextExtracted: (String) -> Unit) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            onTextExtracted(visionText.text)
        }
        .addOnFailureListener { e ->
            onTextExtracted("Error reconociendo texto: ${e.message}")
        }
}

private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

// Helper functions for basic parsing from AI response
private fun extractMerchant(text: String): String {
    val lines = text.lines()
    // Look for lines that mention "Comercio" or "Nombre"
    val merchantLine = lines.find { it.contains("comercio", ignoreCase = true) || it.contains("nombre", ignoreCase = true) }
    return merchantLine?.substringAfter(":")?.trim()?.removePrefix("*")?.trim() ?: ""
}

private fun extractAmount(text: String): Double {
    val totalLine = text.lines().find { 
        it.contains("total", ignoreCase = true) || it.contains("monto", ignoreCase = true) || it.contains("pagar", ignoreCase = true)
    }
    if (totalLine != null) {
        val regex = """(\d+[\.,]\d+)""".toRegex()
        val match = regex.find(totalLine)
        return match?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
    }
    return 0.0
}
