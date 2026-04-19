package com.google.ai.edge.gallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.Date

enum class TransactionType {
    INCOME, EXPENSE
}

enum class TransactionCategory(val icon: String, val displayName: String) {
    FOOD("🍔", "Comida"),
    TRANSPORT("🚗", "Transporte"),
    HEALTH("💊", "Salud"),
    WORK("💼", "Trabajo"),
    HOME("🏠", "Hogar"),
    OTHER("⭐", "Otro")
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String = "",
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: TransactionCategory = TransactionCategory.OTHER,
    val date: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }
    }

    @TypeConverter
    fun fromTransactionCategory(value: TransactionCategory): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionCategory(value: String): TransactionCategory {
        return try {
            TransactionCategory.valueOf(value)
        } catch (e: Exception) {
            TransactionCategory.OTHER
        }
    }
}
