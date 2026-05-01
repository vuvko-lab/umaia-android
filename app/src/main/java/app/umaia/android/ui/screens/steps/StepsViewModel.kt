package app.umaia.android.ui.screens.steps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.auth.AuthService
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.data.local.GpsSnapshot
import app.umaia.android.data.local.StepPreferences
import app.umaia.android.data.location.LocationTracker
import app.umaia.android.domain.model.StepMilestone
import app.umaia.android.domain.model.allMilestones
import app.umaia.android.domain.repository.StepRepository
import app.umaia.android.domain.repository.StepTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v1.3.4: which welcome dialog (if any) to render on first composition.
 * Mirrors iOS StepsScreen.maybeShowWelcome — first session shows the full
 * intro; later same-day sessions show nothing; sessions after ≥1 day away
 * show the return dialog with a Nur bonus.
 */
sealed class WelcomeOverlay {
    object None : WelcomeOverlay()
    object FirstVisit : WelcomeOverlay()
    data class Returning(val daysAway: Int, val nurBonus: Int) : WelcomeOverlay()
}

data class StepsUiState(
    val dailySteps: Int = 0,
    val totalSteps: Int = 0,
    val weeklySteps: Int = 0,
    val monthlySteps: Int = 0,
    val monthlyNur: Int = 0,
    /** Local "claim-subtract" counter for the current month — what the user
     *  has already redeemed from this month's Nur. Subtract from server-truth
     *  monthly Nur to render the displayed reward-tile progress. */
    val monthlyNurSubtract: Int = 0,
    val nurFromSteps: Int = 0,
    /** Server-truth Nur earned today (Asia/Almaty) across all sources —
     *  step Nur + daily share + daily login + Oracle + wisdom quizzes. Used
     *  by the share image so what the user posts reflects total-day Nur, not
     *  just the step formula preview. Falls back to [nurFromSteps] until the
     *  first server fetch completes. */
    val todayServerNur: Int = 0,
    val reachedMilestones: List<StepMilestone> = emptyList(),
    val nextMilestone: StepMilestone? = null,
    val isSensorAvailable: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val offlineStepsCaught: Int = 0,
    val stepHistory: Map<String, Int> = emptyMap(),
    val suspectedCheating: Boolean = false,
    val healthConnectActive: Boolean = false,
    /** True iff the daily-share +10 Nur bonus has already been claimed
     *  today (Asia/Almaty). Drives the badge swap from "+10 Nur" → ✓ on
     *  the Walk-tab Share buttons. Refreshed in [recomputeAggregates] so
     *  it picks up `bonusNurGranted` events without an explicit observer
     *  in the composable. */
    val shareClaimedToday: Boolean = false,
    // Debug — per-source step counts. -1 means "not authorized".
    val debugHcSteps: Int = -1,
    val debugSensorSteps: Int = -1
)

// v1.3.1: stepsToNur now lives in domain.Nur (asymptotic, matches server).
// Kept as a top-level alias so existing call-sites (StepsScreen UI) keep working.
internal fun stepsToNur(steps: Int): Int = app.umaia.android.domain.stepsToNur(steps)

