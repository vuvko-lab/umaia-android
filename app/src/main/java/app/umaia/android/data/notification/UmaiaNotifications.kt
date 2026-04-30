package app.umaia.android.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.umaia.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local notifications. Mirrors iOS `NotificationService` —
 * `scheduleDailySummary` (delivered by [DailySummaryWorker]),
 * `notifyRewardUnlocked`, and `notifyTopRankDrop`.
 *
 * Channels are created lazily per-call (idempotent on Android 26+) so the
 * helper has no init cost on devices below O.
 */
@Singleton
class UmaiaNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val manager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    /** Posts the localized "check today's Nur" reminder. Called from
     *  [DailySummaryWorker] at 20:00 Asia/Almaty. */
    fun notifyDailySummary(title: String, body: String) {
        ensureChannel(CHANNEL_DAILY_SUMMARY, "Daily summary", "Reminds you to check today's Nur and your rank.")
        if (!hasPostPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_SUMMARY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIF_ID_DAILY_SUMMARY, notification) }
    }

    /** Posts the reward-unlocked notification. iOS dedupes per
     *  (rewardId, periodId, userId) via UserDefaults; on Android the dedupe
     *  lives in [GamePreferences.markUnlockNotified] — caller checks it. */
    fun notifyRewardUnlocked(partner: String, item: String) {
        ensureChannel(CHANNEL_REWARD_UNLOCK, "Rewards", "Notifies when a monthly reward unlocks.")
        if (!hasPostPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_REWARD_UNLOCK)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Umaia 🎁")
            .setContentText("You've unlocked $item. Tap Claim to redeem from $partner.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIF_ID_REWARD_UNLOCK, notification) }
    }

    /** Posts the top-rank-drop notification. Caller (LeaderboardViewModel)
     *  rate-limits per period so we don't spam the same drop twice. */
    fun notifyTopRankDrop(newRank: Int) {
        ensureChannel(CHANNEL_RANK_DROP, "Rank changes", "Notifies when you fall out of the monthly top 3.")
        if (!hasPostPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_RANK_DROP)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Umaia")
            .setContentText("You're no longer in the top 3 (#$newRank). Keep moving!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIF_ID_RANK_DROP, notification) }
    }

    private fun ensureChannel(id: String, name: String, description: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
            this.description = description
        }
        val sysManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sysManager.createNotificationChannel(channel)
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return manager.areNotificationsEnabled()
    }

    companion object {
        const val CHANNEL_REWARD_UNLOCK = "umaia_reward_unlock"
        const val CHANNEL_RANK_DROP = "umaia_rank_drop"
        const val CHANNEL_DAILY_SUMMARY = "umaia_daily_summary"
        private const val NOTIF_ID_REWARD_UNLOCK = 1001
        private const val NOTIF_ID_RANK_DROP = 1002
        private const val NOTIF_ID_DAILY_SUMMARY = 1003
    }
}
