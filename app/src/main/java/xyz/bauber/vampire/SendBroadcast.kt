package xyz.bauber.vampire
import android.content.Intent
import android.os.Bundle

object SendBroadcast {
    private val SEND_GLUCOSE = "xyz.bauber.vampire.SEND_GLUCOSE"

    fun glucose( bundle: Bundle?, packageName: String?) {

        Intent().also { intent ->
            intent.action = SEND_GLUCOSE
            if (packageName != null) intent.setPackage(packageName.trim())
            if (bundle != null) intent.putExtras(bundle)
            BaseApplication.instance.sendBroadcast(intent)
        }
    }

}