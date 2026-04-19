package com.google.ai.edge.gallery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumenTarjetaDao {
    @Query("SELECT * FROM resumenes_tarjeta ORDER BY fechaCarga DESC")
    fun getAllResumenes(): Flow<List<ResumenTarjeta>>

    @Insert
    suspend fun insertResumen(resumen: ResumenTarjeta)

    @Query("DELETE FROM resumenes_tarjeta")
    suspend fun deleteAll()
}
