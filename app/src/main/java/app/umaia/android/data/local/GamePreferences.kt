package app.umaia.android.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamePreferences @Inject constructor(@ApplicationContext ctx: Context) {

    private val prefs: SharedPreferences = ctx.getSharedPreferences("umaia_game_prefs", Context.MODE_PRIVATE)
    private val json  = Json { ignoreUnknownKeys = true }

    var dailySteps: Int
        get() = prefs.getInt("umaia_daily_steps", 0)
        set(v) { prefs.edit().putInt("umaia_daily_steps", v).apply() }

    var dailyStepsDate: String?
        get() = prefs.getString("umaia_daily_steps_date", null)
        set(v) { prefs.edit().putString("umaia_daily_steps_date", v).apply() }

    var totalSteps: Int
        get() = prefs.getInt("umaia_total_steps", 0)
        set(v) { prefs.edit().putInt("umaia_total_steps", v).apply() }

    var welcomeShownUid: String?
        get() = prefs.getString("umaia_welcome_shown_uid", null)
        set(v) { prefs.edit().putString("umaia_welcome_shown_uid", v).apply() }

    var lastVisitDate: String?
        get() = prefs.getString("umaia_last_visit_date", null)
        set(v) { prefs.edit().putString("umaia_last_visit_date", v).apply() }

    var registrationDate: String?
        get() = prefs.getString("umaia_registration_date", null)
        set(v) { prefs.edit().putString("umaia_registration_date", v).apply() }

    var onboardingDone: Boolean
        get() = prefs.getBoolean("umaia_onboarding_done", false)
        set(v) { prefs.edit().putBoolean("umaia_onboarding_done", v).apply() }

    var oracleNurAwarded: Boolean
        get() = prefs.getBoolean("umaia_oracle_nur_awarded", false)
        set(v) { prefs.edit().putBoolean("umaia_oracle_nur_awarded", v).apply() }

    fun addTotalSteps(delta: Int) { totalSteps += delta }

    fun updateStepHistory(todaySteps: Int) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val map = getStepHistory().toMutableMap()
        map[today] = todaySteps
        putStepHistory(map)
    }

    fun setStepHistory(map: Map<String, Int>) { putStepHistory(map) }

    fun getStepHistory(): Map<String, Int> {
        val raw = prefs.getString("umaia_step_history", null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Int>>(raw) }.getOrDefault(emptyMap())
    }

    fun putStepHistory(map: Map<String, Int>) {
        prefs.edit()
            .putString("umaia_step_history", json.encodeToString(kotlinx.serialization.serializer(), map))
            .apply()
    }

    // ── Weekly / Monthly step aggregates (registration-anchored) ─────────────

    private fun getOrCreateRegistrationDate(): java.time.LocalDate {
        if (registrationDate != null) {
            return java.time.LocalDate.parse(registrationDate!!)
        }
        val today = java.time.LocalDate.now()
        registrationDate = today.toString()
        return today
    }

    val currentWeekStart: java.time.LocalDate get() {
        val regDate = getOrCreateRegistrationDate()
        val today = java.time.LocalDate.now()
        val daysSinceReg = java.time.temporal.ChronoUnit.DAYS.between(regDate, today).toInt()
        val weekIndex = daysSinceReg / 7
        return regDate.plusDays((weekIndex * 7).toLong())
    }

    val currentMonthStart: java.time.LocalDate get() {
        val regDate = getOrCreateRegistrationDate()
        val today = java.time.LocalDate.now()
        val daysSinceReg = java.time.temporal.ChronoUnit.DAYS.between(regDate, today).toInt()
        val monthIndex = daysSinceReg / 30
        return regDate.plusDays((monthIndex * 30).toLong())
    }

    fun weeklySteps(includingDaily: Int): Int {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val today = fmt.format(java.util.Date())
        val history = getStepHistory().toMutableMap()
        history[today] = includingDaily
        val weekStart = currentWeekStart
        return history.entries.sumOf { (dateStr, steps) ->
            val date = runCatching { java.time.LocalDate.parse(dateStr) }.getOrNull() ?: return@sumOf 0
            if (!date.isBefore(weekStart)) steps else 0
        }
    }

    fun effectiveMonthlySteps(includingDaily: Int): Int {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val today = fmt.format(java.util.Date())
        val history = getStepHistory().toMutableMap()
        history[today] = includingDaily
        val monthStart = currentMonthStart
        return history.entries.sumOf { (dateStr, steps) ->
            val date = runCatching { java.time.LocalDate.parse(dateStr) }.getOrNull() ?: return@sumOf 0
            if (!date.isBefore(monthStart)) steps else 0
        }
    }

    // ── Reward claiming ───────────────────────────────────────────────────────

    fun isRewardClaimed(id: String): Boolean {
        val period = prefs.getString("reward_period_$id", null) ?: return false
        val claimedAt = prefs.getLong("reward_claimed_at_$id", 0L)
        val now = java.util.Calendar.getInstance()
        val claimedCal = java.util.Calendar.getInstance().also { it.timeInMillis = claimedAt }
        return when (period) {
            "weekly" -> claimedCal.get(java.util.Calendar.WEEK_OF_YEAR) == now.get(java.util.Calendar.WEEK_OF_YEAR) &&
                        claimedCal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)
            "monthly" -> claimedCal.get(java.util.Calendar.MONTH) == now.get(java.util.Calendar.MONTH) &&
                         claimedCal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)
            else -> false
        }
    }

    fun claimReward(id: String, period: String) {
        prefs.edit()
            .putString("reward_period_$id", period)
            .putLong("reward_claimed_at_$id", System.currentTimeMillis())
            .apply()
    }

    // ── Welcome dialogs ──────────────────────────────────────────────────────

    fun isWelcomeShown(uid: String): Boolean {
        return welcomeShownUid == uid
    }

    fun markWelcomeShown(uid: String) {
        welcomeShownUid = uid
    }

    fun recordVisit() {
        lastVisitDate = java.time.LocalDate.now().toString()
        if (registrationDate == null) {
            registrationDate = java.time.LocalDate.now().toString()
        }
    }

    fun daysSinceLastVisit(): Int {
        val lastVisit = lastVisitDate ?: return 0
        return try {
            val last = java.time.LocalDate.parse(lastVisit)
            val today = java.time.LocalDate.now()
            java.time.temporal.ChronoUnit.DAYS.between(last, today).toInt()
        } catch (e: Exception) {
            0
        }
    }
}
