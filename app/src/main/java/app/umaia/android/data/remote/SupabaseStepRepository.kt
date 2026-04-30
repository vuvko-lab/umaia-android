package app.umaia.android.data.remote

import app.umaia.android.domain.repository.LeaderboardData
import app.umaia.android.domain.repository.LeaderboardEntry
import app.umaia.android.domain.repository.LeaderboardPeriod
import app.umaia.android.domain.repository.StepRepository
import app.umaia.android.domain.repository.StepSubmitResult
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseStepRepository @Inject constructor(private val db: PostgrestClient) : StepRepository {

    private val sessionId = UUID.randomUUID().toString()

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Serializable
    private data class StepInsert(
        val user_id: String,
        val step_count: Int,
        val source: String,
        val client_ts: String? = null,
        val session_id: String? = null,
        val app_version: String? = null,
        val suspected_cheating: Boolean? = null
    )

    @Serializable
    private data class StepRow(
        val id: Long,
        val step_count: Int,
        val nur_awarded: Int = 0,
        val rejected: Boolean = false,
        val reject_reason: String? = null
    )

    @Serializable
    private data class LeaderboardRow(
        val user_id: String,
        val display_name: String? = null,
        val total_steps: Int,
        val total_nur: Int = 0,
        val rank: Int = 0
    )

    // ── StepRepository ───────────────────────────────────────────────────────

    override suspend fun submitSteps(count: Int, source: String, suspectedCheating: Boolean): StepSubmitResult {
        if (count <= 0) return StepSubmitResult(success = true, nurAwarded = 0, rejected = false, rejectReason = null)

        val uid = db.userId
        // ISO-8601 UTC timestamp; explicit timezone avoids JVM-default-zone bugs.
        val ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val insert = StepInsert(
            user_id = uid,
            step_count = count,
            source = source,
            client_ts = ts,
            session_id = sessionId,
            app_version = app.umaia.android.BuildConfig.VERSION_NAME,
            suspected_cheating = if (suspectedCheating) true else null
        )
        return runCatching {
            val row: StepRow = db.insertReturning(
                value = insert,
                table = "user_steps",
                columns = "id,step_count,nur_awarded,rejected,reject_reason"
            )
            android.util.Log.d("UmaiaSteps", "submitSteps OK: count=$count nur=${row.nur_awarded} rejected=${row.rejected}")
            StepSubmitResult(
                success = true,
                nurAwarded = row.nur_awarded,
                rejected = row.rejected,
                rejectReason = row.reject_reason
            )
        }.getOrElse {
            android.util.Log.e("UmaiaSteps", "submitSteps FAILED: ${it.message}", it)
            StepSubmitResult(success = false, nurAwarded = 0, rejected = false, rejectReason = it.message)
        }
    }

    override suspend fun getLeaderboard(period: LeaderboardPeriod, companyCode: String?): LeaderboardData {
        val uid = db.userId
        val params = buildMap<String, String> {
            // Daily leaderboard follows Almaty local calendar day (server attributes step_date in Asia/Almaty).
            if (period == LeaderboardPeriod.DAILY) put("p_date", almatyTodayString())
            // Only include p_company when set — absence equals NULL = public-pool.
            if (!companyCode.isNullOrBlank()) put("p_company", companyCode)
        }
        val function = when (period) {
            LeaderboardPeriod.DAILY   -> "get_leaderboard_daily"
            LeaderboardPeriod.WEEKLY  -> "get_leaderboard_weekly"
            LeaderboardPeriod.MONTHLY -> "get_leaderboard_monthly"
            LeaderboardPeriod.ALLTIME -> "get_leaderboard_alltime"
        }

        val rows: List<LeaderboardRow> = runCatching<List<LeaderboardRow>> {
            db.rpc<List<LeaderboardRow>>(function, params)
        }.getOrElse {
            android.util.Log.e("UmaiaSteps", "getLeaderboard($period, $companyCode) FAILED: ${it.message}", it)
            emptyList()
        }

        android.util.Log.d("UmaiaSteps", "getLeaderboard($period, $companyCode): ${rows.size} rows")
        val sorted = rows.sortedByDescending { it.total_steps }
        val myRow = sorted.firstOrNull { it.user_id == uid }
        val top50 = sorted.take(50)
        val entries = top50.mapIndexed { index, row ->
            LeaderboardEntry(
                userId = row.user_id,
                fullName = row.display_name,
                totalSteps = row.total_steps,
                totalNur = row.total_nur,
                rank = if (row.rank > 0) row.rank else index + 1,
                isMe = row.user_id == uid
            )
        }
        val myRank = myRow?.let { r ->
            if (r.rank > 0) r.rank else sorted.indexOfFirst { it.user_id == uid } + 1
        }
        return LeaderboardData(
            entries = entries,
            myRank = myRank,
            mySteps = myRow?.total_steps
        )
    }

    /** Current date string (yyyy-MM-dd) in Asia/Almaty (UTC+5). All daily
     *  step/leaderboard bookkeeping is anchored to Almaty local day so that
     *  multiple devices in different zones still aggregate to the same row. */
    private fun almatyTodayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Almaty")
        }.format(Date())
}
