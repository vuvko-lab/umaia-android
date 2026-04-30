package app.umaia.android.data.sensor

import android.util.Log
import app.umaia.android.data.local.StepPreferences
import app.umaia.android.domain.repository.StepRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializes step-delta submissions across multiple submitters (foreground
 * tick loop, HealthConnect background sync, offline catch-up). Kotlin port
 * of `umaia-ios/Umaia/Data/Sensor/StepSubmissionCoordinator.swift`.
 *
 * Without this, two submitters could race on
 * [StepPreferences.lastSubmittedDailyTotal]:
 *
 *   t0  fg reads baseline=1000, current=1100, delta=100
 *   t0  bg reads baseline=1000, current=1100, delta=100
 *   t1  fg POSTs 100 with source="live"
 *   t1  bg POSTs 100 with source="hc_background"
 *   t2  fg writes baseline=1100
 *   t2  bg writes baseline=1100
 *
 * The server's idempotency is keyed on (user_id, session_id, source,
 * client_ts, step_count); different `source` values bypass dedup, so both
 * rows land and the leaderboard sees 200 steps where the user walked 100.
 *
 * The [Mutex] pins the read-baseline → claim-baseline portion so later
 * concurrent callers see the already-claimed baseline and short-circuit on
 * delta == 0. Network failures roll the claim back via CAS so a transient
 * outage doesn't permanently lose a delta — but the rollback only fires
 * when no later call has advanced the baseline past our claim.
 *
 * Also enforces the same 32s gate as
 * [app.umaia.android.ui.screens.steps.StepsViewModel.MIN_SUBMIT_GAP_MS]
 * (mirrors the server's 30s `too_frequent` rule) so off-cadence callers
 * — sheet open/close, stop/start cycles, the HC background worker — never
 * trip the server's anti-spam.
 */
@Singleton
class StepSubmissionCoordinator @Inject constructor(
    private val stepRepository: StepRepository,
    private val stepPreferences: StepPreferences,
) {

    private val mutex = Mutex()

    /** Submits `currentDaily − lastSubmittedDailyTotal` to the server with
     *  the given `source` / `suspectedCheating` annotations. Returns the
     *  delta actually claimed (0 when nothing to send or gated by the
     *  32s rule). */
    suspend fun submitDelta(
        currentDaily: Int,
        source: String,
        suspectedCheating: Boolean,
    ): Int = mutex.withLock {
        submitDeltaLocked(
            currentDaily = currentDaily,
            startingBaseline = stepPreferences.lastSubmittedDailyTotal(),
            source = source,
            suspectedCheating = suspectedCheating,
        )
    }

    /** Reconciles the local baseline with what the server already has for
     *  today, then submits whatever gap remains as `source`. Used at app
     *  boot when local DataStore may have been wiped (reinstall) but the
     *  server still remembers earlier deltas — without this fast-forward,
     *  the boot path would resubmit the entire daily total and the HC
     *  observer would also resubmit it, doubling everything for fresh
     *  installs. */
    suspend fun reconcileAndSubmit(
        currentDaily: Int,
        knownServerTotal: Int,
        source: String,
        suspectedCheating: Boolean,
    ): Int = mutex.withLock {
        val local = stepPreferences.lastSubmittedDailyTotal()
        val baseline = maxOf(local, knownServerTotal)
        if (baseline > local) {
            stepPreferences.setLastSubmittedDailyTotal(baseline)
            Log.d(TAG, "reconcile($source) — fast-forwarded baseline $local→$baseline to match server")
        }
        submitDeltaLocked(
            currentDaily = currentDaily,
            startingBaseline = baseline,
            source = source,
            suspectedCheating = suspectedCheating,
        )
    }

    private suspend fun submitDeltaLocked(
        currentDaily: Int,
        startingBaseline: Int,
        source: String,
        suspectedCheating: Boolean,
    ): Int {
        val delta = (currentDaily - startingBaseline).coerceAtLeast(0)
        if (delta == 0) {
            Log.d(TAG, "submit($source) — no delta (current=$currentDaily, baseline=$startingBaseline)")
            return 0
        }

        val now = System.currentTimeMillis()
        if (now - stepPreferences.lastSubmitAttemptAt < MIN_SUBMIT_GAP_MS) {
            Log.d(TAG, "submit($source) — gated by 32s rule, deferring delta=$delta")
            return 0
        }

        // Claim the baseline before the network await. We're inside `mutex`,
        // so this read-then-write is atomic against other coordinator entries.
        stepPreferences.setLastSubmittedDailyTotal(currentDaily)
        Log.d(TAG, "submit($source) — claimed delta=$delta baseline=$startingBaseline→$currentDaily")

        val result = runCatching {
            stepRepository.submitSteps(delta, source, suspectedCheating)
        }.getOrElse {
            // CAS rollback: only revert if no later call has moved the
            // baseline past our claim. Keeps later, larger claims intact
            // while letting transient failures be retried by the next tick.
            if (stepPreferences.lastSubmittedDailyTotal() == currentDaily) {
                stepPreferences.setLastSubmittedDailyTotal(startingBaseline)
                Log.e(TAG, "submit($source) FAILED, rolled baseline back to $startingBaseline: ${it.message}")
            } else {
                Log.e(TAG, "submit($source) FAILED but baseline already advanced; not rolling back: ${it.message}")
            }
            return 0
        }

        // Whether server `rejected` the row or not, advance the gate timer
        // so back-to-back retries don't get rejected again immediately.
        stepPreferences.lastSubmitAttemptAt = now

        if (!result.success || result.rejected) {
            // Server stored the row but didn't credit it. The baseline we
            // already advanced is wrong — roll back so the next tick retries
            // the same delta (just outside the 32s gate now).
            if (stepPreferences.lastSubmittedDailyTotal() == currentDaily) {
                stepPreferences.setLastSubmittedDailyTotal(startingBaseline)
            }
            Log.w(TAG, "submit($source) rejected by server: ${result.rejectReason}")
            return 0
        }

        Log.i(TAG, "submit($source) OK — $delta steps, +${result.nurAwarded} Nur")
        return delta
    }

    companion object {
        private const val TAG = "UmaiaStepCoord"
        /** Mirrors `StepsViewModel.MIN_SUBMIT_GAP_MS` (32s ≈ server's 30s + skew). */
        private const val MIN_SUBMIT_GAP_MS = 32_000L
    }
}
