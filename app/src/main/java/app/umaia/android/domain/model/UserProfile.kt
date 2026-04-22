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
    val questionnaireDone: Boolean
)
