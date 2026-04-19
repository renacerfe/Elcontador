package com.google.ai.edge.gallery.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.data.ResumenTarjeta
import com.google.ai.edge.gallery.data.Transaction
import com.google.ai.edge.gallery.data.TransactionType
import java.text.NumberFormat
import java.util.*

@Composable
fun TransactionRow(transaction: Transaction, isDark: Boolean = false) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
    val isIncome = transaction.type == TransactionType.INCOME
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(transaction.category.icon, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                transaction.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            (if (isIncome) "+ " else "- ") + currencyFormatter.format(transaction.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) {
                if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
            } else {
                if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
            }
        )
    }
}

@Composable
fun HistoryItem(resumen: ResumenTarjeta, isDark: Boolean) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(resumen.fechaResumen, fontWeight = FontWeight.Bold)
                Text(
                    "Vencimiento: ${resumen.fechaVencimiento}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                currencyFormatter.format(resumen.totalAPagar),
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
            )
        }
    }
}
