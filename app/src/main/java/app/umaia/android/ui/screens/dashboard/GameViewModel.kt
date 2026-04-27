package app.umaia.android.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.local.GameStateStore
import app.umaia.android.domain.engine.*
import app.umaia.android.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SessionInfo(
    val minutes: Int = 0,
    val isWarning: Boolean = false,
    val isDecaying: Boolean = false,
    val showSessionComplete: Boolean = false
)

sealed class PopulationEvent {
    data class Grew(val count: Int) : PopulationEvent()
    data class Declined(val count: Int) : PopulationEvent()
}

private const val SESSION_WARN_MINUTES = 15
private const val SESSION_DECAY_MINUTES = 15

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameStateStore: GameStateStore,
    private val nurRepository: app.umaia.android.domain.repository.NurRepository,
    private val analytics: AnalyticsService
) : ViewModel() {

    val gameState: StateFlow<GameState> = gameStateStore.stateFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameState())

    private val _session = MutableStateFlow(SessionInfo())
    val session: StateFlow<SessionInfo> = _session.asStateFlow()

    private val _populationEvent = MutableStateFlow<PopulationEvent?>(null)
    val populationEvent: StateFlow<PopulationEvent?> = _populationEvent.asStateFlow()

    private var tickJob: Job? = null
    private var sessionJob: Job? = null
    private var sessionStart = System.currentTimeMillis()

    fun start() {
        sessionStart = System.currentTimeMillis()
        _session.value = SessionInfo()

        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                val elapsed = ((System.currentTimeMillis() - sessionStart) / 60_000).toInt()
                val wasDecaying = _session.value.isDecaying
                _session.value = SessionInfo(
                    minutes = elapsed,
                    isWarning = elapsed >= SESSION_WARN_MINUTES,
                    isDecaying = elapsed >= SESSION_DECAY_MINUTES
                )
                if (_session.value.isDecaying && !wasDecaying) {
                    analytics.sessionDecayStarted(elapsed)
                }
            }
        }

        if (tickJob?.isActive == true) return
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val oldPop = gameStateStore.load().population
                gameStateStore.update { state ->
                    var next = tick(state)
                    if (_session.value.isDecaying) {
                        val decayed = next.nur - (NUR_DECAY_PER_MINUTE / (60_000.0 / TICK_MS))
                        next = next.copy(nur = maxOf(0.0, decayed))
                    }
                    checkAndCompleteQuests(next)
                }
                val newPop = gameStateStore.load().population
                when {
                    newPop > oldPop -> _populationEvent.value = PopulationEvent.Grew(newPop)
                    newPop < oldPop -> _populationEvent.value = PopulationEvent.Declined(newPop)
                }
                checkNurSync()
            }
        }
        syncNurFromRemote()
        backfillOracleCompleted()
    }

    fun stop() {
        if (_session.value.isDecaying) {
            _session.value = _session.value.copy(showSessionComplete = true)
        }
        tickJob?.cancel()
        tickJob = null
        sessionJob?.cancel()
        sessionJob = null
    }

    fun dismissSessionComplete() {
        _session.value = _session.value.copy(showSessionComplete = false)
    }

    fun clearPopulationEvent() {
        _populationEvent.value = null
    }

    @Volatile private var buildInFlight = false

    fun buildBuilding(type: BuildingId) {
        // Debounce: a single tap should not enqueue two builds
        if (buildInFlight) return
        buildInFlight = true
        viewModelScope.launch {
            try {
                gameStateStore.update { state ->
                    val built = buildBuilding(state, type) ?: return@update state
                    val def = buildingDef(type)
                    // Auto-assign one worker to the new building if it has slots and population is free
                    val withWorker = if (def.maxWorkers > 0 && built.totalWorkers < built.population) {
                        val newInstance = built.buildings.last()
                        assignWorker(built, newInstance.instanceId, +1)
                    } else built
                    analytics.buildingBuilt(type.name.lowercase())
                    checkAndCompleteQuests(withWorker)
                }
            } finally {
                buildInFlight = false
            }
        }
    }

    fun assignWorker(instanceId: String, delta: Int) {
        viewModelScope.launch {
            gameStateStore.update { state ->
                val next = assignWorker(state, instanceId, delta)
                checkAndCompleteQuests(next)
            }
        }
    }

    fun markPanelOpened(key: String) {
        viewModelScope.launch {
            gameStateStore.update { state ->
                if (key in state.panelsOpened) state
                else checkAndCompleteQuests(state.copy(panelsOpened = state.panelsOpened + key))
            }
        }
    }

    private fun checkAndCompleteQuests(state: GameState): GameState {
        var s = state
        val pending = allQuests.firstOrNull { it.id !in s.completedQuests } ?: return s
        if (!isQuestComplete(s, pending)) return s
        s = s.copy(
            completedQuests = s.completedQuests + pending.id,
            nur = s.nur + pending.nurReward
        )
        analytics.questCompleted(pending.id)
        viewModelScope.launch {
            runCatching { nurRepository.addNur(pending.nurReward, "quest_${pending.id}") }
        }
        return checkAndCompleteQuests(s)
    }

    private var lastNurSync = 0.0
    private suspend fun checkNurSync() {
        val state = gameStateStore.load()
        if (state.nur != lastNurSync) {
            lastNurSync = state.nur
            runCatching { nurRepository.addNur(0, "sync") }
        }
    }

    private fun syncNurFromRemote() {
        viewModelScope.launch {
            val remote = runCatching { nurRepository.getBalance() }.getOrNull()?.toDouble() ?: return@launch
            gameStateStore.update { state ->
                if (remote > state.nur) state.copy(nur = remote) else state
            }
        }
    }

    private fun backfillOracleCompleted() {
        // OracleViewModel sets oracleCompleted when assessment is submitted.
        // No backfill needed here — guard is handled in OracleViewModel.
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
