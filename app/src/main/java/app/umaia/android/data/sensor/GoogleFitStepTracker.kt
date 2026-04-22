package app.umaia.android.data.sensor

import android.content.Context
import android.util.Log
import app.umaia.android.domain.repository.StepTracker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleFitStepTracker @Inject constructor(
    @ApplicationContext private val context: Context
) : StepTracker {

    val fitnessOptions: FitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .build()

    private var subscribed = false

    override val isAvailable = true

    override val isAuthorized: Boolean
        get() {
            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            return GoogleSignIn.hasPermissions(account, fitnessOptions)
        }

    override suspend fun requestAuthorization(): Boolean = isAuthorized

    /**
     * Subscribe to step recording so Google Fit accumulates step data
     * from the hardware sensor in the background.
     */
    suspend fun ensureSubscribed() {
        if (subscribed || !isAuthorized) return
        try {
            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            Fitness.getRecordingClient(context, account)
                .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
                .await()
            subscribed = true
            Log.d(TAG, "Subscribed to step recording")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to step recording", e)
        }
    }

    /**
     * Polls Google Fit History API every 5 seconds for today's step total.
     * Unlike the hardware sensor approach, this captures ALL steps taken today
     * regardless of whether the app was open.
     */
    override fun observeDailySteps(): Flow<Int> = flow {
        ensureSubscribed()
        while (true) {
            val steps = readTodaySteps()
            emit(steps)
            delay(5_000L)
        }
    }

    override suspend fun currentDailySteps(): Int = readTodaySteps()

    override suspend fun querySteps(from: Date, to: Date): Int {
        if (!isAuthorized) return 0
        return try {
            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            val request = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(from.time, to.time, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(request)
                .await()

            response.buckets.sumOf { bucket ->
                bucket.dataSets.sumOf { dataSet ->
                    dataSet.dataPoints.sumOf { it.getValue(Field.FIELD_STEPS).asInt() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "querySteps failed", e)
            0
        }
    }

    private suspend fun readTodaySteps(): Int {
        if (!isAuthorized) return 0
        return try {
            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = cal.timeInMillis
            val endTime = System.currentTimeMillis()

            val request = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(request)
                .await()

            val steps = response.buckets.sumOf { bucket ->
                bucket.dataSets.sumOf { dataSet ->
                    dataSet.dataPoints.sumOf { it.getValue(Field.FIELD_STEPS).asInt() }
                }
            }
            Log.d(TAG, "readTodaySteps: $steps")
            steps
        } catch (e: Exception) {
            Log.e(TAG, "readTodaySteps failed", e)
            0
        }
    }

    companion object {
        private const val TAG = "GoogleFitStepTracker"
    }
}
