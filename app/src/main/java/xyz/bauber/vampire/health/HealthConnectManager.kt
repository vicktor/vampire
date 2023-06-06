package xyz.bauber.vampire.health

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodGlucoseRecord.Companion.RELATION_TO_MEAL_GENERAL
import androidx.health.connect.client.records.BloodGlucoseRecord.Companion.SPECIMEN_SOURCE_INTERSTITIAL_FLUID
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.records.metadata.Metadata
import xyz.bauber.vampire.BaseApplication
import java.time.Instant
import java.time.ZonedDateTime
import java.util.LinkedList


const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1


class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getWritePermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class)
    )
    var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)
        private set

    init {
        checkAvailability()
    }

    fun checkAvailability() {
        availability.value = when {
            HealthConnectClient.isProviderAvailable(context) -> HealthConnectAvailability.INSTALLED
            isSupported() -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }


    /**
     * Determines whether all the specified permissions are already granted. It is recommended to
     * call [PermissionController.getGrantedPermissions] first in the permissions flow, as if the
     * permissions are already granted then there is no need to request permissions via
     * [PermissionController.createRequestPermissionResultContract].
     */
    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    val healthConnectCompatibleApps by lazy {
        val intent = Intent("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE")

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            )
        }

        packages.associate {
            val icon = try {
                context.packageManager.getApplicationIcon(it.activityInfo.packageName)
            } catch(e: Resources.NotFoundException) {
                null
            }
            val label = context.packageManager.getApplicationLabel(it.activityInfo.applicationInfo)
                .toString()
            it.activityInfo.packageName to
                    HealthConnectAppInfo(
                        packageName = it.activityInfo.packageName,
                        icon = icon,
                        appLabel = label
                    )
        }
    }

    suspend fun revokeAllPermissions(){
        healthConnectClient.permissionController.revokeAllPermissions()
    }

    suspend fun writeGlucose(value: Double, units: String) {
        val time = ZonedDateTime.now().withNano(0)

        Log.d(BaseApplication.TAG, "guardando glucosa  "+value)
        val list = LinkedList<BloodGlucoseRecord>()
        val record = BloodGlucoseRecord(
            time.toInstant(),
            time.offset,
            if (units.equals("mgdl")) BloodGlucose.milligramsPerDeciliter(value) else BloodGlucose.millimolesPerLiter(value),
            SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
            MealType.MEAL_TYPE_UNKNOWN, RELATION_TO_MEAL_GENERAL,
            Metadata()
        )
        list.add(record)

        healthConnectClient.insertRecords(list)
    }

    fun roundToDecimal(number: Double): Double {
        return "%.1f".format(number).replace(",",".").toDouble()
    }

    suspend fun readGlucose(start: Instant, end: Instant): List<BloodGlucoseRecord> {
        val request = ReadRecordsRequest(
            recordType = BloodGlucoseRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    private fun isSupported() = Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK

}

enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}
