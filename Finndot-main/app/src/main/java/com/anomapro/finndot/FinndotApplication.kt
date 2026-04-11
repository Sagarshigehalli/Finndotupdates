package com.anomapro.finndot

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinndotApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    private var activityReferences = 0
    private var isInForeground = false

    /**
     * Publicly accessible flag to check if the app is in the foreground.
     * Used by SmsBroadcastReceiver to determine whether to show notifications.
     */
    @Volatile
    var isAppInForeground: Boolean = false
        private set
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        setupActivityLifecycleCallbacks()
    }
    
    private fun setupActivityLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            
            override fun onActivityStarted(activity: Activity) {
                activityReferences++
                if (!isInForeground) {
                    // App came to foreground
                    isInForeground = true
                    isAppInForeground = true
                }
            }
            
            override fun onActivityResumed(activity: Activity) {}
            
            override fun onActivityPaused(activity: Activity) {}
            
            override fun onActivityStopped(activity: Activity) {
                activityReferences--
                if (activityReferences == 0) {
                    // App went to background
                    isInForeground = false
                    isAppInForeground = false
                }
            }
            
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}