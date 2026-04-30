package app.umaia.android.ui.screens.steps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
 *
 * Uses `goAsync()` to keep the receiver — and therefore the process —
 * alive until the network call to `nurRepository.addNur` actually completes.
 * Without it, `onReceive` returns immediately, the OS may kill the process,
 * and the +10 server-side mirror never lands (observed in v1.5.0 — local
 * `umaia_last_share_nur_date` got set but `user_coin_transactions` had no
 * `daily_share` rows for the user, ever).
 */
@AndroidEntryPoint
class ShareCompletionReceiver : BroadcastReceiver() {

    @Inject lateinit var gamePreferences: GamePreferences
    @Inject lateinit var nurRepository: NurRepository
    @Inject lateinit var analytics: AnalyticsService

    override fun onReceive(context: Context, intent: Intent) {
        val granted = gamePreferences.claimDailyShareNur()
        if (granted <= 0) {
            Log.d(TAG, "share completion fired but already claimed today — no-op")
            return
        }
        analytics.dailyShareClaimed()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                runCatching { nurRepository.addNur(granted, "daily_share") }
                    .onSuccess { Log.i(TAG, "+$granted Nur daily_share — server balance now $it") }
                    .onFailure { Log.e(TAG, "+$granted Nur daily_share FAILED server-side: ${it.message}", it) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "UmaiaShareReceiver"
    }
}
