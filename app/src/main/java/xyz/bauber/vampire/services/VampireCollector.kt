package xyz.bauber.vampire.services

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import xyz.bauber.vampire.BaseApplication
import xyz.bauber.vampire.BaseApplication.Companion.TAG
import xyz.bauber.vampire.SendBroadcast
import xyz.bauber.vampire.database.DatabaseManager
import xyz.bauber.vampire.database.GlucoseRecord
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit


class VampireCollector : NotificationListenerService() {
    @VisibleForTesting
    var lastPackage: String? = null
    var mContext: Context? = null
    override fun onCreate() {
        super.onCreate()
        mContext = applicationContext
        Log.d(BaseApplication.TAG, "onCreate: NotificationListenerService ")
        isRunning = true

    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        toggleNotificationListenerService()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val fromPackage = sbn.packageName
        Log.d(BaseApplication.TAG, "Notification from: $fromPackage")
        if (coOptedPackages.contains(fromPackage)) {
            Log.d(BaseApplication.TAG, "VALID Notification from: $fromPackage")
            if (sbn.isOngoing) {
                lastPackage = fromPackage
                processNotification(sbn.notification)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
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

    private fun processNotification(notification: Notification?) {
        Log.d(BaseApplication.TAG, "processNotification")
        val content = Notification.Builder.recoverBuilder(mContext, notification).createContentView()

        if (content != null) {
            processContent(content)
        }
    }

    private fun filterString(value: String): String {
        return if (lastPackage == null) value else when (lastPackage) {
            else -> value
                .replace("mmol/L", "")
                .replace("mmol/l", "")
                .replace("mg/dL", "")
                .replace("mg/dl", "")
                .replace("≤", "")
                .replace("≥", "")
                .trim { it <= ' ' }
        }
    }

    private fun processContent(cview: RemoteViews? ) {
        if (cview == null) return
        val applied = cview.apply(this, null)
        val root = applied.rootView as ViewGroup
        val texts = ArrayList<TextView>()
        getTextViews(texts, root)
        Log.d(BaseApplication.TAG, "Text views: " + texts.size)
        var matches = 0
        var mgdl = 0
        for (view in texts) {
            try {
                val tv = view
                val text = if (tv.text != null) tv.text.toString() else ""
                val desc =
                    if (tv.contentDescription != null) tv.contentDescription.toString() else ""
                Log.d(BaseApplication.TAG, "Examining: >$text< : >$desc<")
                val ftext = filterString(text)
                mgdl = ftext.toInt()
                if (mgdl > 0) {
                    matches++
                }
            } catch (e: Exception) {
                //
            }
        }
        if (matches > 1) {
            Log.e(BaseApplication.TAG, "Found too many matches: $matches")
        } else if (matches == 1) {
            Log.d(BaseApplication.TAG, "Found glucose: $mgdl")
            if (mgdl in 30..450) {
                Log.e(BaseApplication.TAG, "glucose reading $mgdl")
                val healthConnectManager = (application as BaseApplication).healthConnectManager
                var databaseManager = mContext?.let { DatabaseManager(it) }


                val time = tsl()
                val lastGlucose = databaseManager?.getLastGlucose()
                current_trend = "UNKNOWN"

                if (lastGlucose != null) {
                    current_trend = getTrend(lastGlucose, time, mgdl)
                }

                Log.d(BaseApplication.TAG, current_trend)

                databaseManager?.insertGlucoseRecord(
                    time,
                    getTimeOffset(),
                    mgdl.toDouble(),
                    "mgdl",
                    "interstitial",
                    current_trend,
                    "Dexcom"
                )

                val sharedPreferences = getSharedPreferences("vampire", Context.MODE_PRIVATE)
                val p = sharedPreferences.getString("shareto", null)

                val bundle = Bundle()
                bundle.putFloat("glucose", mgdl.toFloat())
                bundle.putString("units", "mgdl")
                SendBroadcast.glucose(bundle, p)
                Log.d(TAG, "Glucose sent to $p")

                GlobalScope.launch {
                    if (healthConnectManager.hasAllPermissions(healthConnectManager.permissions)) {
                        healthConnectManager.writeGlucose(mgdl.toDouble(), 1)
                    }
                }
            }
        }
    }

    private fun getTimeOffset(): Long {
        val calendar = Calendar.getInstance()
        val timeZone = TimeZone.getDefault()
        val offsetInMillis = timeZone.getOffset(calendar.timeInMillis)
        val offsetInMinutes = offsetInMillis / (60 * 1000)
        return offsetInMinutes.toLong()

    }

    private fun getTextViews(output: MutableList<TextView>, parent: ViewGroup) {
        val children = parent.childCount
        for (i in 0 until children) {
            val view = parent.getChildAt(i)
            if (view.visibility == View.VISIBLE) {
                if (view is ImageView) {
                    Log.d(BaseApplication.TAG, "hemos encontrado una imagen ID: "+view.id.toString())
/*
                    val drawable = view.drawable
                    val stream = ByteArrayOutputStream()

                    if (drawable is VectorDrawable) {
                        val vectorDrawable = drawable as VectorDrawable
                        // Crea un Bitmap del tamaño correcto
                        val bitmap = Bitmap.createBitmap(
                            vectorDrawable.intrinsicWidth,
                            vectorDrawable.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )

                        // Dibuja el VectorDrawable en el Bitmap
                        val canvas = Canvas(bitmap)
                        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
                        vectorDrawable.draw(canvas)

                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    }

                    if (drawable is BitmapDrawable) {
                        val bitmap = (drawable as BitmapDrawable).bitmap
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    }

                    // Codifica el Bitmap en una matriz de bytes
                    val bitmapBytes = stream.toByteArray()

                    // Calcula el hash SHA-256
                    val digest = MessageDigest.getInstance("SHA-256")
                    val hash = digest.digest(bitmapBytes)

                    // Convierte el hash a una cadena hexadecimal para una fácil visualización
                    val hexHash = hash.joinToString("") { "%02x".format(it) }
                    Log.d(BaseApplication.TAG, "hemos encontrado una imagen SHA: $hexHash")


                    current_trend = "UNKNOWN"


                    if (view.id.equals(2131297229)) {
                        Log.d(BaseApplication.TAG, "TREND: FLAT → ")
                        current_trend = "FLAT"
                    } else {
                        current_trend = view.id.toString()
                    }
*/
                }
                if (view is TextView) {
                    output.add(view)
                } else if (view is ViewGroup) {
                    getTextViews(output, view)
                }
            }
        }

    }

    private fun getTrend(lastGlucose: GlucoseRecord, time: Long, mgdl: Int) : String {
        var trend = "UNKNOWN"
        /*
            1 - DOUBLE_UP ↑↑        increasing >3 mg/dL/min
            2 - SINGLE_UP ↑         increasing 2-3 mg/dL/min
            3 - UP_45 ↗             increasing 1-2 mg/dL/min
            4 - FLAT →              not increasing/decreasing > 1 mg/dL/min
            5 - DOWN_45 ↘           decreasing 1-2 mg/dL/min
            6 - SINGLE_DOWN ↓       decreasing 2-3 mg/dL/min
            7 - DOUBLE_DOWN ↓↓      decreasing >3 mg/dL/min
            */

        Log.d(BaseApplication.TAG, "Last glucose: "+lastGlucose.glucoseValue)
        Log.d(BaseApplication.TAG, "Current glucose: "+mgdl)
        if (lastGlucose != null) {
            val time_elapsed = time - lastGlucose.timestamp
            val minutes = TimeUnit.MILLISECONDS.toMinutes(time_elapsed).toInt()
            val diff = mgdl - lastGlucose.glucoseValue
            if (minutes in 1..6) {
                val slope = diff / 5

                Log.d(BaseApplication.TAG, "minutes: " + minutes)
                Log.d(BaseApplication.TAG, "diff glucose: " + diff)
                Log.d(BaseApplication.TAG, "slope glucose: " + slope)
                if (minutes < 6) {
                    if (slope > 3.3) trend = "DOUBLE_UP"
                    if (slope in 2.3..3.3) trend = "SINGLE_UP"
                    if (slope in 1.4..2.2) trend = "UP_45"
                    if (slope in -1.3..1.3) trend = "FLAT"
                    if (slope in -2.2..-1.4) trend = "DOWN_45"
                    if (slope in -3.3..-2.3) trend = "SINGLE_DOWN"
                    if (slope < -3.3) trend = "DOUBLE_DOWN"
                }
            } else {
                trend = "FLAT"
            }
        }
        return trend
    }

    private fun tsl(): Long {
        return System.currentTimeMillis()
    }

    companion object {
        private val coOptedPackages = HashSet<String>()

        var isRunning = false
        var current_trend = "unknown"
        init {
            coOptedPackages.add("com.dexcom.g6")
            coOptedPackages.add("com.dexcom.g6.region1.mmol")
            coOptedPackages.add("com.dexcom.g6.region3.mgdl")
            coOptedPackages.add("com.dexcom.dexcomone")
            coOptedPackages.add("com.dexcom.g7")
            coOptedPackages.add("com.camdiab.fx_alert.mmoll")
            coOptedPackages.add("com.camdiab.fx_alert.mgdl")
            coOptedPackages.add("com.camdiab.fx_alert.hx.mmoll")
            coOptedPackages.add("com.camdiab.fx_alert.hx.mgdl")
            coOptedPackages.add("com.medtronic.diabetes.guardian")
            coOptedPackages.add("com.medtronic.diabetes.minimedmobile.eu")
            coOptedPackages.add("com.medtronic.diabetes.minimedmobile.us")
            coOptedPackages.add("com.freestylelibre3.app")
        }



    }


}