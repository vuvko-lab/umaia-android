package app.umaia.android.data.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import app.umaia.android.data.local.StepPreferences
import app.umaia.android.domain.repository.StepTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Step tracker backed by the hardware TYPE_STEP_COUNTER sensor.
 * One of two sources merged by [CompositeStepTracker]; the other is Health Connect.
 */
@Singleton
class SensorStepTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepPrefs: StepPreferences
) : StepTracker {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val stepSensor     = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val lastEmitted = AtomicInteger(0)

    override val isAvailable = true

    override val isAuthorized: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        else true

    override suspend fun requestAuthorization(): Boolean = isAuthorized

    override fun observeDailySteps(): Flow<Int> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
                val totalSteps = event.values[0].toInt()
                val today = LocalDate.now().toString()

                if (stepPrefs.baselineDate != today) {
                    stepPrefs.baselineCount = totalSteps
                    stepPrefs.baselineDate  = today
                }

                val baseline = stepPrefs.baselineCount.takeIf { it >= 0 } ?: run {
                    stepPrefs.baselineCount = totalSteps
                    stepPrefs.baselineDate  = today
                    totalSteps
                }

                val daily = maxOf(0, totalSteps - baseline)
                lastEmitted.set(daily)
                stepPrefs.lastActiveDate = today
                // Record cumulative counter so we can compute gap deltas next session
                stepPrefs.lastSensorCumulative = totalSteps
                stepPrefs.lastSensorCumulativeTs = System.currentTimeMillis()
                trySend(daily)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (stepSensor != null) {
            sensorManager?.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        awaitClose { sensorManager?.unregisterListener(listener) }
    }

    override suspend fun currentDailySteps(): Int = lastEmitted.get()

    /**
     * Returns the cumulative-counter delta since [from]. Best-effort:
     *   * Requires a cumulative snapshot recorded at-or-before [from] (last app session).
     *   * Returns 0 if we have no snapshot, or the snapshot post-dates [from] (we have no data
     *     about [from]).
     *   * The current cumulative count is read by registering a one-shot listener with a short
     *     timeout — TYPE_STEP_COUNTER doesn't support direct sync reads.
     */
    override suspend fun querySteps(from: java.util.Date, to: java.util.Date): Int {
        val baselineCumulative = stepPrefs.lastSensorCumulative
        val baselineTs = stepPrefs.lastSensorCumulativeTs
        if (baselineCumulative < 0 || baselineTs == 0L) return 0
        if (baselineTs > from.time) return 0  // Snapshot is newer than [from] — no info
        val current = readCurrentCumulative() ?: return 0
        val delta = current - baselineCumulative
        return if (delta > 0) delta else 0
    }

    private suspend fun readCurrentCumulative(): Int? {
        val sm = sensorManager ?: return null
        val sensor = stepSensor ?: return null
        return kotlinx.coroutines.withTimeoutOrNull(2_000L) {
            kotlinx.coroutines.suspendCancellableCoroutine<Int> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
                        sm.unregisterListener(this)
                        if (cont.isActive) cont.resumeWith(Result.success(event.values[0].toInt()))
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
                cont.invokeOnCancellation { sm.unregisterListener(listener) }
            }
        }
    }
}
