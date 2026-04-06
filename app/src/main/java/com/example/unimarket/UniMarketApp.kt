package com.example.unimarket

import android.app.Application
import com.example.unimarket.localization.LanguageManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UniMarketApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LanguageManager.applySavedLocale(this)
    }
}
