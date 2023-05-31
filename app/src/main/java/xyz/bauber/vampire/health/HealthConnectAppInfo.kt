package xyz.bauber.vampire.health

import android.graphics.drawable.Drawable

/**
 * Information about an app that can be used for displaying attribution.
 */
data class HealthConnectAppInfo(
    val packageName: String,
    val appLabel: String,
    val icon: Drawable?
)