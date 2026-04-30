package app.umaia.android.data.remote

import app.umaia.android.domain.model.UserProfile
import app.umaia.android.domain.repository.InvalidCompanyCodeException
import app.umaia.android.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseProfileRepository @Inject constructor(private val db: PostgrestClient) : ProfileRepository {

    private val _profileFlow = MutableStateFlow<UserProfile?>(null)

    override fun observeProfile(): Flow<UserProfile?> = _profileFlow.asStateFlow()

    override suspend fun getProfile(): UserProfile {
        val uid = db.userId

        @Serializable data class ProfileDto(
            val id: String? = null, val user_id: String,
            val full_name: String? = null, val gender: String? = null,
            val age: Int? = null, val city: String? = null,
            val onboarding_complete: Boolean? = null, val app_open_count: Int? = null,
            val company_code: String? = null
        )
        @Serializable data class CompanyDto(val code: String, val name: String)
        @Serializable data class CoinsDto(val balance: Int? = null)
        @Serializable data class StreakDto(val current_streak: Int? = null, val longest_streak: Int? = null)
        @Serializable data class AssessmentDto(
            val tribal_role: String? = null,
            val oracle_session: Int? = null,
            val questionnaire_completed: Boolean? = null
        )

        val profile: ProfileDto = db.select(
            table = "profiles", filters = mapOf("user_id" to uid), single = true
        )
        val coins: CoinsDto?   = db.selectOptional(table = "user_coins",  filters = mapOf("user_id" to uid), single = true)
        val streaks: StreakDto? = db.selectOptional(table = "user_streaks", filters = mapOf("user_id" to uid), single = true)
        val assess: AssessmentDto? = db.selectOptional(
            table = "health_assessments",
            columns = "tribal_role,oracle_session,questionnaire_completed",
            filters = mapOf("user_id" to uid), single = true
        )
        // Resolve company display name when a code is set. Done as a separate
        // SELECT (not a JOIN) — the companies table is small and select-by-PK is cheap.
        val companyName: String? = profile.company_code
            ?.takeIf { it.isNotBlank() }
            ?.let {
                db.selectOptional<CompanyDto>(
                    table = "companies",
                    columns = "code,name",
                    filters = mapOf("code" to it),
                    single = true
                )?.name
            }

        val result = UserProfile(
            userId            = profile.user_id,
            fullName          = profile.full_name,
            gender            = profile.gender,
            age               = profile.age,
            city              = profile.city,
            onboardingComplete= profile.onboarding_complete ?: false,
            appOpenCount      = profile.app_open_count ?: 0,
            nurBalance        = coins?.balance ?: 0,
            currentStreak     = streaks?.current_streak ?: 0,
            longestStreak     = streaks?.longest_streak ?: 0,
            tribalRole        = assess?.tribal_role,
            oracleSession     = assess?.oracle_session ?: 0,
            questionnaireDone = assess?.questionnaire_completed ?: false,
            companyCode       = profile.company_code,
            companyName       = companyName
        )
        _profileFlow.value = result
        return result
    }

    override suspend fun updateProfile(fullName: String?, city: String?, gender: String?, age: Int?) {
        @Serializable data class ProfileUpdate(
            val full_name: String? = null, val city: String? = null,
            val gender: String? = null, val age: Int? = null
        )
        db.update(ProfileUpdate(fullName, city, gender, age), "profiles", mapOf("user_id" to db.userId))
    }

    override suspend fun completeOnboarding(tribalRole: String) {
        @Serializable data class OB(val onboarding_complete: Boolean = true)
        db.update(OB(), "profiles", mapOf("user_id" to db.userId))
        @Serializable data class AssessUpsert(val user_id: String, val tribal_role: String)
        db.upsert(AssessUpsert(db.userId, tribalRole), "health_assessments", onConflict = "user_id")
    }

    override suspend fun incrementAppOpen(): Int {
        val profile = getProfile()
        val next = profile.appOpenCount + 1
        @Serializable data class AOUpdate(val app_open_count: Int)
        db.update(AOUpdate(next), "profiles", mapOf("user_id" to db.userId))
        return next
    }

    override suspend fun saveOracleResult(tribalRole: String) {
        @Serializable data class OracleUpsert(
            val user_id: String, val tribal_role: String, val questionnaire_completed: Boolean = true
        )
        db.upsert(OracleUpsert(db.userId, tribalRole), "health_assessments", onConflict = "user_id")
    }

    override suspend fun setCompanyCode(code: String): Pair<String, String> {
        @Serializable data class CompanyDto(val code: String, val name: String)
        val trimmed = code.trim().uppercase()
        val company = try {
            db.rpc<CompanyDto>("set_company_code", mapOf("p_code" to trimmed))
        } catch (t: Throwable) {
            // Server raises P0001 "Invalid company code" with a 400 — surface as typed error.
            if (t.message?.contains("Invalid company code", ignoreCase = true) == true) {
                throw InvalidCompanyCodeException()
            }
            throw t
        }
        // Refresh the cached profile so observers see the new company code/name.
        runCatching { getProfile() }
        return company.code to company.name
    }
}
