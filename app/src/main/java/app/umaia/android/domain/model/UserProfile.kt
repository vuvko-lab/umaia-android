package app.umaia.android.domain.model

data class UserProfile(
    val userId: String,
    val fullName: String?,
    val gender: String?,
    val age: Int?,
    val city: String?,
    val onboardingComplete: Boolean,
    val appOpenCount: Int,
    val nurBalance: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val tribalRole: String?,
    val oracleSession: Int,
    val questionnaireDone: Boolean,
    /** Code of the company the user is enrolled in (e.g. "ALMAU2026"). Null = public pool. */
    val companyCode: String? = null,
    /** Joined from `companies.name` — e.g. "AlmaU". Null for public-pool users. */
    val companyName: String? = null,
) {
    val isCompanyMember: Boolean get() = companyCode != null
}
