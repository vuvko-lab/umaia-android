package app.umaia.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.umaia.android.data.local.AppPreferences
import app.umaia.android.data.work.DailySummaryWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class UmaiaApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var appPreferences: AppPreferences

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appPreferences.applyLocale()
        scheduleDailySummary()
    }

    /** Mirrors iOS `NotificationService.scheduleDailySummary` — daily 20:00
     *  Asia/Almaty, repeating, idempotent across launches via KEEP. */
    private fun scheduleDailySummary() {
        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(msUntilNext20InAlmaty(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailySummaryWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun msUntilNext20InAlmaty(): Long {
        val zone = TimeZone.getTimeZone("Asia/Almaty")
        val now = Calendar.getInstance(zone)
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
