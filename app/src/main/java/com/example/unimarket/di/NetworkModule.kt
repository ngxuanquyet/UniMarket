package com.example.unimarket.di

import com.example.unimarket.BuildConfig
import com.example.unimarket.data.api.GeminiApiService
import com.example.unimarket.data.api.ImageApiService
import com.example.unimarket.data.api.NotificationApiService
import com.example.unimarket.data.api.SepayApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object  NetworkModule {
    private const val IMAGE_BASE_URL = "https://api.imgbb.com/1/"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val SEPAY_BASE_URL = "https://my.sepay.vn/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideImageRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(IMAGE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideImageApiService(retrofit: Retrofit): ImageApiService {
        return retrofit.create(ImageApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationApiService(okHttpClient: OkHttpClient): NotificationApiService {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.NOTIFICATION_SERVER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(NotificationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(okHttpClient: OkHttpClient): GeminiApiService {
        return Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(GeminiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSepayApiService(okHttpClient: OkHttpClient): SepayApiService {
        return Retrofit.Builder()
            .baseUrl(SEPAY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SepayApiService::class.java)
    }
}
