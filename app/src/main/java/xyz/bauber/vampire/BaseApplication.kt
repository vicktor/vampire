package xyz.bauber.vampire

import android.app.ActivityManager
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import xyz.bauber.vampire.health.HealthConnectManager
import xyz.bauber.vampire.services.CheckService
import xyz.bauber.vampire.webserver.WebServer

class BaseApplication : Application() {
    val healthConnectManager by lazy {
        HealthConnectManager(this)
    }


    override fun onCreate() {
        super.onCreate()
        instance = this
        serviceChecker = ComponentName(this, CheckService::class.java)

    }

    companion object {
        const val TAG = "vampire"
        const val JOB_ID = 1970
        var serviceChecker: ComponentName? = null
        var server: WebServer? = null

        lateinit var instance: BaseApplication
            private set
    }
}