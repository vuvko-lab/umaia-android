package app.umaia.android.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed local cache for everything the StepsScreen and
 * Profile UI need that the server hasn't authoritatively replied with yet:
 * step history, registration date, welcome flags, and **per-user** reward
 * claim/spend bookkeeping.
 *
 * v1.3 changes:
 *  - `currentMonthStart` is the 1st of the calendar month (Asia/Almaty),
 *    NOT registration-anchored. This matches the server's
 *    `get_leaderboard_monthly` window (`DATE_TRUNC('month', now() AT TIME
 *    ZONE 'Asia/Almaty')`) so the local UI and the server stay aligned.
 *  - `currentWeekStart` is the ISO Monday (Asia/Almaty) so the weekly
 *    leaderboard "Resets every Monday" matches server-side reality.
 *  - All reward state (claimed flag, monthly Nur subtract, spent Nur,
 *    unlock-notification dedupe, company-choice flag) is suffixed with the
 *    Supabase user id so two accounts on one device don't share state.
 */
@Singleton
class GamePreferences @Inject constructor(@ApplicationContext ctx: Context) {

    private val prefs: SharedPreferences = ctx.getSharedPreferences("umaia_game_prefs", Context.MODE_PRIVATE)
    private val json  = Json { ignoreUnknownKeys = true }

    private val almatyZone: ZoneId = ZoneId.of("Asia/Almaty")
    private fun today(): LocalDate = LocalDate.now(almatyZone)

    // ── Daily steps ──────────────────────────────────────────────────────────

    var dailySteps: Int
        get() = prefs.getInt("umaia_daily_steps", 0)
        set(v) { prefs.edit().putInt("umaia_daily_steps", v).apply() }

    var dailyStepsDate: String?
        get() = prefs.getString("umaia_daily_steps_date", null)
        set(v) { prefs.edit().putString("umaia_daily_steps_date", v).apply() }

    var onboardingDone: Boolean
        get() = prefs.getBoolean("umaia_onboarding_done", false)
        set(v) { prefs.edit().putBoolean("umaia_onboarding_done", v).apply() }

    /** Quiz-completion flag — adds a flat +30 Nur into the lifetime total. */
    var oracleNurAwarded: Boolean
        get() = prefs.getBoolean("umaia_oracle_nur_awarded", false)
        set(v) { prefs.edit().putBoolean("umaia_oracle_nur_awarded", v).apply() }

    private val totalQuizNur: Int get() = if (oracleNurAwarded) 30 else 0

    // ── Step history (per-day map persisted as JSON) ─────────────────────────

    fun updateStepHistory(todaySteps: Int) {
        val todayStr = today().toString()
        val map = getStepHistory().toMutableMap()
        map[todayStr] = todaySteps
        putStepHistory(map)
    }

    fun setStepHistory(map: Map<String, Int>) { putStepHistory(map) }

    fun getStepHistory(): Map<String, Int> {
        val raw = prefs.getString("umaia_step_history", null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Int>>(raw) }.getOrDefault(emptyMap())
    }

    private fun putStepHistory(map: Map<String, Int>) {
        prefs.edit()
            .putString("umaia_step_history", json.encodeToString(kotlinx.serialization.serializer(), map))
            .apply()
    }

    // ── Calendar-aligned reward windows (Asia/Almaty) ────────────────────────

    /** 1st of the current calendar month, Asia/Almaty. */
    val currentMonthStart: LocalDate
        get() = today().withDayOfMonth(1)

    /** Period id used for the monthly reward window — "yyyy-MM-dd" of the 1st. */
    val currentMonthPeriodId: String get() = currentMonthStart.toString()

    /** ISO Monday of the current week, Asia/Almaty. */
    val currentWeekStart: LocalDate
        get() {
            val wf = WeekFields.ISO
            val today = today()
            return today.minusDays((today.get(wf.dayOfWeek()) - 1).toLong())
        }

    fun weeklySteps(includingDaily: Int): Int {
        val todayStr = today().toString()
        val history = getStepHistory().toMutableMap()
        history[todayStr] = includingDaily
        val weekStart = currentWeekStart
        return history.entries.sumOf { (dateStr, steps) ->
            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@sumOf 0
            if (!date.isBefore(weekStart)) steps else 0
        }
    }

    fun monthlySteps(includingDaily: Int): Int {
        val todayStr = today().toString()
        val history = getStepHistory().toMutableMap()
        history[todayStr] = includingDaily
        val monthStart = currentMonthStart
        return history.entries.sumOf { (dateStr, steps) ->
            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@sumOf 0
            if (!date.isBefore(monthStart)) steps else 0
        }
    }

    /**
     * Monthly Nur with claim deductions applied. Rewards Card and EarnMoreHints
     * use this so submitting a claim instantly drops the visible monthly bar
     * by the reward cost (matches iOS visual-purchase semantics).
     */
    fun effectiveMonthlyNur(includingDaily: Int, userId: String): Int {
        val rawSteps = monthlySteps(includingDaily)
        val nur = rawSteps / 100
        return (nur - monthlyNurSubtract(userId)).coerceAtLeast(0)
    }

    // ── Reward claim state (per-user) ────────────────────────────────────────
    //
    // Key formats per ANDROID_PORTING_GUIDE.md §7.1.

