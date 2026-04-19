package com.google.ai.edge.gallery.di

import android.content.Context
import androidx.room.Room
import com.google.ai.edge.gallery.data.AppDatabase
import com.google.ai.edge.gallery.data.ResumenTarjetaDao
import com.google.ai.edge.gallery.data.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "el_contador_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideResumenTarjetaDao(database: AppDatabase): ResumenTarjetaDao {
        return database.resumenTarjetaDao()
    }
}
