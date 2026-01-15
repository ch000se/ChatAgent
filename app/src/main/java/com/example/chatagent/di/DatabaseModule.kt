package com.example.chatagent.di

import android.content.Context
import androidx.room.Room
import com.example.chatagent.data.local.ChatDatabase
import com.example.chatagent.data.local.dao.DocumentDao
import com.example.chatagent.data.local.dao.EmbeddingDao
import com.example.chatagent.data.local.dao.MessageDao
import com.example.chatagent.data.local.dao.SupportTicketDao
import com.example.chatagent.data.local.dao.SupportUserDao
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
    fun provideChatDatabase(
        @ApplicationContext context: Context
    ): ChatDatabase {
        return Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            "chat_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: ChatDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideDocumentDao(database: ChatDatabase): DocumentDao {
        return database.documentDao()
    }

    @Provides
    @Singleton
    fun provideEmbeddingDao(database: ChatDatabase): EmbeddingDao {
        return database.embeddingDao()
    }

    @Provides
    @Singleton
    fun provideSupportTicketDao(database: ChatDatabase): SupportTicketDao {
        return database.supportTicketDao()
    }

    @Provides
    @Singleton
    fun provideSupportUserDao(database: ChatDatabase): SupportUserDao {
        return database.supportUserDao()
    }
}