    private fun claimedKey(rewardId: String, userId: String) =
        "umaia_reward_${rewardId}_claimed_$userId"
    private fun claimedPeriodKey(rewardId: String, userId: String) =
        "umaia_reward_${rewardId}_period_$userId"

    /** True iff (rewardId, userId) has a claim recorded for the current period. */
    fun isRewardClaimed(rewardId: String, userId: String): Boolean {
        if (!prefs.getBoolean(claimedKey(rewardId, userId), false)) return false
        val period = prefs.getString(claimedPeriodKey(rewardId, userId), null)
        return period == currentMonthPeriodId
    }

    fun claimReward(rewardId: String, periodId: String, userId: String) {
        prefs.edit()
            .putBoolean(claimedKey(rewardId, userId), true)
            .putString(claimedPeriodKey(rewardId, userId), periodId)
            .apply()
    }

    // Per-user monthly Nur subtraction — applied right after a claim so the
    // displayed monthly Nur visibly drops by the reward cost. Cleared
    // automatically when the calendar month rolls (period_id mismatch).

    private fun monthlySubtractPeriodKey(userId: String) = "umaia_monthly_nur_subtract_period_$userId"
    private fun monthlySubtractValueKey(userId: String) = "umaia_monthly_nur_subtract_value_$userId"

    fun recordMonthlyNurSubtract(amount: Int, userId: String) {
        val current = monthlyNurSubtract(userId)
        prefs.edit()
            .putString(monthlySubtractPeriodKey(userId), currentMonthPeriodId)
            .putInt(monthlySubtractValueKey(userId), current + amount)
            .apply()
    }

    fun monthlyNurSubtract(userId: String): Int {
        val period = prefs.getString(monthlySubtractPeriodKey(userId), null)
        if (period != currentMonthPeriodId) return 0
        return prefs.getInt(monthlySubtractValueKey(userId), 0)
    }

    // Lifetime Nur "wallet" deductions — never reset. UMAIA tab Total Nur
    // subtracts this so claims look like a real purchase across all of time.

    private fun totalSpentKey(userId: String) = "umaia_total_spent_nur_$userId"

    fun recordRewardSpend(amount: Int, userId: String) {
        val current = totalSpentNur(userId)
        prefs.edit().putInt(totalSpentKey(userId), current + amount).apply()
    }

    fun totalSpentNur(userId: String): Int = prefs.getInt(totalSpentKey(userId), 0)

    /** Lifetime Nur for the UMAIA tab Total Nur stat. Derived from stepHistory
     *  + quiz nur − spent so all three Nur readings always agree. */
    fun totalNur(includingDaily: Int, userId: String): Int {
        val todayStr = today().toString()
        val history = getStepHistory().toMutableMap()
        history[todayStr] = includingDaily
        val sumSteps = history.values.sum()
        val nur = sumSteps / 100 + totalQuizNur
        return (nur - totalSpentNur(userId)).coerceAtLeast(0)
    }

    // ── Cross-tab unlock notification dedupe (per reward-period-user) ────────

    private fun unlockNotifiedKey(rewardId: String, periodId: String, userId: String) =
        "umaia_unlock_notified_${rewardId}_${periodId}_$userId"

    fun isUnlockNotified(rewardId: String, periodId: String, userId: String): Boolean =
        prefs.getBoolean(unlockNotifiedKey(rewardId, periodId, userId), false)

    fun markUnlockNotified(rewardId: String, periodId: String, userId: String) {
        prefs.edit().putBoolean(unlockNotifiedKey(rewardId, periodId, userId), true).apply()
    }

    // ── Company-code first-launch decision ───────────────────────────────────

    private fun companyChoiceMadeKey(userId: String) = "umaia_company_choice_made_$userId"

    fun isCompanyChoiceMade(userId: String): Boolean =
        prefs.getBoolean(companyChoiceMadeKey(userId), false)

    fun markCompanyChoiceMade(userId: String) {
        prefs.edit().putBoolean(companyChoiceMadeKey(userId), true).apply()
    }

    // ── Welcome / return dialogs ─────────────────────────────────────────────

    private var welcomeShownUid: String?
        get() = prefs.getString("umaia_welcome_shown_uid", null)
        set(v) { prefs.edit().putString("umaia_welcome_shown_uid", v).apply() }

    var lastVisitDate: String?
        get() = prefs.getString("umaia_last_visit_date", null)
        set(v) { prefs.edit().putString("umaia_last_visit_date", v).apply() }

    /** First-recorded launch date — kept for backward compat with v1.2 step
     *  history seeding. Not used for reward windows anymore. */
    var registrationDate: String?
        get() = prefs.getString("umaia_registration_date", null)
        set(v) { prefs.edit().putString("umaia_registration_date", v).apply() }

    fun isWelcomeShown(uid: String): Boolean = welcomeShownUid == uid
    fun markWelcomeShown(uid: String) { welcomeShownUid = uid }

    fun recordVisit() {
        lastVisitDate = today().toString()
        if (registrationDate == null) registrationDate = today().toString()
    }

    fun daysSinceLastVisit(): Int {
        val lastVisit = lastVisitDate ?: return 0
        return runCatching {
            val last = LocalDate.parse(lastVisit)
            java.time.temporal.ChronoUnit.DAYS.between(last, today()).toInt()
        }.getOrDefault(0)
    }
}
