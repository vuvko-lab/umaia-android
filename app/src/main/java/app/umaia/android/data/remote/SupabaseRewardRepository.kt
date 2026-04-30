package app.umaia.android.data.remote

import app.umaia.android.domain.repository.MonthlyWinnerStatus
import app.umaia.android.domain.repository.RewardClaim
import app.umaia.android.domain.repository.RewardClaimStatus
import app.umaia.android.domain.repository.RewardClaimSubmission
import app.umaia.android.domain.repository.RewardDeliveryMethod
import app.umaia.android.domain.repository.RewardRepository
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseRewardRepository @Inject constructor(private val db: PostgrestClient) : RewardRepository {

    // ── Wire DTOs ────────────────────────────────────────────────────────────

    @Serializable
    private data class RewardClaimInsert(
        val user_id: String,
        val reward_id: String,
        val period_id: String,
        val full_name: String,
        val phone: String,
        val size: String,
        val delivery_method: String,
        val city: String,
        val address: String? = null,
        val notes: String? = null,
    )

    @Serializable
    private data class RewardClaimRow(
        val id: Long,
        val reward_id: String,
        val period_id: String,
        val status: String,
        val size: String,
        val delivery_method: String,
        val city: String,
        val address: String? = null,
        val tracking_url: String? = null,
        val claimed_at: String,
        val fulfilled_at: String? = null,
    )

    @Serializable
    private data class WinnerStatusRow(
        val am_winner: Boolean,
        val my_rank: Int? = null,
        val podium_size: Int,
        val spots_taken: Int,
        val spots_remaining: Int,
        val target_nur: Int,
        val my_monthly_nur: Int,
        val period_id: String,
    )

    private val claimColumns =
        "id,reward_id,period_id,status,size,delivery_method,city,address,tracking_url,claimed_at,fulfilled_at"

    // ── RewardRepository ─────────────────────────────────────────────────────

    override suspend fun submitClaim(submission: RewardClaimSubmission): RewardClaim {
        val payload = RewardClaimInsert(
            user_id = db.userId,
            reward_id = submission.rewardId,
            period_id = submission.periodId,
            full_name = submission.fullName,
            phone = submission.phone,
            size = submission.size,
            delivery_method = submission.deliveryMethod.wire,
            city = submission.city,
            // Send null (not "") so the SQL CHECK doesn't reject pickup with a blank address.
            address = submission.address?.takeIf { it.isNotBlank() },
            notes = submission.notes?.takeIf { it.isNotBlank() },
        )
        // Upsert to avoid 409 on retap; (user_id, reward_id, period_id) is UNIQUE.
        // The RLS policy blocks updates once status leaves 'pending', so this
        // only works while ops hasn't started fulfilment yet.
        val row: RewardClaimRow = db.upsertReturning(
            value = payload,
            table = "reward_claims",
            onConflict = "user_id,reward_id,period_id",
            columns = claimColumns
        )
        return row.toDomain()
    }

    override suspend fun getClaim(rewardId: String, periodId: String): RewardClaim? {
        val row: RewardClaimRow? = db.selectOptional(
            table = "reward_claims",
            columns = claimColumns,
            filters = mapOf(
                "user_id" to db.userId,
                "reward_id" to rewardId,
                "period_id" to periodId
            ),
            single = true
        )
        return row?.toDomain()
    }

    override suspend fun getMonthlyWinnerStatus(): MonthlyWinnerStatus {
        // RPC returns SETOF (one row); PostgREST encodes that as a JSON array.
        val rows: List<WinnerStatusRow> = runCatching {
            db.rpc<List<WinnerStatusRow>>("monthly_winner_status_for_me")
        }.getOrElse {
            android.util.Log.e("UmaiaRewards", "monthly_winner_status_for_me FAILED: ${it.message}", it)
            emptyList()
        }
        val row = rows.firstOrNull() ?: return MonthlyWinnerStatus(
            amWinner = false, myRank = null,
            podiumSize = 3, spotsTaken = 0, spotsRemaining = 3,
            targetNur = 2000, myMonthlyNur = 0,
            periodId = ""
        )
        return MonthlyWinnerStatus(
            amWinner = row.am_winner,
            myRank = row.my_rank,
            podiumSize = row.podium_size,
            spotsTaken = row.spots_taken,
            spotsRemaining = row.spots_remaining,
            targetNur = row.target_nur,
            myMonthlyNur = row.my_monthly_nur,
            periodId = row.period_id
        )
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private fun RewardClaimRow.toDomain(): RewardClaim = RewardClaim(
        id = id,
        rewardId = reward_id,
        periodId = period_id,
        status = parseStatus(status),
        size = size,
        deliveryMethod = RewardDeliveryMethod.fromWire(delivery_method),
        city = city,
        address = address,
        trackingUrl = tracking_url,
        claimedAt = claimed_at,
        fulfilledAt = fulfilled_at,
    )

    private fun parseStatus(s: String): RewardClaimStatus = when (s.lowercase()) {
        "approved" -> RewardClaimStatus.APPROVED
        "shipped" -> RewardClaimStatus.SHIPPED
        "delivered" -> RewardClaimStatus.DELIVERED
        "rejected" -> RewardClaimStatus.REJECTED
        else -> RewardClaimStatus.PENDING
    }
}
