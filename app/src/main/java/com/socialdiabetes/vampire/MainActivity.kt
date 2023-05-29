package com.socialdiabetes.vampire

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.health.connect.client.HealthConnectClient

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        val healthConnectManager = HealthConnectManager(this)
        val availability by healthConnectManager.availability
        if (availability == HealthConnectClient.SDK_AVAILABLE) {
            Toast.makeText(this, "health connect manager disponbible", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "health connect manager NO disponbible", Toast.LENGTH_LONG).show()
        }

    }
}