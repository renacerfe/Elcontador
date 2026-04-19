package com.google.ai.edge.gallery.ui.misfinanzas

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.ai.edge.gallery.data.ResumenTarjeta
import com.google.ai.edge.gallery.data.Transaction
import com.google.ai.edge.gallery.data.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ReportExporter {

    fun generateFinancePdf(
        context: Context,
        periodName: String,
        transactions: List<Transaction>,
        resumenes: List<ResumenTarjeta> = emptyList(),
        onComplete: (File?) -> Unit
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
        }

        // A4 page size: 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = 40f
        val margin = 40f
        val contentWidth = 595f - (2 * margin)

        // 1. Header
        canvas.drawText("El Contador", margin, y, titlePaint)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Generado el: ${sdf.format(Date())}", 400f, y, bodyPaint)
        y += 20f
        canvas.drawText("Informe Financiero - $periodName", margin, y, headerPaint)
        y += 30f

        // 2. Summary
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = totalIncome - totalExpenses

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

        canvas.drawText("RESUMEN DEL PERÍODO", margin, y, headerPaint)
        y += 20f
        canvas.drawText("Total Ingresos: ${currencyFormatter.format(totalIncome)}", margin, y, bodyPaint)
        y += 15f
        canvas.drawText("Total Gastos: ${currencyFormatter.format(totalExpenses)}", margin, y, bodyPaint)
        y += 15f
        canvas.drawText("Balance: ${currencyFormatter.format(balance)}", margin, y, bodyPaint.apply { 
            color = if (balance >= 0) Color.BLACK else Color.RED
        })
        bodyPaint.color = Color.BLACK
        y += 40f

        // 3. Simple Bar Chart
        canvas.drawText("VISUALIZACIÓN", margin, y, headerPaint)
        y += 20f
        val chartHeight = 100f
        val barWidth = 60f
        val maxVal = maxOf(totalIncome, totalExpenses, 1.0).toFloat()
        
        // Income bar
        val incomeBarHeight = (totalIncome.toFloat() / maxVal) * chartHeight
        paint.color = Color.GREEN
        canvas.drawRect(margin + 50f, y + (chartHeight - incomeBarHeight), margin + 50f + barWidth, y + chartHeight, paint)
        canvas.drawText("Ingresos", margin + 50f, y + chartHeight + 15f, bodyPaint)
        
        // Expense bar
        val expenseBarHeight = (totalExpenses.toFloat() / maxVal) * chartHeight
        paint.color = Color.RED
        canvas.drawRect(margin + 150f, y + (chartHeight - expenseBarHeight), margin + 150f + barWidth, y + chartHeight, paint)
        canvas.drawText("Gastos", margin + 150f, y + chartHeight + 15f, bodyPaint)
        
        y += chartHeight + 40f

        // 4. Transactions Table
        canvas.drawText("DETALLE DE TRANSACCIONES", margin, y, headerPaint)
        y += 20f
        
        // Table Header
        val colWidths = floatArrayOf(80f, 200f, 80f, 60f, 80f)
        var currentX = margin
        val tableHeaders = arrayOf("Fecha", "Descripción", "Categoría", "Tipo", "Monto")
        
        paint.color = Color.LTGRAY
        canvas.drawRect(margin, y - 12f, margin + contentWidth, y + 5f, paint)
        paint.color = Color.BLACK
        
        for (i in tableHeaders.indices) {
            canvas.drawText(tableHeaders[i], currentX, y, bodyPaint.apply { isFakeBoldText = true })
            currentX += colWidths[i]
        }
        bodyPaint.isFakeBoldText = false
        y += 20f

        val tableDateSdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        transactions.sortedByDescending { it.date }.take(20).forEach { trans ->
            if (y > 780f) { // Simple page break check (not fully robust but works for first page)
                // In a real app we'd start a new page here
            }
            currentX = margin
            canvas.drawText(tableDateSdf.format(Date(trans.date)), currentX, y, bodyPaint)
            currentX += colWidths[0]
            canvas.drawText(trans.description.take(25), currentX, y, bodyPaint)
            currentX += colWidths[1]
            canvas.drawText(trans.category.displayName, currentX, y, bodyPaint)
            currentX += colWidths[2]
            canvas.drawText(if (trans.type == TransactionType.INCOME) "ING" else "GST", currentX, y, bodyPaint)
            currentX += colWidths[3]
            canvas.drawText(currencyFormatter.format(trans.amount), currentX, y, bodyPaint)
            y += 15f
        }

        // 5. Tarjeta Naranja Section
        if (resumenes.isNotEmpty()) {
            y += 20f
            canvas.drawText("TARJETA NARANJA", margin, y, headerPaint)
            y += 20f
            resumenes.forEach { res ->
                canvas.drawText("${res.fechaResumen}: Total ${currencyFormatter.format(res.totalAPagar)} (Vence ${res.fechaVencimiento})", margin, y, bodyPaint)
                y += 15f
            }
        }

        // 6. Footer
        canvas.drawText("Generado por El Contador — Tu asistente financiero personal con IA local", margin, 810f, bodyPaint.apply { textSize = 10f; color = Color.GRAY })

        pdfDocument.finishPage(page)

        // Save file
        val fileName = "ElContador_${periodName.replace(" ", "_")}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            onComplete(file)
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(null)
        } finally {
            pdfDocument.close()
        }
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Informe PDF"))
    }
}
