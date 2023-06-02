package xyz.bauber.vampire.services

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import xyz.bauber.vampire.BaseApplication

class StartOnBoot : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent!!.action) {

            if (!isJobServiceOn(context!!)) {
                val jobScheduler =
                    context!!.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

                val jobInfo = JobInfo.Builder(0, BaseApplication.serviceChecker!!)
                    .setPeriodic(15 * 60 * 1000)
                    .setRequiresCharging(false)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build()

                jobScheduler.schedule(jobInfo)
            }
        }
    }

    fun isJobServiceOn(context: Context): Boolean {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        var hasBeenScheduled = false

        for (jobInfo in jobScheduler.allPendingJobs) {
            if (jobInfo.id == BaseApplication.JOB_ID) {
                hasBeenScheduled = true
                break
            }
        }

        return hasBeenScheduled
    }

}