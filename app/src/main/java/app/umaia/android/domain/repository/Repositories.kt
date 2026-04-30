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

    /**
     * Validates a company invite code via the `set_company_code` RPC and binds
     * it to the current user's profile. Returns the matched (code, displayName).
     * Throws [InvalidCompanyCodeException] when the code doesn't exist.
     */
    suspend fun setCompanyCode(code: String): Pair<String, String>
}

class InvalidCompanyCodeException(message: String = "Invalid company code") : RuntimeException(message)

interface NurRepository {
    suspend fun getBalance(): Int
    suspend fun addNur(amount: Int, reason: String): Int
}

interface StepTracker {
    val isAvailable: Boolean get() = true
    val isAuthorized: Boolean
    suspend fun requestAuthorization(): Boolean
    fun observeDailySteps(): Flow<Int>
    suspend fun currentDailySteps(): Int
    suspend fun querySteps(from: java.util.Date, to: java.util.Date): Int
}

// ── Leaderboard / Step submission ─────────────────────────────────────────────

enum class LeaderboardPeriod { DAILY, WEEKLY, MONTHLY, ALLTIME }

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

    /**
     * Fetch leaderboard for [period], scoped to [companyCode]. Pass `null` to
     * fetch the public-pool leaderboard (everyone with no company assigned).
     */
    suspend fun getLeaderboard(period: LeaderboardPeriod, companyCode: String?): LeaderboardData
}

// ── Reward Claims ────────────────────────────────────────────────────────────

enum class RewardClaimStatus { PENDING, APPROVED, SHIPPED, DELIVERED, REJECTED }

enum class RewardDeliveryMethod(val wire: String) {
    PICKUP("pickup"),
    SHIP("ship");

    companion object {
        fun fromWire(s: String?): RewardDeliveryMethod = when (s) {
            "ship" -> SHIP
            else -> PICKUP
        }
    }
}

data class RewardClaimSubmission(
    val rewardId: String,           // e.g. "tshirt_monthly"
    val periodId: String,           // start of the reward period, "yyyy-MM-dd"
    val fullName: String,
    val phone: String,
    val size: String,               // "XS"…"XXL"
    val deliveryMethod: RewardDeliveryMethod,
    val city: String,
    val address: String?,           // required iff deliveryMethod == SHIP
    val notes: String?,
)

data class RewardClaim(
    val id: Long,
    val rewardId: String,
    val periodId: String,
    val status: RewardClaimStatus,
    val size: String,
    val deliveryMethod: RewardDeliveryMethod,
    val city: String,
    val address: String?,
    val trackingUrl: String?,
    val claimedAt: String,          // ISO-8601
    val fulfilledAt: String?,
)

/**
 * User's standing on the current month's podium for their cohort. Wire shape
 * matches the `monthly_winner_status_for_me()` RPC return columns.
 */
data class MonthlyWinnerStatus(
    val amWinner: Boolean,
    val myRank: Int?,
    val podiumSize: Int,            // 3 by default
    val spotsTaken: Int,
    val spotsRemaining: Int,
    val targetNur: Int,             // typically 2000
    val myMonthlyNur: Int,
    val periodId: String,           // "2026-04-01"
)

interface RewardRepository {
    /** Submit a new claim (or update a still-pending one for the same period). */
    suspend fun submitClaim(submission: RewardClaimSubmission): RewardClaim

    /** Returns the current claim for (rewardId, periodId), or null if none yet. */
    suspend fun getClaim(rewardId: String, periodId: String): RewardClaim?

    /** Pulls the current month's podium status for the calling user. */
    suspend fun getMonthlyWinnerStatus(): MonthlyWinnerStatus
}
