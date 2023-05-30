package com.socialdiabetes.vampire

import android.app.Activity
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
import com.socialdiabetes.vampire.services.UiBasedCollector
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

import java.util.Locale

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
        val cn = ComponentName(this, UiBasedCollector::class.java)
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

        val last_glucosa = databaseManager.getLastGlucose()

        if (last_glucosa!! != null) {
            if (last_glucosa?.glucoseUnits.equals("mgdl")) {
                findViewById<TextView>(R.id.glucose).text =
                    last_glucosa?.glucoseValue?.toInt().toString()
                findViewById<TextView>(R.id.units).text = "mg/dL"

            } else {
                findViewById<TextView>(R.id.glucose).text = last_glucosa?.glucoseValue.toString()
                findViewById<TextView>(R.id.units).text = "mmol"
            }
            val sdf = SimpleDateFormat("dd-MM HH:mm", Locale.getDefault())

            findViewById<TextView>(R.id.fecha).text = sdf.format(last_glucosa.timestamp)
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
}