package app.umaia.android.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GpsSnapshot(
    val latitude: Double,
    val longitude: Double,
    val stepCheckpoint: Int
)

@Singleton
class StepPreferences @Inject constructor(@ApplicationContext ctx: Context) {

    private val prefs = ctx.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    // ── Baseline ──────────────────────────────────────────────────────────────

    /** Running sensor total recorded at start-of-day. -1 = not set. */
    var baselineCount: Int
        get() = prefs.getInt("step_baseline_count", -1)
        set(v) { prefs.edit().putInt("step_baseline_count", v).apply() }

    /** Date string (yyyy-MM-dd) for the baseline snapshot. */
    var baselineDate: String?
        get() = prefs.getString("step_baseline_date", null)
        set(v) { prefs.edit().putString("step_baseline_date", v).apply() }

    // ── Activity tracking ─────────────────────────────────────────────────────

    /** Last date the app was active — kept for backward compat with baseline logic. */
    var lastActiveDate: String?
        get() = prefs.getString("step_last_active_date", null)
        set(v) { prefs.edit().putString("step_last_active_date", v).apply() }

    /** Epoch millis of last activity — used for the 60-second catch-up threshold. */
    var lastActiveTimestamp: Long
        get() = prefs.getLong("step_last_active_ts", 0L)
        private set(v) { prefs.edit().putLong("step_last_active_ts", v).apply() }

    /** Last observed cumulative TYPE_STEP_COUNTER value, captured while sensor listener was active.
     *  Used to compute deltas across app sessions when HC has no data for the gap. */
    var lastSensorCumulative: Int
        get() = prefs.getInt("step_last_sensor_cumulative", -1)
        set(v) { prefs.edit().putInt("step_last_sensor_cumulative", v).apply() }

    /** Epoch millis when [lastSensorCumulative] was captured. */
    var lastSensorCumulativeTs: Long
        get() = prefs.getLong("step_last_sensor_cumulative_ts", 0L)
        set(v) { prefs.edit().putLong("step_last_sensor_cumulative_ts", v).apply() }

    // ── Submission idempotency (Asia/Almaty per-day) ──────────────────────────

    /**
     * Cumulative steps already submitted to the server for today (Asia/Almaty).
     * Used as the anchor for delta submissions: at each flush, send
     * `currentSteps − lastSubmittedDailyTotal` and on success advance.
     * Survives cold start, so re-launching the app no longer re-submits the
     * day's count and doubles the leaderboard.
     */
    private fun lastSubmittedKey(date: String) = "step_last_submitted_total_$date"

    /** Almaty `yyyy-MM-dd`. */
    private fun almatyToday(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Almaty")
        }.format(Date())

    fun lastSubmittedDailyTotal(date: String = almatyToday()): Int =
        prefs.getInt(lastSubmittedKey(date), 0)

    fun setLastSubmittedDailyTotal(total: Int, date: String = almatyToday()) {
        prefs.edit().putInt(lastSubmittedKey(date), total).apply()
    }

    /** Call whenever the user is actively using the app / steps are flowing. */
    fun recordActive() {
        val now = System.currentTimeMillis()
        lastActiveTimestamp = now
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        lastActiveDate = dateStr
    }

    // ── GPS anti-cheat ────────────────────────────────────────────────────────

    private fun todayKey() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** True if the cheating flag has been set for today. */
    val isSuspectedCheatingToday: Boolean
        get() = prefs.getBoolean("step_cheat_flag_${todayKey()}", false)

    /** Mark today's session as suspected cheating. */
    fun flagSuspectedCheating() {
        prefs.edit().putBoolean("step_cheat_flag_${todayKey()}", true).apply()
    }

    /** Step count at the last GPS checkpoint (rounded to nearest 1000). */
    var lastGpsCheckpoint: Int
        get() = prefs.getInt("step_gps_checkpoint", 0)
        set(v) { prefs.edit().putInt("step_gps_checkpoint", v).apply() }

    /** Up to last 10 GPS snapshots, stored as JSON. */
    val gpsSnapshots: List<GpsSnapshot>
        get() {
            val raw = prefs.getString("step_gps_snapshots", null) ?: return emptyList()
            return runCatching { json.decodeFromString<List<GpsSnapshot>>(raw) }.getOrElse { emptyList() }
        }

    fun addGpsSnapshot(snap: GpsSnapshot) {
        val updated = (gpsSnapshots + snap).takeLast(10)
        prefs.edit().putString("step_gps_snapshots", json.encodeToString(updated)).apply()
    }

    // ── Account deletion ──────────────────────────────────────────────────────

    /** Clears all step preferences — call before deleting the account. */
    fun reset() {
        prefs.edit().clear().apply()
    }
}
