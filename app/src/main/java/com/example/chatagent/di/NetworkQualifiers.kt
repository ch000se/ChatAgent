package com.example.chatagent.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClaudeRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HuggingFaceRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class McpRetrofit