package com.example.unimarket.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // Adjust as needed
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                "KEY_UPLOAD_IMAGE" to "",
                "API_KEY" to "",
                "BANK_CODE" to "MB",
                "BANK_NAME" to "MBBank",
                "BANK_ACCOUNT_NAME" to "NGUYEN XUAN QUYET",
                "BANK_ACCOUNT_NUMBER" to "0356433860",
                "SEPAY_API_KEY" to "",
                "API_KEY_OTP_SMS" to "",
                "CODE_LENGTH" to "4",
                "SENDER" to "71e8a9ea-7daf-4fcc-b5d2-51d5ba57408a",
                "TEMPLATE" to "027fe998-92a5-4fd6-9b2a-83e4576ce1a9",
                "university_list" to "{\"universities\":[]}"
            )
        )
        return remoteConfig
    }

    @Provides
    @Singleton
    fun provideFireStore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
