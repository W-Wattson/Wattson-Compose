package com.wattson.di

import android.content.Context
import androidx.room.Room
import com.wattson.data.local.AppDatabase
import com.wattson.data.local.dao.ChatMessageDao
import com.wattson.data.local.dao.RepairConversationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wattson_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRepairConversationDao(database: AppDatabase): RepairConversationDao {
        return database.repairConversationDao()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }
}
