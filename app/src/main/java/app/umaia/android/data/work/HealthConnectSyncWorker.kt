package app.umaia.android.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.umaia.android.data.sensor.HealthConnectStepTracker
import app.umaia.android.data.sensor.StepSubmissionCoordinator
import app.umaia.android.domain.repository.StepRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Periodic HealthConnect → server sync. Approximates iOS's HealthKit
 *  background observer: while the user has Umaia closed, every ~2h
 *  WorkManager wakes, reads today's HC step total, reconciles with the
 *  server's truth via [StepSubmissionCoordinator.reconcileAndSubmit], and
 *  submits any unflushed delta. Without this, an Android user who walks but
 *  doesn't open the app gets 0 Nur for the day until they next launch. */
@HiltWorker
class HealthConnectSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val hcTracker: HealthConnectStepTracker,
    private val stepRepository: StepRepository,
    private val coordinator: StepSubmissionCoordinator,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!hcTracker.isAvailable || !hcTracker.isAuthorized) {
            Log.d(TAG, "HC unavailable/unauthorized — skipping background sync")
            return Result.success()
        }
        return runCatching {
            val current = hcTracker.currentDailySteps()
            val server = runCatching { stepRepository.getTodayServerSteps() }.getOrDefault(0)
            val granted = coordinator.reconcileAndSubmit(
                currentDaily = current,
                knownServerTotal = server,
                source = "hc_background",
                suspectedCheating = false,
            )
            Log.i(TAG, "HC bg sync: current=$current server=$server granted=$granted")
            Result.success()
        }.getOrElse {
            Log.e(TAG, "HC bg sync failed: ${it.message}", it)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "umaia_hc_sync"
        private const val TAG = "UmaiaHCSync"
    }
}
