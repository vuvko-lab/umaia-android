package app.umaia.android.data.sensor

import app.umaia.android.domain.repository.StepTracker
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class CompositeStepTracker @Inject constructor(
    val healthConnect: HealthConnectStepTracker,
    val googleFit: GoogleFitStepTracker,
    val sensor: SensorStepTracker
) : StepTracker {

    private fun active(): StepTracker = when {
        healthConnect.isAvailable && healthConnect.isAuthorized -> healthConnect
        googleFit.isAuthorized -> googleFit
        else -> sensor
    }

    override val isAvailable = true

    override val isAuthorized: Boolean
        get() = active() !== sensor || sensor.isAuthorized

    override suspend fun requestAuthorization() = active().requestAuthorization()

    override fun observeDailySteps(): Flow<Int> = active().observeDailySteps()

    override suspend fun currentDailySteps(): Int = active().currentDailySteps()

    override suspend fun querySteps(from: Date, to: Date): Int = active().querySteps(from, to)
}
