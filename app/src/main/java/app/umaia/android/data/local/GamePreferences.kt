package app.umaia.android.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // ── Wisdom-test (Nutrition) anti-farm (v1.3.3) ───────────────────────────

    /** Set of quizIds the user has already been awarded Nur for. Re-attempting
     *  the same quiz returns 0 grant — server-side `nurRepository.addNur` is
     *  also gated on the local return so we don't pump duplicate transactions.
     *  Mirrors iOS `awardedQuizIds`. */
    private val awardedQuizIdsKey = "umaia_awarded_quiz_ids"

    /** Returns the amount actually granted (0 if the quiz has been claimed before).
     *  Emits [bonusNurGranted] on grant so observers (StepsViewModel, ProfileViewModel)
     *  refresh their displayed totals immediately. */
    fun addQuizNurOnce(quizId: String, amount: Int): Int {
        if (amount <= 0) return 0
        val ids = prefs.getStringSet(awardedQuizIdsKey, emptySet()).orEmpty().toMutableSet()
        if (quizId in ids) return 0
        ids.add(quizId)
        prefs.edit().putStringSet(awardedQuizIdsKey, ids).apply()
        addBonusNur(amount)
        _bonusNurGranted.tryEmit(Unit)
        return amount
    }

    /** Lets callers outside this class (Oracle completion) trigger the same
     *  cross-tab refresh as a daily-share / quiz grant — used after the
     *  server-side `nurRepository.addNur` for Oracle, which writes directly
     *  to `user_coins.balance` without going through the local quiz path. */
    fun notifyBonusGranted() {
        _bonusNurGranted.tryEmit(Unit)
    }

    fun hasAwardedQuizNur(quizId: String): Boolean =
        quizId in prefs.getStringSet(awardedQuizIdsKey, emptySet()).orEmpty()

    // ── Nutrition category progression (v1.3.3) ──────────────────────────────

    private val nutritionCompletedKey = "umaia_nutrition_completed_categories"

    fun completedNutritionCategories(): List<String> =
        prefs.getStringSet(nutritionCompletedKey, emptySet()).orEmpty().toList()

    fun markNutritionCategoryCompleted(category: String) {
        val current = prefs.getStringSet(nutritionCompletedKey, emptySet()).orEmpty().toMutableSet()
        if (current.add(category)) {
            prefs.edit().putStringSet(nutritionCompletedKey, current).apply()
        }
    }

    /** Accumulated non-step bonus Nur (daily-share, future bonuses). Bumped
     *  by [claimDailyShareNur]; included in [totalNur] so it propagates into
     *  the UMAIA-tab Total Nur stat without affecting server-truth monthly
     *  Nur (which is step-only). */
    private val bonusNurKey = "umaia_bonus_nur_accumulated"
    private fun accumulatedBonusNur(): Int = prefs.getInt(bonusNurKey, 0)
    private fun addBonusNur(amount: Int) {
        prefs.edit().putInt(bonusNurKey, accumulatedBonusNur() + amount).apply()
    }

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
        val nur = sumSteps / 100 + totalQuizNur + accumulatedBonusNur()
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

    // ── Top-3 rank tracking (v1.4.0) ─────────────────────────────────────────
    //
    // iOS persists `lastTop3Status` (Bool) + `lastTop3CheckPeriod` to detect
    // the moment a user falls out of the monthly top 3. Mirrors keys here so
    // a rank-drop notification only fires once per (period, transition).

    private val lastTop3PeriodKey = "umaia_last_top3_period"
    private val lastTop3StatusKey = "umaia_last_top3_status"  // 1 = was top-3, 0 = was outside

    /** True if our last sample said "in top 3" for the given periodId. */
    fun lastWasTopThree(periodId: String): Boolean {
        if (prefs.getString(lastTop3PeriodKey, null) != periodId) return false
        return prefs.getBoolean(lastTop3StatusKey, false)
    }

    fun setLastTopThreeStatus(periodId: String, isTopThree: Boolean) {
        prefs.edit()
            .putString(lastTop3PeriodKey, periodId)
            .putBoolean(lastTop3StatusKey, isTopThree)
            .apply()
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

    // ── Daily share-Nur bonus (v1.3.1) ───────────────────────────────────────

    /** Emits after a non-step Nur grant lands (daily-share, future bonuses).
     *  Lets the Steps screen recompute aggregates without waiting for the
     *  next pedometer tick. Replaces iOS NotificationCenter `umaiaBonusNurGranted`. */
    private val _bonusNurGranted = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val bonusNurGranted: SharedFlow<Unit> = _bonusNurGranted.asSharedFlow()

    private fun lastShareNurDateKey() = "umaia_last_share_nur_date"

    /** True iff the user has already claimed today's share-Nur bonus. */
    fun hasClaimedShareNurToday(): Boolean =
        prefs.getString(lastShareNurDateKey(), null) == today().toString()

    /**
     * Grant the daily share-Nur bonus if not already claimed today. Returns the
     * amount granted (0 if already claimed). The caller is responsible for the
     * server-side mirror via `NurRepository.addNur(amount, "daily_share")`.
     * Emits [bonusNurGranted] so the Walk-tab aggregates refresh immediately.
     */
    fun claimDailyShareNur(): Int {
        if (hasClaimedShareNurToday()) return 0
        prefs.edit().putString(lastShareNurDateKey(), today().toString()).apply()
        addBonusNur(SHARE_DAILY_NUR)
        _bonusNurGranted.tryEmit(Unit)
        return SHARE_DAILY_NUR
    }

    fun daysSinceLastVisit(): Int {
        val lastVisit = lastVisitDate ?: return 0
        return runCatching {
            val last = LocalDate.parse(lastVisit)
            java.time.temporal.ChronoUnit.DAYS.between(last, today()).toInt()
        }.getOrDefault(0)
    }

    companion object {
        /** Daily share-Nur grant. Same value on iOS (`GamePreferences.shareDailyNur`). */
        const val SHARE_DAILY_NUR: Int = 10
    }
}
