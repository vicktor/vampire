package xyz.bauber.vampire.services

import android.app.ActivityManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.util.Log

class CheckService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Log.d("vampire", "onStartJob")
        val isRunning = isServiceRunning(VampireCollector::class.java)

        if (!isRunning) {
            Log.d("vampire", "[CheckService] el servicio VampireCollector esta parado, reiniciar")
            val intent = Intent(this, VampireCollector::class.java)
            startService(intent)
        } else {
            Log.d("vampire", "[CheckService] el servicio VampireCollector esta ejecutandose")
        }

        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // Devuelve true si la tarea debe reprogramarse
        Log.d("vampire", "[CheckService] programamos el job para ejecutarse de nuevo")
        return true
    }

    @Suppress("DEPRECATION")
    fun <T> isServiceRunning(service: Class<T>): Boolean {
        return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any { it -> it.service.className == service.name }
    }
}
