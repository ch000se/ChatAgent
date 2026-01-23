package com.example.chatagent.di

import com.example.chatagent.BuildConfig
import com.example.chatagent.data.remote.api.ChatApiService
import com.example.chatagent.data.remote.api.OllamaApiService
import com.example.chatagent.data.remote.client.SseResponseConverterFactory
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", BuildConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)  // Increased for longer AI responses
            .writeTimeout(60, TimeUnit.SECONDS)

        // Only for DEBUG: Bypass SSL certificate verification
        // WARNING: This is UNSAFE and should NEVER be used in production!
        if (BuildConfig.DEBUG) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())

                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @ClaudeRetrofit
    fun provideClaudeRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideChatApiService(@ClaudeRetrofit retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }

    @Provides
    @Singleton
    @HuggingFaceRetrofit
    fun provideHuggingFaceRetrofit(): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.HF_API_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })

        // Only for DEBUG: Bypass SSL certificate verification
        if (BuildConfig.DEBUG) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())

                clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                clientBuilder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val client = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl("https://router.huggingface.co/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    @Provides
    @Singleton
    fun provideHuggingFaceApiService(
        @HuggingFaceRetrofit retrofit: Retrofit
    ): com.example.chatagent.data.remote.api.HuggingFaceApiService {
        return retrofit.create(com.example.chatagent.data.remote.api.HuggingFaceApiService::class.java)
    }

    @Provides
    @Singleton
    @McpRetrofit
    fun provideMcpRetrofit(loggingInterceptor: HttpLoggingInterceptor, gson: Gson): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())

                clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                clientBuilder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(clientBuilder.build())
            .addConverterFactory(SseResponseConverterFactory(gson))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideMcpApiService(@McpRetrofit retrofit: Retrofit): com.example.chatagent.data.remote.api.McpApiService {
        return retrofit.create(com.example.chatagent.data.remote.api.McpApiService::class.java)
    }

    @Provides
    @Singleton
    @EmbeddingRetrofit
    fun provideEmbeddingRetrofit(): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.HF_API_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())

                clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                clientBuilder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val client = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl("https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideEmbeddingApiService(
        @EmbeddingRetrofit retrofit: Retrofit
    ): com.example.chatagent.data.remote.api.EmbeddingApiService {
        return retrofit.create(com.example.chatagent.data.remote.api.EmbeddingApiService::class.java)
    }

    @Provides
    @Singleton
    @OllamaRetrofit
    fun provideOllamaRetrofit(loggingInterceptor: HttpLoggingInterceptor): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS) // Long timeout for local LLM inference
            .writeTimeout(60, TimeUnit.SECONDS)

        return Retrofit.Builder()
            .baseUrl(BuildConfig.OLLAMA_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOllamaApiService(
        @OllamaRetrofit retrofit: Retrofit
    ): OllamaApiService {
        return retrofit.create(OllamaApiService::class.java)
    }
}
