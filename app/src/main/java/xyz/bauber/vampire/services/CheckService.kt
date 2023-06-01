package xyz.bauber.vampire.services

import android.app.ActivityManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import xyz.bauber.vampire.BaseApplication


class CheckService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Log.d(BaseApplication.TAG, "[CheckService] onStartJob")
        val isRunning = isServiceRunning(VampireCollector::class.java)

        if (!isRunning) {
            Log.d(BaseApplication.TAG, "[CheckService] el servicio VampireCollector esta parado, reiniciar")
            toggleNotificationListenerService()
        } else {
            Log.d(BaseApplication.TAG, "[CheckService] el servicio VampireCollector esta ejecutandose")
        }

        return false
    }

    private fun toggleNotificationListenerService() {
        val pm = packageManager
        pm.setComponentEnabledSetting(
            ComponentName(
                this,
                VampireCollector::class.java
            ),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            ComponentName(
                this,
                VampireCollector::class.java
            ),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // Devolver true si la tarea debe reprogramarse
        Log.d(BaseApplication.TAG, "[CheckService] reprogramamos el job para ejecutarse de nuevo")
        return true
    }

    @Suppress("DEPRECATION")
    fun <T> isServiceRunning(service: Class<T>): Boolean {
        return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any { it -> it.service.className == service.name }
    }
}
