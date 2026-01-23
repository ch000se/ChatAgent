package com.example.chatagent.di

import com.example.chatagent.data.repository.BenchmarkRepositoryImpl
import com.example.chatagent.data.repository.ChatRepositoryImpl
import com.example.chatagent.data.repository.DocumentRepositoryImpl
import com.example.chatagent.data.repository.OllamaRepositoryImpl
import com.example.chatagent.domain.repository.BenchmarkRepository
import com.example.chatagent.domain.repository.ChatRepository
import com.example.chatagent.domain.repository.DocumentRepository
import com.example.chatagent.domain.repository.OllamaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindBenchmarkRepository(
        benchmarkRepositoryImpl: BenchmarkRepositoryImpl
    ): BenchmarkRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        documentRepositoryImpl: DocumentRepositoryImpl
    ): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindOllamaRepository(
        ollamaRepositoryImpl: OllamaRepositoryImpl
    ): OllamaRepository
}