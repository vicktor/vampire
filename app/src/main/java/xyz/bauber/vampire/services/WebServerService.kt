package xyz.bauber.vampire.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import xyz.bauber.vampire.BaseApplication
import xyz.bauber.vampire.R
import xyz.bauber.vampire.webserver.WebServer


class WebServerService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.webserver))
            .setContentText(getString(R.string.ws_running))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(NOTIFICATION_ID, notification)


        if (BaseApplication.server == null) {
            BaseApplication.server = WebServer()
            BaseApplication.server!!.start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        if (BaseApplication.server!!.isAlive)
            BaseApplication.server?.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.webserver)
            val descriptionText = getString(R.string.ws_running)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "VampireWebServerHttpChannel"
        const val NOTIFICATION_ID = 19701506
    }
}
