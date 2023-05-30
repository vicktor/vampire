package com.socialdiabetes.vampire.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
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
import com.socialdiabetes.vampire.BaseApplication
import com.socialdiabetes.vampire.DatabaseManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Calendar
import java.util.TimeZone

class VampireCollector : NotificationListenerService() {
    @VisibleForTesting
    var lastPackage: String? = null
    override fun onCreate() {
        super.onCreate()
        mContext = applicationContext
        Log.d("vampire", "onCreate: NotificationListenerService ")
        isRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val fromPackage = sbn.packageName
        Log.d(TAG, "Notification from: $fromPackage")
        if (coOptedPackages.contains(fromPackage)) {
            Log.d(TAG, "VALID Notification from: $fromPackage")
            if (sbn.isOngoing) {
                lastPackage = fromPackage
                processNotification(sbn.notification)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        //
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    private fun processNotification(notification: Notification?) {
        Log.d(TAG, "processNotification")
        val content = Notification.Builder.recoverBuilder(mContext, notification).createContentView()

        if (notification?.contentView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val cid = notification.channelId
            }
            processRemote(notification.contentView)
        }
    }

    fun filterString(value: String): String {
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

    private fun processRemote(cview: RemoteViews?) {
        Log.d(TAG, "processRemote")
        if (cview == null) return
        val applied = cview.apply(this, null)
        val root = applied.rootView as ViewGroup
        val texts = ArrayList<TextView>()
        getTextViews(texts, root)
        Log.d(TAG, "Text views: " + texts.size)
        var matches = 0
        var mgdl = 0
        for (view in texts) {
            try {
                val tv = view
                val text = if (tv.text != null) tv.text.toString() else ""
                val desc =
                    if (tv.contentDescription != null) tv.contentDescription.toString() else ""
                Log.d(TAG, "Examining: >$text< : >$desc<")
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
            Log.e(TAG, "Found too many matches: $matches")
        } else if (matches == 1) {
            Log.d(TAG, "Found glucose: $mgdl")
            if (mgdl in 30..405) {
                Log.e(TAG, "glucose reading $mgdl")
                val healthConnectManager = (application as BaseApplication).healthConnectManager
                var databaseManager = mContext?.let { DatabaseManager(it) }

                databaseManager?.insertGlucoseRecord(
                    tsl(),
                    getTimeOffset(),
                    mgdl.toDouble(),
                    "mgdl",
                    "interstitial",
                    current_trend,
                    "Dexcom"
                )

                GlobalScope.launch {
                    healthConnectManager.writeGlucose(mgdl.toDouble(), 1)
                }

            } else {
                Log.wtf(TAG, "Glucose value outside acceptable range: $mgdl")
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
                    Log.d(TAG, "hemos encontrado una imagen ID: "+view.id.toString())

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
                    Log.d(TAG, "hemos encontrado una imagen SHA: $hexHash")

                    /*
                    1 - DOUBLE_UP ↑↑
                    2 - SINGLE_UP ↑
                    3 - UP_45 ↗
                    4 - FLAT →
                    5 - DOWN_45 ↘
                    6 - SINGLE_DOWN ↓
                    7 - DOUBLE_DOWN ↓↓
                    */

                    current_trend = "UNKNOWN"

                    if (view.id.equals(2131297229)) {
                        Log.d(TAG, "TREND: FLAT → ")
                        current_trend = "FLAT"
                    } else {
                        current_trend = view.id.toString()
                    }

                }
                if (view is TextView) {
                    output.add(view)
                } else if (view is ViewGroup) {
                    getTextViews(output, view)
                }
            }
        }

    }

    companion object {
        private const val TAG = "vampire"
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
        }

        var mContext: Context? = null

        private fun tsl(): Long {
            return System.currentTimeMillis()
        }
    }
}