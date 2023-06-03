package xyz.bauber.vampire

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.getValue
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.bauber.vampire.database.DatabaseManager
import xyz.bauber.vampire.database.GlucoseRecord
import xyz.bauber.vampire.health.HealthConnectAvailability
import xyz.bauber.vampire.health.HealthConnectManager
import xyz.bauber.vampire.services.CheckService
import xyz.bauber.vampire.services.VampireCollector
import xyz.bauber.vampire.webserver.WebServer
import java.util.concurrent.TimeUnit

@OptIn(DelicateCoroutinesApi::class)
class MainActivity : ComponentActivity() {

    private lateinit var server: WebServer
    private lateinit var healthConnectManager: HealthConnectManager
    val HEALTH_CONNECT_RESPONSE_ID = 10000



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        healthConnectManager = (application as BaseApplication).healthConnectManager

        findViewById<Button>(R.id.bPermission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        GlobalScope.launch {
            if (healthConnectManager.hasAllPermissions(healthConnectManager.permissions)) {
                // run main thread
                withContext(Dispatchers.Main) {
                    findViewById<Switch>(R.id.hconnect).isChecked = true
                }
            }
        }

        findViewById<Switch>(R.id.hconnect).setOnClickListener {
            toggleHC(findViewById<Switch>(R.id.hconnect).isChecked)
        }

        val cn = ComponentName(this, VampireCollector::class.java)
        val flat = Settings.Secure.getString(
            this.getContentResolver(),
            "enabled_notification_listeners"
        )
        val enabled = flat != null && flat.contains(cn.flattenToString())
        if (!enabled) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        checkBattery()

        server = WebServer()
        try {
            server.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        startJobScheduler()

    }

    fun startJobScheduler() {
//        if (!isJobServiceOn()) {
            val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val jobInfo = JobInfo.Builder(
                BaseApplication.JOB_ID,
                ComponentName(this, CheckService::class.java)
            )
                .setPeriodic(15 * 60 * 1000)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()

            jobScheduler.schedule(jobInfo)
//        }
    }

    fun isJobServiceOn(): Boolean {
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        var hasBeenScheduled = false

        for (jobInfo in jobScheduler.allPendingJobs) {
            if (jobInfo.id == BaseApplication.JOB_ID) {
                hasBeenScheduled = true
                break
            }
        }

        return hasBeenScheduled
    }

    private fun toggleHC(checked: Boolean) {
        when(checked) {
            true -> {
                val availability by healthConnectManager.availability

                if (availability == HealthConnectAvailability.INSTALLED) {
                    GlobalScope.launch {
                        healthConnectManager.hasAllPermissions(healthConnectManager.permissions)
                        healthConnectManager.healthConnectCompatibleApps

                        GlobalScope.launch {
                            if (!healthConnectManager.hasAllPermissions(healthConnectManager.permissions)) {
                                Log.d(BaseApplication.TAG, "no hay permisos concedidos")
                                val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()
                                permissionsLauncher.createIntent(application, healthConnectManager.permissions)
                                requestPermissionsActivityContract()
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        this, R.string.hc_notavailable, Toast.LENGTH_SHORT
                    ).show()
                    val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                    val gpIntent = Intent(Intent.ACTION_VIEW, uri)
                    this.startActivity(gpIntent)
                }

                val permsIntent = PermissionController.createRequestPermissionResultContract().createIntent(this, healthConnectManager.permissions);
                startActivityForResult(permsIntent, HEALTH_CONNECT_RESPONSE_ID);
            }
            false -> {
                GlobalScope.launch {
                    healthConnectManager.revokeAllPermissions()
                }
            }
        }
    }

    fun checkBattery() {
        if (!isIgnoringBatteryOptimizations()) {
            val name = resources.getString(R.string.app_name)
            Toast.makeText(applicationContext, "Battery optimization -> All apps -> $name -> Don't optimize", Toast.LENGTH_LONG).show()

            //val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            //startActivity(intent)
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } else {
            Log.d(BaseApplication.TAG, "Se ignoran las restricciones de bateria")
        }
    }

    /**
     * return true if in App's Battery settings "Not optimized" and false if "Optimizing battery use"
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val pwrm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val name = packageName
        return pwrm.isIgnoringBatteryOptimizations(name)
    }

    override fun onResume() {
        super.onResume()

        val databaseManager = DatabaseManager(this)

        val lastGlucosa : GlucoseRecord? = databaseManager.getLastGlucose()

        if (lastGlucosa != null) {
            if (lastGlucosa.glucoseUnits.equals("mgdl")) {
                findViewById<TextView>(R.id.glucose).text =
                    lastGlucosa.glucoseValue?.toInt().toString()
                findViewById<TextView>(R.id.units).text = getString(R.string.mgdl)

            } else {
                findViewById<TextView>(R.id.glucose).text = lastGlucosa.glucoseValue.toString()
                findViewById<TextView>(R.id.units).text = getString(R.string.mmol)
            }

            val trend = when(lastGlucosa.trend) {
                "DOUBLE_UP" -> "↑↑"
                "SINGLE_UP" -> "↑"
                "UP_45" -> "↗"
                "FLAT" -> "→"
                "DOWN_45" -> "↘"
                "SINGLE_DOWN" -> "↓"
                "DOUBLE_DOWN" -> "↓↓"
                else -> "?"
            }

            findViewById<TextView>(R.id.trend).text = trend
            findViewById<TextView>(R.id.fecha).text = getReadableTimeDiff(lastGlucosa.timestamp)

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }


    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        val healthConnectManager = (application as BaseApplication).healthConnectManager
        return PermissionController.createRequestPermissionResultContract()
    }


    fun getReadableTimeDiff(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        if (days > 0) return "$days ${getString(R.string.days)}"

        val hours = TimeUnit.MILLISECONDS.toHours(diff).toInt()
        if (hours == 1) return "$hours ${getString(R.string.hour)}" else if (hours > 1) return "$hours ${getString(R.string.hours)}"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
        if (minutes == 1) return "$minutes ${getString(R.string.minute)}" else if (minutes > 1) return "$minutes ${getString(R.string.minutes)}"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        return "$seconds ${getString(R.string.seconds)}"
    }


}