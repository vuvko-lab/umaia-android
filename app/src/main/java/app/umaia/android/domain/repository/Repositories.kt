package app.umaia.android.domain.repository

import app.umaia.android.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface LoginRepository {
    /** Records today's login (idempotent). Returns Nur earned this call (0 if already logged in). */
    suspend fun recordDailyLogin(): Int
    suspend fun getCurrentStreak(): Int
}

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile
    suspend fun updateProfile(fullName: String?, city: String?, gender: String?, age: Int?)
    suspend fun completeOnboarding(tribalRole: String)
    suspend fun incrementAppOpen(): Int
    suspend fun saveOracleResult(tribalRole: String)
}

interface NurRepository {
    suspend fun getBalance(): Int
    suspend fun addNur(amount: Int, reason: String): Int
}

interface StepTracker {
    val isAuthorized: Boolean
    suspend fun requestAuthorization(): Boolean
    fun observeDailySteps(): Flow<Int>
    suspend fun currentDailySteps(): Int
    suspend fun querySteps(from: java.util.Date, to: java.util.Date): Int
}

// ── Leaderboard / Step submission ─────────────────────────────────────────────

enum class LeaderboardPeriod { DAILY, WEEKLY, ALLTIME }

data class StepSubmitResult(
    val success: Boolean,
    val nurAwarded: Int,
    val rejected: Boolean,
    val rejectReason: String?
)

data class LeaderboardEntry(
    val userId: String,
    val fullName: String?,
    val totalSteps: Int,
    val totalNur: Int,
    val rank: Int,
    val isMe: Boolean
)

data class LeaderboardData(
    val entries: List<LeaderboardEntry>,
    val myRank: Int?,
    val mySteps: Int?
)

interface StepRepository {
    suspend fun submitSteps(
        count: Int,
        source: String,
        suspectedCheating: Boolean = false
    ): StepSubmitResult

    suspend fun getLeaderboard(period: LeaderboardPeriod): LeaderboardData
}
