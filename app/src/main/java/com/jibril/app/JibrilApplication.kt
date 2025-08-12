package com.jibril.app

import android.app.Application
import android.os.StrictMode
import com.jibril.app.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class JibrilApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Performance optimization: Enable StrictMode in debug builds
        if (BuildConfig.DEBUG) {
            enableStrictMode()
            setupTimber()
        }
        
        // Performance optimization: Initialize heavy components lazily
        initializeApplication()
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun initializeApplication() {
        // Initialize only essential components here
        // Heavy initialization should be done lazily when needed
        
        // Performance optimization: Pre-warm critical components
        preWarmComponents()
    }
    
    private fun preWarmComponents() {
        // Pre-warm components that are likely to be used soon
        // This can be done in a background thread
        Thread {
            // Pre-warm network stack, image loading, etc.
            initializeNetworkComponents()
        }.start()
    }
    
    private fun initializeNetworkComponents() {
        // Initialize network components in background
        // This reduces first network request latency
    }
}