@HiltViewModel
class StepsViewModel @Inject constructor(
    private val stepTracker: StepTracker,
    private val gamePreferences: GamePreferences,
    private val stepRepository: StepRepository,
    private val stepPreferences: StepPreferences,
    private val analytics: AnalyticsService,
    private val locationTracker: LocationTracker,
    private val authService: AuthService,
    private val stepBackfillService: app.umaia.android.data.sensor.StepBackfillService,
    private val nurRepository: app.umaia.android.domain.repository.NurRepository,
    private val notifications: app.umaia.android.data.notification.UmaiaNotifications,
    private val submissionCoordinator: app.umaia.android.data.sensor.StepSubmissionCoordinator,
) : ViewModel() {

    /**
     * Daily-share +10 Nur grant. Called when the user taps Share. Returns
     * true iff the bonus was granted (i.e. first share today). Mirrors the
     * grant on the server (`user_coins.balance += 10`) and fires a PostHog
     * event. The local guard in [GamePreferences] dedupes within Almaty-day.
     */
    fun claimDailyShare(): Boolean {
        val granted = gamePreferences.claimDailyShareNur()
        if (granted <= 0) return false
        analytics.dailyShareClaimed()
        viewModelScope.launch {
            runCatching { nurRepository.addNur(granted, "daily_share") }
        }
        return true
    }

    fun hasClaimedShareNurToday(): Boolean = gamePreferences.hasClaimedShareNurToday()

    private val _welcome = MutableStateFlow<WelcomeOverlay>(WelcomeOverlay.None)
    val welcome: StateFlow<WelcomeOverlay> = _welcome.asStateFlow()

    /**
     * Compute which welcome dialog to show, based on `isWelcomeShown(uid)` and
     * `daysSinceLastVisit()`. Idempotent: subsequent calls per session see the
     * dialog already dismissed (FirstVisit only fires once per uid; returning
     * dialog is one-shot per session).
     */
    fun checkWelcome() {
        val uid = authService.currentUserId ?: return
        if (_welcome.value !is WelcomeOverlay.None) return  // already deciding
        when {
            !gamePreferences.isWelcomeShown(uid) -> _welcome.value = WelcomeOverlay.FirstVisit
            else -> {
                val days = gamePreferences.daysSinceLastVisit()
                if (days >= 1) {
                    val bonus = (days * 5).coerceAtMost(50)
                    _welcome.value = WelcomeOverlay.Returning(daysAway = days, nurBonus = bonus)
                }
            }
        }
        gamePreferences.recordVisit()
    }

    fun dismissWelcome() {
        val uid = authService.currentUserId ?: return
        gamePreferences.markWelcomeShown(uid)
        _welcome.value = WelcomeOverlay.None
    }

    /**
     * Cold-start daily-step seed. Reads the persisted `dailySteps`, but resets
     * to 0 if `dailyStepsDate` is stale (Asia/Almaty rollover). Without this
     * the UI starts at 0 and the observer's `previous = 0` math triggers a
     * full re-submit of today's count (the v1.3.1 leaderboard-doubling bug).
     */
    private val initialDailySteps: Int = run {
        val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Almaty")).toString()
        if (gamePreferences.dailyStepsDate != today) {
            gamePreferences.dailySteps = 0
            gamePreferences.dailyStepsDate = today
            0
        } else {
            gamePreferences.dailySteps
        }
    }

    private val _uiState = MutableStateFlow(
        StepsUiState(
            isPermissionGranted = stepTracker.isAuthorized,
            dailySteps = initialDailySteps,
            nurFromSteps = stepsToNur(initialDailySteps),
            stepHistory = gamePreferences.getStepHistory(),
            totalSteps = gamePreferences.getStepHistory().values.sum() + initialDailySteps,
            reachedMilestones = getReachedMilestones(initialDailySteps),
            nextMilestone = getNextMilestone(initialDailySteps),
            suspectedCheating = stepPreferences.isSuspectedCheatingToday,
            healthConnectActive = healthConnectAuthorized()
        )
    )
    val uiState: StateFlow<StepsUiState> = _uiState.asStateFlow()

    init {
        // Initial HC permission probe — populates the tracker's cached authorization
        // state so the first composition reflects reality, not the default `false`.
        viewModelScope.launch {
            (stepTracker as? app.umaia.android.data.sensor.CompositeStepTracker)
                ?.healthConnect?.refreshAuthorization()
            doPermissionResult()
            recomputeAggregates(_uiState.value.dailySteps)
        }
        // v1.3.1: refresh aggregates whenever a non-step Nur grant lands
        // (daily-share +10). Replaces iOS NotificationCenter subscription.
        viewModelScope.launch {
            gamePreferences.bonusNurGranted.collect {
                recomputeAggregates(_uiState.value.dailySteps)
            }
        }
    }

    private var observeJob: Job? = null
    private var submitJob: Job? = null
    // Note: there's no in-memory pending-delta counter anymore. Each flush
    // computes the unsubmitted batch from `currentSteps − lastSubmittedDailyTotal`
    // (StepPreferences) so cold-starts / crashes can't double-submit today's count.

    /** Re-evaluate authorization after any permission flow returns and start tracking if granted. */
    fun onPermissionResult() {
        viewModelScope.launch {
            (stepTracker as? app.umaia.android.data.sensor.CompositeStepTracker)
                ?.healthConnect?.refreshAuthorization()
            doPermissionResult()
        }
    }

    private fun doPermissionResult() {
        val granted = stepTracker.isAuthorized
        _uiState.update { it.copy(
            isPermissionGranted = granted,
            healthConnectActive = healthConnectAuthorized()
        ) }
        analytics.stepPermissionRequested(granted)
        if (granted) startTracking()
    }

    private fun healthConnectAuthorized(): Boolean = when (val t = stepTracker) {
        is app.umaia.android.data.sensor.HealthConnectStepTracker -> t.isAuthorized
        is app.umaia.android.data.sensor.CompositeStepTracker -> t.healthConnect.isAuthorized
        else -> false
    }

    /** Returns (hc, sensor) daily step counts; -1 if that source is unauthorized. Debug only. */
    private suspend fun readPerSourceForDebug(): Pair<Int, Int> {
        val composite = stepTracker as? app.umaia.android.data.sensor.CompositeStepTracker
        val hc = composite?.healthConnect?.let {
            if (it.isAvailable && it.isAuthorized) runCatching { it.currentDailySteps() }.getOrDefault(-1) else -1
        } ?: -1
        val sensor = composite?.sensor?.let {
            if (it.isAuthorized) runCatching { it.currentDailySteps() }.getOrDefault(-1) else -1
        } ?: -1
        return hc to sensor
    }

    fun isHealthConnectAvailable(): Boolean = when (val t = stepTracker) {
        is app.umaia.android.data.sensor.HealthConnectStepTracker -> t.isAvailable
        is app.umaia.android.data.sensor.CompositeStepTracker -> t.healthConnect.isAvailable
        else -> false
    }

    fun healthConnectPermissions(): Set<String> = when (val t = stepTracker) {
        is app.umaia.android.data.sensor.HealthConnectStepTracker -> t.permissions
        is app.umaia.android.data.sensor.CompositeStepTracker -> t.healthConnect.permissions
        else -> emptySet()
    }

    fun healthConnectPermissionContract(): androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> =
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()

    /** Returns an Intent that opens Health Connect's main settings UI, or null if no
     *  resolvable activity exists on the device. */
    fun healthConnectSettingsIntent(context: android.content.Context): android.content.Intent? {
        val pm = context.packageManager
        val candidates = listOf(
            android.content.Intent("android.health.connect.action.HEALTH_HOME_SETTINGS"),
            android.content.Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
        )
        return candidates.firstOrNull { it.resolveActivity(pm) != null }
    }

    fun onAppear() {
        val steps = _uiState.value.dailySteps
        _uiState.update { it.copy(
            reachedMilestones = getReachedMilestones(steps),
            nextMilestone = getNextMilestone(steps)
        )}
        recomputeAggregates(steps)
    }

    fun startTracking() {
        if (!stepTracker.isAuthorized) return
        bootReconcile()
        catchUpOfflineSteps()
        startObserving()
        startSubmitLoop()
        stepPreferences.recordActive()
    }

    /** Reconcile local `lastSubmittedDailyTotal` with what the server already
     *  has for the calling user today (Asia/Almaty). Keeps a fresh-install
     *  device from re-submitting the day's count when the server still
     *  remembers earlier accepted submissions from another device or a
     *  prior session. Mirrors iOS `submissionCoordinator.reconcileAndSubmit`
     *  on app appear. */
    private fun bootReconcile() {
        viewModelScope.launch {
            val current = _uiState.value.dailySteps
            if (current <= 0) return@launch
            val serverToday = runCatching { stepRepository.getTodayServerSteps() }.getOrDefault(0)
            val suspected = _uiState.value.suspectedCheating
            submissionCoordinator.reconcileAndSubmit(
                currentDaily = current,
                knownServerTotal = serverToday,
                source = "boot",
                suspectedCheating = suspected,
            )
        }
    }

    fun stopTracking() {
        observeJob?.cancel()
        submitJob?.cancel()
        viewModelScope.launch { flushPendingSteps() }
        stepPreferences.recordActive()
    }

    private fun catchUpOfflineSteps() {
        val lastTs = stepPreferences.lastActiveTimestamp
        if (lastTs == 0L) {
            stepPreferences.recordActive()
            return
        }
        val now = System.currentTimeMillis()
        if ((now - lastTs) < 60_000L) return

        viewModelScope.launch {
            val offlineSteps = stepBackfillService.backfill(now)
            if (offlineSteps > 0) {
                _uiState.update { it.copy(offlineStepsCaught = offlineSteps) }
                analytics.offlineStepsCaught(offlineSteps)
                // Record the offline burst in today's history bucket so derived
                // totals (week/month/total Nur) catch the gap.
                val today = _uiState.value.dailySteps + offlineSteps
                gamePreferences.dailySteps = today
                gamePreferences.updateStepHistory(today)
                recomputeAggregates(today)
                // Route through the coordinator so this submitter races
                // safely against the periodic flush, the boot reconcile, and
                // (Phase B.3) the HC background worker. The 32s gate lives
                // inside the coordinator.
                submissionCoordinator.submitDelta(
                    currentDaily = today,
                    source = "offline",
                    suspectedCheating = _uiState.value.suspectedCheating,
                )
            }
            stepPreferences.recordActive()
        }
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            stepTracker.observeDailySteps().collect { steps ->
                val previous = _uiState.value.dailySteps
                val oldMilestones = getReachedMilestones(previous)
                val (hc, sensor) = readPerSourceForDebug()
                _uiState.update { it.copy(
                    dailySteps = steps,
                    nurFromSteps = stepsToNur(steps),
                    reachedMilestones = getReachedMilestones(steps),
                    nextMilestone = getNextMilestone(steps),
                    isSensorAvailable = true,
                    debugHcSteps = hc,
                    debugSensorSteps = sensor
                )}
                val newMilestones = getReachedMilestones(steps)
                if (newMilestones.size > oldMilestones.size) {
                    newMilestones.lastOrNull()?.let { analytics.stepMilestoneReached(it.steps) }
                }
                // The actual unsubmitted-delta is computed at flush time from
                // `currentSteps − lastSubmittedDailyTotal` (see [pendingDelta]),
                // so we just need to refresh local prefs/aggregates here.
                if (steps > previous) {
                    gamePreferences.dailySteps = steps
                    gamePreferences.updateStepHistory(steps)
                    recomputeAggregates(steps)
                    detectRewardUnlock()
                    checkGpsAtCheckpoint(previous, steps)
                }
                stepPreferences.recordActive()
            }
        }
    }

    /** Recompute weekly/monthly/total aggregates from stepHistory + today's pending. */
    private fun recomputeAggregates(today: Int) {
        val uid = authService.currentUserId
        val history = gamePreferences.getStepHistory()
        val totalSteps = history.values.sum() + today
        val weekly = gamePreferences.weeklySteps(today)
        val monthly = gamePreferences.monthlySteps(today)
        val monthlyNur = uid?.let { gamePreferences.effectiveMonthlyNur(today, it) } ?: (monthly / 100)
        val subtract = uid?.let { gamePreferences.monthlyNurSubtract(it) } ?: 0
        _uiState.update { it.copy(
            stepHistory = history,
            totalSteps = totalSteps,
            weeklySteps = weekly,
            monthlySteps = monthly,
            monthlyNur = monthlyNur,
            monthlyNurSubtract = subtract,
            shareClaimedToday = gamePreferences.hasClaimedShareNurToday(),
        )}
        // Refresh today's full server-truth Nur (steps + bonuses) so the
        // share image reflects the actual total. Fire-and-forget; falls back
        // to nurFromSteps if the call fails.
        viewModelScope.launch {
            val n = runCatching { stepRepository.getTodayServerNur() }.getOrDefault(0)
            if (n > 0) _uiState.update { it.copy(todayServerNur = n) }
        }
    }

    /**
     * Cross-tab unlock detection — fires once when monthly Nur first crosses
     * the reward target. Runs every step batch regardless of which tab is on
     * screen. Per-(rewardId, periodId, userId) dedupe in GamePreferences
     * prevents the notification from re-firing if the user briefly drops
     * below the target and crosses again.
     */
    private fun detectRewardUnlock() {
        val uid = authService.currentUserId ?: return
        val nur = _uiState.value.monthlyNur
        if (nur < WinnerStatusViewModel.MONTHLY_REWARD_COST_NUR) return
        val periodId = gamePreferences.currentMonthPeriodId
        val rewardId = WinnerStatusViewModel.REWARD_ID
        if (gamePreferences.isUnlockNotified(rewardId, periodId, uid)) return
        gamePreferences.markUnlockNotified(rewardId, periodId, uid)
        analytics.notifyRewardUnlocked(rewardId, periodId, uid, partner = "Umaia", item = "T-shirt")
        // v1.4.0: also fire a system tray notification (mirrors iOS).
        notifications.notifyRewardUnlocked(partner = "Umaia", item = "T-shirt")
    }

    private fun startSubmitLoop() {
        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            while (true) {
                delay(35_000L)
                flushPendingSteps()
            }
        }
    }

    private suspend fun flushPendingSteps() {
        // All submission logic — delta computation, baseline claim, CAS
        // rollback, and the 32s anti-spam gate — lives in the coordinator.
        // This method is a thin shim that exists for the existing call sites
        // (periodic loop, stopTracking).
        submissionCoordinator.submitDelta(
            currentDaily = _uiState.value.dailySteps,
            source = "live",
            suspectedCheating = _uiState.value.suspectedCheating,
        )
    }

    private fun checkGpsAtCheckpoint(previousSteps: Int, currentSteps: Int) {
        val lastCheckpoint = stepPreferences.lastGpsCheckpoint
        val nextCheckpoint = ((currentSteps / 1000) * 1000)
        if (nextCheckpoint <= lastCheckpoint || currentSteps < 1000) return

        stepPreferences.lastGpsCheckpoint = nextCheckpoint
        viewModelScope.launch {
            val location = locationTracker.currentLocation() ?: return@launch
            val snap = GpsSnapshot(location.latitude, location.longitude, nextCheckpoint)
            stepPreferences.addGpsSnapshot(snap)

            val snapshots = stepPreferences.gpsSnapshots
            if (snapshots.size >= 5) {
                val last5 = snapshots.takeLast(5)
                val anchor = last5.first()
                val maxDist = last5.drop(1).maxOf { s ->
                    LocationTracker.distance(anchor.latitude, anchor.longitude, s.latitude, s.longitude)
                }
                if (maxDist < 100.0) {
                    stepPreferences.flagSuspectedCheating()
                    _uiState.update { it.copy(suspectedCheating = true) }
                    analytics.suspectedStepCheating(currentSteps, maxDist)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
        submitJob?.cancel()
    }
}

fun getReachedMilestones(steps: Int): List<StepMilestone> = allMilestones.filter { steps >= it.steps }
fun getNextMilestone(steps: Int): StepMilestone? = allMilestones.firstOrNull { steps < it.steps }

fun getStepNarrative(steps: Int, ru: Boolean = false): String = when {
    steps == 0 -> if (ru) "Степь ждёт первого шага..." else "The steppe awaits your first step..."
    steps < 1000 -> if (ru) "Путь начинается с одного шага." else "A journey begins with a single step."
    steps < 3000 -> if (ru) "Твои ноги помнят дорогу." else "Your feet remember the path."
    steps < 6000 -> if (ru) "Нур течёт с каждым шагом." else "Nur flows with every step."
    steps < 10000 -> if (ru) "Племя чувствует твою энергию!" else "The tribe feels your energy!"
    steps < 15000 -> if (ru) "Ты несёшь свет в деревню!" else "You carry light to the village!"
    else -> if (ru) "Легенда степи шагает сегодня!" else "A legend of the steppe walks today!"
}
