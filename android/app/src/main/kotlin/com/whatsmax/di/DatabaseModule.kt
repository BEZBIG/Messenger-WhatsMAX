/**
 * di/DatabaseModule.kt
 * Hilt-модуль Room: предоставляет AppDatabase и DAO-объекты.
 */
package com.whatsmax.di

import android.content.Context
import androidx.room.Room
import com.whatsmax.data.local.AppDatabase
import com.whatsmax.data.local.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()
}
