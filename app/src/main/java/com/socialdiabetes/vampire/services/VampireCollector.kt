package com.socialdiabetes.vampire.services

import android.app.Notification
import android.content.Context
import android.content.Intent
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
import java.util.Calendar
import java.util.TimeZone

class VampireCollector : NotificationListenerService() {
    @VisibleForTesting
    var lastPackage: String? = null
    override fun onCreate() {
        super.onCreate()
        mContext = applicationContext
        Log.d("vampire", "onCreate: NotificationListenerService ")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val fromPackage = sbn.packageName
        Log.d(TAG, "Notification from: $fromPackage")
        if (coOptedPackages.contains(fromPackage)) {
            Log.d(TAG, "VALID Notification from: $fromPackage")
            if (sbn.isOngoing) {
                Log.d(TAG, "is On going")
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
        if (notification == null) {
            Log.e(TAG, "Null notification")
            return
        }
        if (notification.contentView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val cid = notification.channelId
                Log.d(TAG, "Channel ID: $cid")
            }
            processRemote(notification.contentView)
        } else {
            Log.e(TAG, "Content is empty")
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
        if (matches == 0) {
            Log.e(TAG, "Did not find any matches")
        } else if (matches > 1) {
            Log.e(TAG, "Found too many matches: $matches")
        } else {


            // HealthConnectManager healthConnectManager = (mContext)BaseApplication.healthConnectManager;

            // Sensor.createDefaultIfMissing();
            val timestamp = tsl()
            Log.d(TAG, "Found specific value: $mgdl")
            if (mgdl >= 40 && mgdl <= 405) {
                Log.e("vampire", "glucose reading $mgdl")
                val healthConnectManager = (application as BaseApplication).healthConnectManager
                var databaseManager = mContext?.let { DatabaseManager(it) }

                databaseManager?.insertGlucoseRecord(
                    tsl(),
                    getTimeOffset(),
                    mgdl.toDouble(),
                    "mgdl",
                    "interstitial",
                    "flat",
                    "Dexcom"
                )

                GlobalScope.launch {
                    healthConnectManager.writeGlucose(mgdl.toDouble(), 1)
                }


                /*
                val grace = DexCollectionType.getCurrentSamplePeriod() * 4;
                val recent = msSince(lastReadingTimestamp) < grace;
                val period = recent ? grace : DexCollectionType.getCurrentDeduplicationPeriod();
                if (BgReading.getForPreciseTimestamp(timestamp, period, false) == null) {
                    if (isJammed(mgdl)) {
                        Log.wtf(TAG, "Apparently value is jammed at: " + mgdl);
                    } else {
                        Log.d(TAG, "Inserting new value");
                        PersistentStore.setLong(mContext, UI_BASED_STORE_LAST_VALUE, mgdl);
                        boolean bgr = BgReading.bgReadingInsertFromG5(mgdl, timestamp);
                        if (bgr != null) {
                            bgr.find_slope();
                            bgr.noRawWillBeAvailable();
                            bgr.injectDisplayGlucose(BestGlucose.getDisplayGlucose());
                        }
                    }


                } else {
                    Log.d(TAG, "Duplicate value");
                }
            */
            } else {
                Log.wtf(TAG, "Glucose value outside acceptable range: $mgdl")
            }
        }
        //texts.clear();
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
                    Log.d("vampire", "hemos encontrado una imagen")
                    Log.d("vampire", view.toString())
                }
                if (view is TextView) {
                    output.add(view)
                } else if (view is ViewGroup) {
                    getTextViews(output, view)
                }
            }
        }

    } /*
    public static void onEnableCheckPermission(final Activity activity) {
        if (DexCollectionType.getDexCollectionType() == UiBased) {
            Log.d(TAG, "Detected that we are enabled");
            switchToAndEnable(activity);
        }
    }

    public static SharedPreferences.OnSharedPreferenceChangeListener getListener(final Activity activity) {
        return (prefs, key) -> {
            if (key.equals(DexCollectionType.DEX_COLLECTION_METHOD)) {
                try {
                    onEnableCheckPermission(activity);
                } catch (Exception e) {
                    //
                }
            }
        };
    }

    public static void switchToAndEnable(final Activity activity) {
        DexCollectionType.setDexCollectionType(UiBased);
        Sensor.createDefaultIfMissing();
        if (!isNotificationServiceEnabled()) {
            JoH.show_ok_dialog(activity, gs(R.string.please_allow_permission),
                    "Permission is needed to receive data from other applications. xDrip does not do anything beyond this scope. Please enable xDrip on the next screen",
                    () -> activity.startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        }
    }

    private static boolean isNotificationServiceEnabled() {
        String pkgName = mContext.getPackageName();
        String flat = Settings.Secure.getString(mContext.getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    */

    companion object {
        private const val TAG = "vampire"
        private const val UI_BASED_STORE_LAST_VALUE = "UI_BASED_STORE_LAST_VALUE"
        private const val UI_BASED_STORE_LAST_REPEAT = "UI_BASED_STORE_LAST_REPEAT"
        private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
        private const val ACTION_NOTIFICATION_LISTENER_SETTINGS =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
        private val coOptedPackages = HashSet<String>()

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