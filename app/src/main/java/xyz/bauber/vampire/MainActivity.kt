package xyz.bauber.vampire

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.getValue
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import xyz.bauber.vampire.database.DatabaseManager
import xyz.bauber.vampire.health.HealthConnectAvailability
import xyz.bauber.vampire.services.VampireCollector
import xyz.bauber.vampire.webserver.WebServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import xyz.bauber.vampire.database.GlucoseRecord
import java.text.SimpleDateFormat

import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var server: WebServer

    val HEALTH_CONNECT_RESPONSE_ID = 10000

    val permissions = setOf(
        HealthPermission.getWritePermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class)
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val healthConnectManager = (application as BaseApplication).healthConnectManager

        val availability by healthConnectManager.availability
        val cn = ComponentName(this, VampireCollector::class.java)
        val flat = Settings.Secure.getString(
            this.getContentResolver(),
            "enabled_notification_listeners"
        )
        val enabled = flat != null && flat.contains(cn.flattenToString())
        if (!enabled) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        if (availability == HealthConnectAvailability.INSTALLED) {
            Toast.makeText(this, "health connect manager disponbible", Toast.LENGTH_LONG).show()
            GlobalScope.launch {
                healthConnectManager.hasAllPermissions(permissions)
                healthConnectManager.healthConnectCompatibleApps

                GlobalScope.launch {
                    if (!healthConnectManager.hasAllPermissions(permissions)) {
                        Log.d("vampire", "no hay permisos concedidos")
                        val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()
                        permissionsLauncher.createIntent(application, permissions)
                        requestPermissionsActivityContract()
                    }
                }
            }
        } else {
            Toast.makeText(this, "health connect manager NO disponbible", Toast.LENGTH_LONG).show()
            Toast.makeText(
                this, "Health Connect is not available", Toast.LENGTH_SHORT
            ).show()
            val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata")
            val gpIntent = Intent(Intent.ACTION_VIEW, uri)
            this.startActivity(gpIntent)
        }

        val permsIntent = PermissionController.createRequestPermissionResultContract().createIntent(this, permissions);
        startActivityForResult(permsIntent, HEALTH_CONNECT_RESPONSE_ID);


        server = WebServer()
        try {
            server.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onResume() {
        super.onResume()

        val databaseManager = DatabaseManager(this)

        val lastGlucosa : GlucoseRecord? = databaseManager.getLastGlucose()

        if (lastGlucosa != null) {
            if (lastGlucosa.glucoseUnits.equals("mgdl")) {
                findViewById<TextView>(R.id.glucose).text =
                    lastGlucosa.glucoseValue?.toInt().toString()
                findViewById<TextView>(R.id.units).text = "mg/dL"

            } else {
                findViewById<TextView>(R.id.glucose).text = lastGlucosa.glucoseValue.toString()
                findViewById<TextView>(R.id.units).text = "mmol/L"
            }
            val sdf = SimpleDateFormat("dd-MM HH:mm", Locale.getDefault())

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
        if (days > 0) return "$days día(s)"

        val hours = TimeUnit.MILLISECONDS.toHours(diff).toInt()
        if (hours == 1) return "$hours hora" else if (hours > 1) return "$hours horas"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
        if (minutes == 1) return "$minutes minuto" else if (minutes > 1) return "$minutes minutos"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        return "$seconds segundos"
    }
}