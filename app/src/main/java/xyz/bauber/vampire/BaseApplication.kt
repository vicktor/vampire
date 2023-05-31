package xyz.bauber.vampire

import android.app.Application
import xyz.bauber.vampire.health.HealthConnectManager

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