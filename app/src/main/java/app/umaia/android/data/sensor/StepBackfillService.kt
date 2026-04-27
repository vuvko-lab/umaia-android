package app.umaia.android.data.sensor

import app.umaia.android.data.local.StepPreferences
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lazy-backfill service. On Steps screen entry / app foreground, queries every authorized
 * step source for the gap between [StepPreferences.lastActiveTimestamp] and now and
 * returns the max. No bucket merging — the composite does max-of-sources for us.
 */
@Singleton
class StepBackfillService @Inject constructor(
    private val composite: CompositeStepTracker,
    private val stepPrefs: StepPreferences
) {

    /** Maximum gap to backfill — clamps unusually long absences. */
    private val maxGapMs = 7L * 24 * 60 * 60 * 1000  // 7 days

    /** Returns total steps recovered for the gap. */
    suspend fun backfill(now: Long = System.currentTimeMillis()): Int {
        val lastTs = stepPrefs.lastActiveTimestamp
        val from = if (lastTs == 0L) now - 60 * 60 * 1000 else maxOf(lastTs, now - maxGapMs)
        if (now - from < 60_000) return 0
        return composite.querySteps(Date(from), Date(now))
    }
}
