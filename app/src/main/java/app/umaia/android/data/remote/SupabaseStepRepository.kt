package app.umaia.android.data.remote

import app.umaia.android.domain.repository.LeaderboardData
import app.umaia.android.domain.repository.LeaderboardEntry
import app.umaia.android.domain.repository.LeaderboardPeriod
import app.umaia.android.domain.repository.StepRepository
import app.umaia.android.domain.repository.StepSubmitResult
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
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
        val function = when (period) {
            LeaderboardPeriod.DAILY   -> "get_leaderboard_daily"
            LeaderboardPeriod.WEEKLY  -> "get_leaderboard_weekly"
            LeaderboardPeriod.MONTHLY -> "get_leaderboard_monthly"
            LeaderboardPeriod.ALLTIME -> "get_leaderboard_alltime"
        }

        // Server stamps `step_date := CURRENT_DATE` in UTC, but the app
        // reports an Asia/Almaty calendar day. We query the RPC with
        // `p_date = almatyTodayString()` and accept that **peer** rows are
        // empty during the ~5h Almaty-morning boundary window (when no
        // server rows yet have step_date = Almaty-today). The user's own row
        // is overlaid precisely below via `overrideMyRow` + a `submitted_at`
        // range query, so the user always sees their own correct totals
        // even when the peer list is sparse. The proper fix is server-side
        // (alter the trigger to use `(now() AT TIME ZONE 'Asia/Almaty')`).
        val rows: List<LeaderboardRow> = run {
            val params = buildMap<String, String> {
                if (period == LeaderboardPeriod.DAILY) put("p_date", almatyTodayString())
                if (!companyCode.isNullOrBlank()) put("p_company", companyCode)
            }
            runCatching<List<LeaderboardRow>> {
                db.rpc<List<LeaderboardRow>>(function, params)
            }.getOrElse {
                android.util.Log.e("UmaiaSteps", "getLeaderboard($period, $companyCode) FAILED: ${it.message}", it)
                emptyList()
            }
        }

        android.util.Log.d("UmaiaSteps", "getLeaderboard($period, $companyCode): ${rows.size} rows")

        // Server RPCs anchor `step_date` filters to UTC `CURRENT_DATE`, but the
        // app reports an Asia/Almaty calendar window. For DAILY and MONTHLY,
        // overwrite the user's own row with a precise Almaty-window query
        // against `user_steps` (filtered by `submitted_at`). Peers' rows can't
        // be corrected client-side (RLS), so they remain slightly imprecise
        // around the TZ boundary (~5h on the 1st of each month for monthly,
        // ~5h every Almaty morning for daily) — pending a server `p_date` /
        // `SET TIMEZONE 'Asia/Almaty'` fix.
        val mergedRows: List<LeaderboardRow> = when (period) {
            LeaderboardPeriod.DAILY -> {
                val (start, end) = almatyDayUtcRange()
                overrideMyRow(rows, uid, fetchMyStatsInRange(start, end))
            }
            LeaderboardPeriod.MONTHLY -> {
                val (start, end) = almatyMonthUtcRange()
                overrideMyRow(rows, uid, fetchMyStatsInRange(start, end))
            }
            else -> rows
        }

        val sorted = mergedRows.sortedByDescending { it.total_steps }
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
            mySteps = myRow?.total_steps,
            myNur = myRow?.total_nur,
        )
    }

    @Serializable
    private data class ProfileNameRow(val full_name: String? = null)

    /** Best-effort fetch of the calling user's `profiles.full_name`. Used
     *  when synthesizing a leaderboard row during the Almaty/UTC boundary
     *  window so the row matches what other users would see. Falls back to
     *  the server's `Nomad-<6 chars>` default. */
    private suspend fun fetchMyDisplayName(uid: String): String {
        val name: String? = runCatching {
            val row: ProfileNameRow? = db.selectOptional(
                table = "profiles",
                columns = "full_name",
                filters = mapOf("user_id" to uid),
                single = true,
            )
            row?.full_name?.trim()?.takeUnless { it.isEmpty() }
        }.getOrNull()
        return name ?: "Nomad-${uid.take(6)}"
    }

    /** Replace (or insert) the calling user's row in [rows] with precise
     *  `(steps, nur)` from a `user_steps` query. Resets rank to 0 so the
     *  caller re-ranks by sort order. When synthesizing a fresh row (no
     *  existing entry — e.g. the RPC returned 0 rows for this user during
     *  the Almaty/UTC boundary window), fills in the user's profile name so
     *  the row doesn't render as "Anonymous". */
    private suspend fun overrideMyRow(
        rows: List<LeaderboardRow>,
        uid: String,
        precise: Pair<Int, Int>,
    ): List<LeaderboardRow> {
        val (steps, nur) = precise
        val existing = rows.firstOrNull { it.user_id == uid }
        val displayName = existing?.display_name ?: fetchMyDisplayName(uid)
        val replacement = (existing ?: LeaderboardRow(user_id = uid, total_steps = 0))
            .copy(display_name = displayName, total_steps = steps, total_nur = nur, rank = 0)
        return if (existing != null) rows.map { if (it.user_id == uid) replacement else it }
        else rows + replacement
    }

    /** Current date string (yyyy-MM-dd) in Asia/Almaty (UTC+5). Used as the
     *  `p_date` parameter to `get_leaderboard_daily`. */
    private fun almatyTodayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Almaty")
        }.format(Date())

    /** ISO-8601 UTC instants for the Asia/Almaty current calendar day bounds. */
    private fun almatyDayUtcRange(): Pair<String, String> {
        val isoUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Almaty")).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = isoUtc.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = isoUtc.format(cal.time)
        return start to end
    }

    /** Sum (step_count, total_nur) for the calling user across an arbitrary
     *  UTC range. Only reads own rows (RLS-safe).
     *
     *  `total_nur` is the *full* period earnings — not just step Nur. The
     *  server's leaderboard RPC only aggregates `user_steps.nur_awarded`, so
     *  the user's bonus grants (daily share +10, daily login +20, Oracle
     *  completion, wisdom tests) never appear there. We fix that client-side
     *  by also summing `user_coin_transactions.amount` for the same range,
     *  excluding `steps_validated` rows (which would double-count the trigger's
     *  mirror of `user_steps.nur_awarded`). */
    private suspend fun fetchMyStatsInRange(startUtc: String, endUtc: String): Pair<Int, Int> {
        val uid = db.userId
        @Serializable data class StepRow(val step_count: Int, val nur_awarded: Int)
        @Serializable data class TxRow(val amount: Int, val transaction_type: String)

        val (steps, stepNur) = runCatching {
            val rows: List<StepRow> = db.select(
                table = "user_steps",
                columns = "step_count,nur_awarded",
                filters = mapOf("user_id" to uid, "rejected" to "false"),
                rawFilters = listOf(
                    "submitted_at" to "gte.$startUtc",
                    "submitted_at" to "lt.$endUtc",
                ),
            )
            rows.fold(0 to 0) { (s, n), r -> (s + r.step_count) to (n + r.nur_awarded) }
        }.getOrElse {
            android.util.Log.e("UmaiaSteps", "fetchMyStatsInRange steps($startUtc..$endUtc) FAILED: ${it.message}", it)
            0 to 0
        }

        val bonusNur = runCatching {
            val txs: List<TxRow> = db.select(
                table = "user_coin_transactions",
                columns = "amount,transaction_type",
                filters = mapOf("user_id" to uid),
                rawFilters = listOf(
                    "created_at" to "gte.$startUtc",
                    "created_at" to "lt.$endUtc",
                ),
            )
            // Exclude steps_validated — already counted in stepNur above.
            // Sum positive grants (daily_share, daily_login, oracle_*, etc.)
            // and any negatives (refunds / claims) so the period total
            // matches user_coins.balance deltas.
            txs.asSequence().filter { it.transaction_type != "steps_validated" }.sumOf { it.amount }
        }.getOrElse {
            android.util.Log.e("UmaiaSteps", "fetchMyStatsInRange bonus($startUtc..$endUtc) FAILED: ${it.message}", it)
            0
        }

        android.util.Log.d("UmaiaSteps", "fetchMyStatsInRange($startUtc..$endUtc): steps=$steps stepNur=$stepNur bonusNur=$bonusNur")
        return steps to (stepNur + bonusNur)
    }

    /** ISO-8601 UTC instants for the Asia/Almaty current month bounds —
     *  `[start, end)` with end-exclusive. Used to query the user's own
     *  `user_steps` rows precisely by `submitted_at`, so the monthly hero
     *  on Walk shows the right Almaty-month totals even when the server's
     *  `get_leaderboard_monthly` RPC is anchored to the UTC month (which
     *  diverges in the first 5h of an Almaty month). */
    private fun almatyMonthUtcRange(): Pair<String, String> {
        val isoUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Almaty")).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = isoUtc.format(cal.time)
        cal.add(Calendar.MONTH, 1)
        val end = isoUtc.format(cal.time)
        return start to end
    }

    override suspend fun getTodayServerSteps(): Int {
        val (start, end) = almatyDayUtcRange()
        return fetchMyStatsInRange(start, end).first
    }

    override suspend fun getTodayServerNur(): Int {
        val (start, end) = almatyDayUtcRange()
        return fetchMyStatsInRange(start, end).second
    }
}
