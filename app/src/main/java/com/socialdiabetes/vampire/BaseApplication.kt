package com.socialdiabetes.vampire

import android.app.Application

class BaseApplication : Application() {
    val healthConnectManager by lazy {
        HealthConnectManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: BaseApplication
            private set
    }
}