package app.umaia.android.data.sensor

import app.umaia.android.domain.repository.StepTracker
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart

/**
 * Reads steps from every authorized source in parallel and returns the maximum.
 * Rationale: a fitness band synced via Health Connect and the on-device step counter
 * each have blind spots — phone left at home, band not worn, sync lag. Max-of-sources
 * is conservatively right: whichever source actually saw the steps wins, no double-count
 * because we never sum them.
 */
@Singleton
class CompositeStepTracker @Inject constructor(
    val healthConnect: HealthConnectStepTracker,
    val sensor: SensorStepTracker
) : StepTracker {

    /** All authorized sources. */
    private fun authorizedSources(): List<StepTracker> = buildList {
        if (healthConnect.isAvailable && healthConnect.isAuthorized) add(healthConnect)
        if (sensor.isAuthorized) add(sensor)
    }

    override val isAvailable = true

    override val isAuthorized: Boolean
        get() = authorizedSources().isNotEmpty()

    override suspend fun requestAuthorization(): Boolean = isAuthorized

    override fun observeDailySteps(): Flow<Int> {
        val sources = authorizedSources()
        return when (sources.size) {
            0 -> flowOf(0)
            1 -> sources[0].observeDailySteps()
            // combine() only emits after every source has emitted at least once. The on-device
            // sensor flow emits only when a step is taken, so without a seed value the merged
            // flow could deadlock for a stationary user. Prepend 0 to each branch to unblock.
            else -> combine(sources.map { it.observeDailySteps().onStart { emit(0) } }) { values -> values.max() }
        }
    }

    override suspend fun currentDailySteps(): Int =
        authorizedSources().maxOfOrNull { it.currentDailySteps() } ?: 0

    override suspend fun querySteps(from: Date, to: Date): Int =
        authorizedSources().maxOfOrNull { it.querySteps(from, to) } ?: 0
}
