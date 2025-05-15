package com.example.smartdocsconvert.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PDFCrafterTemplate : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "PDFCrafterTemplate"
    }

    override fun onCreate() {
        super.onCreate()
        // İhtiyaç duyduğunuz diğer başlatma işlemleri burada yapılabilir
        Log.d(TAG, "WorkManager initialized with custom configuration")
    }

    /**
     * WorkManager için özel yapılandırma sağlayan metot.
     * Bu metot sayesinde WorkManager'ın davranışını özelleştirebilirsiniz.
     */


    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }
}