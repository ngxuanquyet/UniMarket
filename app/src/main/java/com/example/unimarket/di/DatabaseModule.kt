package com.example.unimarket.di

import android.content.Context
import androidx.room.Room
import com.example.unimarket.data.local.AppDatabase
import com.example.unimarket.data.local.DraftProductDao
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
            "unimarket-db"
        ).build()
    }

    @Provides
    fun provideDraftProductDao(appDatabase: AppDatabase): DraftProductDao {
        return appDatabase.draftProductDao
    }
}
