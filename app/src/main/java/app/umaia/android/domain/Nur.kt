package app.umaia.android.domain

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln

/**
 * Per-day step→Nur asymptote. Mirrors the server-side `validate_step_submission`
 * trigger constant (see `supabase_steps_nur_cap_80.sql`). Bumping this requires
 * a paired SQL migration — otherwise local previews drift from the server's
 * recorded `nur_awarded` and the Walk-tab hero diverges from the leaderboard.
 */
const val NUR_DAILY_CAP: Double = 80.0

/**
 * Nur earned from a single day's cumulative steps:
 * `cap · (1 − e^(−steps/6000))`.
 *
 * Asymptotic — more steps always award more, but with diminishing returns.
 * 5k steps ≈ 45 Nur, 10k ≈ 65, 15k ≈ 73, ∞ → 80.
 */
fun stepsToNur(steps: Int): Int =
    (NUR_DAILY_CAP * (1.0 - exp(-steps.toDouble() / 6000.0))).toInt()

/**
 * Inverse of [stepsToNur]: cumulative steps required for a day to award
 * `targetNur`. Returns null when target ≥ [NUR_DAILY_CAP] (unreachable from
 * walking alone — caller should suggest combining with daily bonuses).
 */
fun stepsForDailyNur(targetNur: Int): Int? {
    if (targetNur <= 0) return 0
    if (targetNur.toDouble() >= NUR_DAILY_CAP) return null
    val frac = targetNur / NUR_DAILY_CAP
    return ceil(-6000.0 * ln(1.0 - frac)).toInt()
}
