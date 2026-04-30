package app.umaia.android.data.remote

import android.util.Log
import app.umaia.android.domain.repository.NurRepository
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseNurRepository @Inject constructor(private val db: PostgrestClient) : NurRepository {

    override suspend fun getBalance(): Int {
        @Serializable data class CoinsRow(val balance: Int)
        val row: CoinsRow? = db.selectOptional(
            table = "user_coins", filters = mapOf("user_id" to db.userId), single = true
        )
        return row?.balance ?: 0
    }

    /**
     * Awards Nur via the server-side `award_nur` RPC. Mirrors iOS
     * `SupabaseNurRepository.addNur` — the RPC is `SECURITY DEFINER` and
     * atomically (a) inserts a `user_coin_transactions` row and
     * (b) upserts `user_coins.balance` with the right `ON CONFLICT` clause.
     * This is the only correct path; the prior client-side
     * `db.upsert(user_coins)` failed every call with `23505 unique
     * constraint user_coins_user_id_key` because PostgREST upserts without
     * an `on_conflict` parameter degrade to a plain INSERT.
     *
     * The RPC has a server-side whitelist + per-reason cap; unknown reasons
     * return `success=false, error=unknown_reason`. Currently allowed:
     * `quest_*`, `nutrition_quiz_*`, `oracle_health_assessment`, `game_sync`.
     * **`daily_share` is NOT yet whitelisted server-side** — needs a SQL
     * patch to `award_nur` (add `WHEN p_reason = 'daily_share' THEN 10`).
     * Until that ships, daily_share calls log a warning but the local
     * accumulator + leaderboard fallback keep the user-facing UX correct.
     */
    override suspend fun addNur(amount: Int, reason: String): Int {
        @Serializable data class AwardResult(
            val success: Boolean = false,
            val awarded: Int? = null,
            val new_balance: Int? = null,
            val error: String? = null,
        )
        val params = mapOf("p_amount" to amount.toString(), "p_reason" to reason)
        val result = runCatching {
            db.rpc<AwardResult>("award_nur", params)
        }.getOrElse {
            Log.e(TAG, "award_nur($reason, $amount) HTTP failure: ${it.message}", it)
            return 0
        }
        if (!result.success) {
            Log.w(TAG, "award_nur($reason, $amount) rejected: ${result.error}")
            return 0
        }
        Log.i(TAG, "award_nur($reason, $amount) OK — new_balance=${result.new_balance}")
        return result.new_balance ?: 0
    }

    companion object {
        private const val TAG = "UmaiaNur"
    }
}
