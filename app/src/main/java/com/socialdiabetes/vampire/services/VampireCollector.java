package com.socialdiabetes.vampire.services;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.socialdiabetes.vampire.HealthConnectManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * JamOrHam
 * UI Based Collector
 */

public class VampireCollector extends NotificationListenerService {

    private static final String TAG = "vampire"; // UiBasedCollector.class.getSimpleName();
    private static final String UI_BASED_STORE_LAST_VALUE = "UI_BASED_STORE_LAST_VALUE";
    private static final String UI_BASED_STORE_LAST_REPEAT = "UI_BASED_STORE_LAST_REPEAT";
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private static final HashSet<String> coOptedPackages = new HashSet<>();

    @VisibleForTesting
    String lastPackage;

    static {
        coOptedPackages.add("com.dexcom.g6");
        coOptedPackages.add("com.dexcom.g6.region1.mmol");
        coOptedPackages.add("com.dexcom.g6.region3.mgdl");
        coOptedPackages.add("com.dexcom.dexcomone");
        coOptedPackages.add("com.dexcom.g7");
        coOptedPackages.add("com.camdiab.fx_alert.mmoll");
        coOptedPackages.add("com.camdiab.fx_alert.mgdl");
        coOptedPackages.add("com.camdiab.fx_alert.hx.mmoll");
        coOptedPackages.add("com.camdiab.fx_alert.hx.mgdl");
        coOptedPackages.add("com.medtronic.diabetes.guardian");
        coOptedPackages.add("com.medtronic.diabetes.minimedmobile.eu");
        coOptedPackages.add("com.medtronic.diabetes.minimedmobile.us");
    }
    public static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        Log.d("vampire", "onCreate UiBasedCollector: NotificationListenerService ");
    }
    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        String fromPackage = sbn.getPackageName();
        Log.d(TAG, "Notification from: " + fromPackage);
        if (coOptedPackages.contains(fromPackage)) {
            Log.d(TAG, "VALID Notification from: " + fromPackage);
            if (sbn.isOngoing()) {
                Log.d(TAG, "is On going");
                lastPackage = fromPackage;
                processNotification(sbn.getNotification());
            }
        }
    }

    @Override
    public void onNotificationRemoved(final StatusBarNotification sbn) {
        //
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    private void processNotification(final Notification notification) {
        Log.d(TAG, "processNotification");
        if (notification == null) {
            Log.e(TAG, "Null notification");
            return;
        }
        if (notification.contentView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String cid = notification.getChannelId();
                Log.d(TAG, "Channel ID: " + cid);
            }
            processRemote(notification.contentView);
        } else {
            Log.e(TAG, "Content is empty");
        }
    }

    String filterString(final String value) {
        if (lastPackage == null) return value;
        switch (lastPackage) {
            default:
                return value
                        .replace("mmol/L", "")
                        .replace("mmol/l", "")
                        .replace("mg/dL", "")
                        .replace("mg/dl", "")
                        .replace("≤", "")
                        .replace("≥", "")
                        .trim();
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void processRemote(final RemoteViews cview) {
        Log.d(TAG, "processRemote");
        if (cview == null) return;
        View applied = cview.apply(this, null);
        ViewGroup root = (ViewGroup) applied.getRootView();
        ArrayList<TextView> texts = new ArrayList<TextView>();
        getTextViews(texts, root);
        Log.d(TAG, "Text views: " + texts.size());
        int matches = 0;
        int mgdl = 0;
        for (TextView view : texts) {
            try {
                TextView tv = (TextView) view;
                String text = tv.getText() != null ? tv.getText().toString() : "";
                String desc = tv.getContentDescription() != null ? tv.getContentDescription().toString() : "";
                Log.d(TAG, "Examining: >" + text + "< : >" + desc + "<");
                String ftext = filterString(text);
                    mgdl = Integer.parseInt(ftext);
                    if (mgdl > 0) {
                        matches++;
                    }
            } catch (Exception e) {
                //
            }
        }
        if (matches == 0) {
            Log.e(TAG, "Did not find any matches");
        } else if (matches > 1) {
            Log.e(TAG, "Found too many matches: " + matches);
        } else {


            // HealthConnectManager healthConnectManager = (mContext)BaseApplication.healthConnectManager;

            // Sensor.createDefaultIfMissing();
            Long timestamp = tsl();
            Log.d(TAG, "Found specific value: " + mgdl);

            if ((mgdl >= 40 && mgdl <= 405)) {
                Log.e("vampire", "glucose reading "+mgdl);



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
                Log.wtf(TAG, "Glucose value outside acceptable range: " + mgdl);
            }
        }
        //texts.clear();
    }



    private static long tsl() {
        return System.currentTimeMillis();
    }

    /*

    public static long msSince(long when) {
        return (tsl() - when);
    }

    static boolean isValidMmol(final String text) {
        return text.matches("[0-9]+[.,][0-9]+");
    }

    private boolean isJammed(final int mgdl) {
        long previousValue = PersistentStore.getLong(mContext, UI_BASED_STORE_LAST_VALUE);
        if (previousValue == mgdl) {
            PersistentStore.incrementLong(mContext, UI_BASED_STORE_LAST_REPEAT);
        } else {
            PersistentStore.setLong(mContext, UI_BASED_STORE_LAST_REPEAT, 0);
        }
        Long lastRepeat = PersistentStore.getLong(mContext, UI_BASED_STORE_LAST_REPEAT);
        Log.d(TAG, "Last repeat: " + lastRepeat);
        return lastRepeat > 3;
    }

     */

    private void getTextViews(final List<TextView> output, final ViewGroup parent) {
        int children = parent.getChildCount();
        for (int i = 0; i < children; i++) {
            View view = parent.getChildAt(i);
            if (view.getVisibility() == View.VISIBLE) {
                if (view instanceof ImageView) {
                    Log.d("vampire", "hemos encontrado una imagen");
                    Log.d("vampimre", ((ImageView) view).toString());
                }
                if (view instanceof TextView) {
                    output.add((TextView) view);
                } else if (view instanceof ViewGroup) {
                    getTextViews(output, (ViewGroup) view);
                }
            }
        }
    }
/*
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
}