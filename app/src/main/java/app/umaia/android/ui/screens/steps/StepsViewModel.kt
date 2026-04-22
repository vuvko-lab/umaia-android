package app.umaia.android.ui.screens.steps

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.data.local.GameStateStore
import app.umaia.android.data.local.GpsSnapshot
import app.umaia.android.data.local.StepPreferences
import app.umaia.android.data.location.LocationTracker
import app.umaia.android.data.sensor.GoogleFitStepTracker
import app.umaia.android.domain.engine.stepsToNur
import app.umaia.android.domain.model.StepMilestone
import app.umaia.android.domain.model.allMilestones
import app.umaia.android.domain.repository.StepRepository
import app.umaia.android.domain.repository.StepTracker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StepsUiState(
    val dailySteps: Int = 0,
    val totalSteps: Int = 0,
    val nurFromSteps: Int = 0,
    val reachedMilestones: List<StepMilestone> = emptyList(),
    val nextMilestone: StepMilestone? = null,
    val isSensorAvailable: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val offlineStepsCaught: Int = 0,
    val stepHistory: Map<String, Int> = emptyMap(),
    val suspectedCheating: Boolean = false
)

@HiltViewModel
class StepsViewModel @Inject constructor(
    private val stepTracker: StepTracker,
    private val gamePreferences: GamePreferences,
    private val stepRepository: StepRepository,
    private val stepPreferences: StepPreferences,
    private val gameStateStore: GameStateStore,
    private val analytics: AnalyticsService,
    private val locationTracker: LocationTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StepsUiState(
            isPermissionGranted = stepTracker.isAuthorized,
            totalSteps = gamePreferences.totalSteps,
            stepHistory = gamePreferences.getStepHistory(),
            suspectedCheating = stepPreferences.isSuspectedCheatingToday
        )
    )
    val uiState: StateFlow<StepsUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var submitJob: Job? = null
    private var pendingDelta = 0

    /** Returns the Google Fit sign-in intent for fitness permissions. */
    fun getSignInIntent(context: Context): Intent {
        val tracker = stepTracker as GoogleFitStepTracker
        val account = GoogleSignIn.getAccountForExtension(context, tracker.fitnessOptions)
        return GoogleSignIn.getClient(context,
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder()
                .addExtension(tracker.fitnessOptions)
                .build()
        ).signInIntent
    }

    /** Called after the Google Fit sign-in result comes back. */
    fun onFitPermissionResult() {
        val granted = stepTracker.isAuthorized
        _uiState.update { it.copy(isPermissionGranted = granted) }
        analytics.stepPermissionRequested(granted)
        if (granted) startTracking()
    }

    fun onAppear() {
        val steps = _uiState.value.dailySteps
        _uiState.update { it.copy(
            reachedMilestones = getReachedMilestones(steps),
            nextMilestone = getNextMilestone(steps)
        )}
    }

    fun startTracking() {
        if (!stepTracker.isAuthorized) return
        catchUpOfflineSteps()
        startObserving()
        startSubmitLoop()
        stepPreferences.recordActive()
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
            val lastDate = Date(lastTs)
            val offlineSteps = stepTracker.querySteps(lastDate, Date())
            if (offlineSteps > 0) {
                _uiState.update { it.copy(offlineStepsCaught = offlineSteps) }
                analytics.offlineStepsCaught(offlineSteps)
                gamePreferences.addTotalSteps(offlineSteps)
                _uiState.update { it.copy(totalSteps = gamePreferences.totalSteps) }
                stepRepository.submitSteps(offlineSteps, "offline")
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
                _uiState.update { it.copy(
                    dailySteps = steps,
                    nurFromSteps = stepsToNur(steps),
                    reachedMilestones = getReachedMilestones(steps),
                    nextMilestone = getNextMilestone(steps),
                    isSensorAvailable = true
                )}
                val newMilestones = getReachedMilestones(steps)
                if (newMilestones.size > oldMilestones.size) {
                    newMilestones.lastOrNull()?.let { analytics.stepMilestoneReached(it.steps) }
                }
                val delta = maxOf(0, steps - previous)
                if (delta > 0) {
                    pendingDelta += delta
                    gamePreferences.dailySteps = steps
                    gamePreferences.addTotalSteps(delta)
                    gamePreferences.updateStepHistory(steps)
                    _uiState.update { it.copy(
                        totalSteps = gamePreferences.totalSteps,
                        stepHistory = gamePreferences.getStepHistory()
                    )}
                    gameStateStore.update { state ->
                        if (state.stepsSynced) state else state.copy(stepsSynced = true)
                    }
                    checkGpsAtCheckpoint(previous, steps)
                }
                stepPreferences.recordActive()
            }
        }
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
        val batch = pendingDelta
        if (batch <= 0) return
        pendingDelta = 0
        val suspected = _uiState.value.suspectedCheating
        val result = stepRepository.submitSteps(batch, "live", suspected)
        if (!result.success) {
            pendingDelta += batch // put back on failure
        }
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
