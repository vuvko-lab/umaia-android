package app.umaia.android.ui.screens.steps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.domain.repository.NurRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires when the user actually picks an app from the system share chooser
 * (`Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER` payload). Faithful Android
 * equivalent of iOS `completionWithItemsHandler { _, completed, _, _ in }` —
 * grant +10 daily-share Nur only when a real share was initiated, not when
 * the user dismissed the chooser.
 *
 * Local dedupe via [GamePreferences.claimDailyShareNur] (returns 0 if
 * already claimed today, Asia/Almaty); server-side mirror via
 * `nurRepository.addNur(10, "daily_share")`.
 */
@AndroidEntryPoint
class ShareCompletionReceiver : BroadcastReceiver() {

    @Inject lateinit var gamePreferences: GamePreferences
    @Inject lateinit var nurRepository: NurRepository
    @Inject lateinit var analytics: AnalyticsService

    override fun onReceive(context: Context, intent: Intent) {
        val granted = gamePreferences.claimDailyShareNur()
        if (granted <= 0) return  // already claimed today
        analytics.dailyShareClaimed()
        // Fire-and-forget on the application scope (Hilt-injected receiver
        // lifetime ends with onReceive return, so we use a supervisor scope
        // tied to the IO dispatcher).
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            runCatching { nurRepository.addNur(granted, "daily_share") }
        }
    }
}
