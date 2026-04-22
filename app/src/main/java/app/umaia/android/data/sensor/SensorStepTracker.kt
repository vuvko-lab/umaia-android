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
 * Fallback step tracker using the hardware TYPE_STEP_COUNTER sensor.
 * Currently unused — GoogleFitStepTracker is the active implementation.
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

    override suspend fun querySteps(from: java.util.Date, to: java.util.Date): Int = 0
}
