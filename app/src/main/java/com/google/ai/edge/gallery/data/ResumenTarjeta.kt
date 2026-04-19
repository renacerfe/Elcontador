package com.google.ai.edge.gallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resumenes_tarjeta")
data class ResumenTarjeta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fechaResumen: String, // e.g., "Abril 2024"
    val totalAPagar: Double,
    val fechaVencimiento: String,
    val textoCompleto: String,
    val fechaCarga: Long = System.currentTimeMillis()
)
