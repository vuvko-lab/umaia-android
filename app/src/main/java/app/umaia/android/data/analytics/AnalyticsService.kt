package app.umaia.android.data.analytics

import android.app.Application
import android.content.Context
import app.umaia.android.BuildConfig
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsService @Inject constructor(@ApplicationContext ctx: Context) {

    init {
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = "https://eu.i.posthog.com"
        )
        PostHogAndroid.setup(ctx as Application, config)
        // Super properties on all events
        PostHog.register("platform", "android")
        PostHog.register("app_version", BuildConfig.VERSION_NAME)
        PostHog.register("app_version_code", BuildConfig.VERSION_CODE)
    }

    fun identify(email: String) {
        PostHog.identify(
            distinctId = email,
            userProperties = mapOf(
                "email" to email,
                "platform" to "android",
                "app_version" to BuildConfig.VERSION_NAME
            )
        )
    }

    fun signedIn(method: String)       = PostHog.capture("signed_in",      properties = mapOf("method" to method))
    fun signedOut()                    = PostHog.capture("signed_out")
    fun onboardingCompleted()          = PostHog.capture("onboarding_completed")
    fun tabSwitched(to: String)        = PostHog.capture("tab_switched",    properties = mapOf("tab" to to))
    fun buildingBuilt(type: String)    = PostHog.capture("building_built",  properties = mapOf("type" to type))
    fun questCompleted(id: Int)        = PostHog.capture("quest_completed", properties = mapOf("quest_id" to id))
    fun stepsSynced(steps: Int)        = PostHog.capture("steps_synced",    properties = mapOf("steps" to steps))
    fun nurEarned(amount: Int, reason: String) =
        PostHog.capture("nur_earned", properties = mapOf("amount" to amount, "reason" to reason))
    fun oracleCompleted(role: String, nurEarned: Int) =
        PostHog.capture("oracle_completed", properties = mapOf("tribal_role" to role, "nur_earned" to nurEarned))
    fun oracleStarted()                = PostHog.capture("oracle_started")
    fun oracleRetaken()                = PostHog.capture("oracle_retaken")
    fun oracleQuestionAnswered(idx: Int, dataKey: String) =
        PostHog.capture("oracle_question_answered", properties = mapOf("index" to idx, "key" to dataKey))
    fun nutritionCategoryCompleted(cat: String) =
        PostHog.capture("nutrition_category_completed", properties = mapOf("category" to cat))
    fun nutritionLessonStarted(cat: String) =
        PostHog.capture("nutrition_lesson_started", properties = mapOf("category" to cat))
    fun nutritionQuizAnswered(quizId: String, correct: Boolean) =
        PostHog.capture("nutrition_quiz_answered", properties = mapOf("quiz_id" to quizId, "correct" to correct))
    fun stepPermissionRequested(granted: Boolean) =
        PostHog.capture("step_permission_requested", properties = mapOf("granted" to granted))
    fun stepMilestoneReached(steps: Int) =
        PostHog.capture("step_milestone_reached", properties = mapOf("steps" to steps))
    fun offlineStepsCaught(steps: Int) =
        PostHog.capture("offline_steps_caught", properties = mapOf("steps" to steps))
    fun sessionDecayStarted(minutes: Int) =
        PostHog.capture("session_decay_started", properties = mapOf("minutes" to minutes))
    fun panelOpened(panelId: String) =
        PostHog.capture("panel_opened", properties = mapOf("panel_id" to panelId))
    fun suspectedStepCheating(steps: Int, distanceMeters: Double) =
        PostHog.capture("suspected_step_cheating", properties = mapOf("steps" to steps, "distance_meters" to distanceMeters.toInt()))

    fun accountDeleted() = PostHog.capture("account_deleted")
    fun languageChanged(lang: String) =
        PostHog.capture("language_changed", properties = mapOf("language" to lang))
    fun themeChanged(theme: String) =
        PostHog.capture("theme_changed", properties = mapOf("theme" to theme))
    fun passwordChanged() = PostHog.capture("password_changed")
    fun leaderboardViewed(period: String) =
        PostHog.capture("leaderboard_viewed", properties = mapOf("period" to period))
    fun leaderboardPeriodChanged(period: String) =
        PostHog.capture("leaderboard_period_changed", properties = mapOf("period" to period))
    fun stepTrackerConnected(tracker: String) =
        PostHog.capture("step_tracker_connected", properties = mapOf("tracker" to tracker))
}
