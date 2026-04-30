package app.umaia.android.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.umaia.android.data.local.AppPreferences
import app.umaia.android.data.notification.UmaiaNotifications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Posts the daily 20:00 reminder. Body strings mirror iOS
 *  `NotificationService.scheduleDailySummary` verbatim. */
@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notifications: UmaiaNotifications,
    private val appPreferences: AppPreferences,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val (title, body) = when (appPreferences.language.value) {
            "kk" -> "Umaia" to "Бүгінгі Нұрыңды және рейтингтегі орныңды тексер."
            "ru" -> "Umaia" to "Проверь свой Нур за сегодня и место в рейтинге."
            else -> "Umaia" to "Check today's Nur and your rank on the leaderboard."
        }
        notifications.notifyDailySummary(title, body)
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "umaia_daily_summary"
    }
}
