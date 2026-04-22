package app.umaia.android.data.sensor

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import app.umaia.android.domain.repository.StepTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking as runBlockingCoroutines

@Singleton
class HealthConnectStepTracker @Inject constructor(
    @ApplicationContext private val context: Context
) : StepTracker {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    override val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE

    override val isAuthorized: Boolean
        get() {
            if (!isAvailable) return false
            return try {
                runBlockingCoroutines {
                    val grantedPermissions = client.permissionController.getGrantedPermissions()
                    permissions.all { grantedPermissions.contains(it) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check HC authorization", e)
                false
            }
        }

    override suspend fun requestAuthorization() = isAuthorized

    override fun observeDailySteps(): Flow<Int> = flow {
        while (true) {
            emit(currentDailySteps())
            delay(5_000L)
        }
    }

    override suspend fun currentDailySteps(): Int {
        if (!isAuthorized) return 0
        val now = Instant.now()
        val startOfDay = LocalDateTime.now()
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant(ZoneOffset.of(ZoneOffset.systemDefault().rules.getOffset(Instant.now()).toString()))
        return readSteps(startOfDay, now)
    }

    override suspend fun querySteps(from: Date, to: Date): Int {
        if (!isAuthorized) return 0
        return readSteps(from.toInstant(), to.toInstant())
    }

    private suspend fun readSteps(start: Instant, end: Instant): Int = try {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        response.records.sumOf { (it as StepsRecord).count.toInt() }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read steps from Health Connect", e)
        0
    }

    companion object {
        private const val TAG = "HealthConnectStepTracker"
    }
}
