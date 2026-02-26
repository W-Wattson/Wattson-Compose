package com.wattson.di

import com.wattson.BuildConfig
import com.wattson.data.remote.api.WattsonApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing network dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides the base URL for the API.
     * Reads from local.properties via BuildConfig: api.base.url=http://...
     */
    @Provides
    @Singleton
    fun provideBaseUrl(): String {
        return BuildConfig.API_BASE_URL
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Use HEADERS instead of BODY to avoid EOFException with chunked responses via ADB reverse
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")

                // Don't override Content-Type for multipart requests (file uploads)
                if (original.body !is okhttp3.MultipartBody) {
                    requestBuilder.header("Content-Type", "application/json")
                }

                val request = requestBuilder
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            // Add logging as network interceptor at the end to avoid body reading issues
            .addNetworkInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS) // Longer timeout for uploads
            .retryOnConnectionFailure(true)
            // Shorter keep-alive to avoid "unexpected end of stream" on stale connections
            .connectionPool(ConnectionPool(5, 15, TimeUnit.SECONDS))
            // Force HTTP/1.1 to avoid HTTP/2 multiplexing issues with some servers
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWattsonApi(retrofit: Retrofit): WattsonApi {
        return retrofit.create(WattsonApi::class.java)
    }
}
