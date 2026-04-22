package app.umaia.android.data.remote

import app.umaia.android.domain.repository.LoginRepository
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val DAILY_LOGIN_COINS = 20

class SupabaseLoginRepository @Inject constructor(private val db: PostgrestClient) : LoginRepository {

    override suspend fun recordDailyLogin(): Int {
        val uid = db.userId
        val today = todayString()

        @Serializable data class LoginRow(val login_date: String)
        val existing: List<LoginRow> = db.selectOptional(
            table = "user_daily_logins",
            filters = mapOf("user_id" to uid, "login_date" to today)
        ) ?: emptyList()

        if (existing.isNotEmpty()) return 0

        @Serializable data class LoginInsert(val user_id: String, val coins_earned: Int)
        db.insert(LoginInsert(uid, DAILY_LOGIN_COINS), "user_daily_logins")

        upsertCoins(uid, DAILY_LOGIN_COINS)
        updateStreak(uid)

        return DAILY_LOGIN_COINS
    }

    override suspend fun getCurrentStreak(): Int {
        val uid = db.userId
        @Serializable data class StreakRow(val current_streak: Int)
        val row: StreakRow? = db.selectOptional(
            table = "user_streaks",
            filters = mapOf("user_id" to uid),
            single = true
        )
        return row?.current_streak ?: 0
    }

    private suspend fun upsertCoins(uid: String, delta: Int) {
        @Serializable data class CoinsRow(val balance: Int)
        val current: CoinsRow? = db.selectOptional(
            table = "user_coins",
            filters = mapOf("user_id" to uid),
            single = true
        )
        @Serializable data class CoinsUpsert(val user_id: String, val balance: Int, val updated_at: String)
        db.upsert(CoinsUpsert(uid, (current?.balance ?: 0) + delta, "now()"), "user_coins")

        @Serializable data class TxInsert(
            val user_id: String, val amount: Int,
            val transaction_type: String,
            val description_en: String, val description_ru: String
        )
        db.insert(TxInsert(uid, delta, "daily_login", "Daily login reward", "Ежедневный вход"), "user_coin_transactions")
    }

    private suspend fun updateStreak(uid: String) {
        @Serializable data class StreakRow(
            val current_streak: Int,
            val longest_streak: Int,
            val last_check_in_date: String?
        )
        val existing: StreakRow? = db.selectOptional(
            table = "user_streaks", filters = mapOf("user_id" to uid), single = true
        )
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val today = fmt.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = fmt.format(cal.time)

        val newStreak = if (existing?.last_check_in_date == yesterday) (existing.current_streak) + 1 else 1
        val longest   = maxOf(existing?.longest_streak ?: 0, newStreak)

        @Serializable data class StreakUpsert(
            val user_id: String, val current_streak: Int, val longest_streak: Int,
            val last_check_in_date: String, val updated_at: String
        )
        db.upsert(StreakUpsert(uid, newStreak, longest, today, "now()"), "user_streaks")
    }

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
