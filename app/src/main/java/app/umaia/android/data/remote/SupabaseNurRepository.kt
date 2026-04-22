package app.umaia.android.data.remote

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

    override suspend fun addNur(amount: Int, reason: String): Int {
        val uid = db.userId
        val current = getBalance()
        val newBalance = maxOf(0, current + amount)

        @Serializable data class CoinsUpsert(val user_id: String, val balance: Int, val updated_at: String)
        db.upsert(CoinsUpsert(uid, newBalance, "now()"), "user_coins")

        @Serializable data class TxInsert(
            val user_id: String, val amount: Int,
            val transaction_type: String,
            val description_en: String, val description_ru: String
        )
        db.insert(TxInsert(uid, amount, reason, reason, reason), "user_coin_transactions")

        return newBalance
    }
}
