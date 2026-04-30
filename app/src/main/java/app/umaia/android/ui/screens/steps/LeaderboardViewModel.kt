package app.umaia.android.ui.screens.steps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.data.notification.UmaiaNotifications
import app.umaia.android.domain.repository.LeaderboardData
import app.umaia.android.domain.repository.LeaderboardPeriod
import app.umaia.android.domain.repository.ProfileRepository
import app.umaia.android.domain.repository.StepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    // v1.3.1: Default to MONTHLY so MonthlyStandingHero on the Walk tab is
    // anchored on the same period the rewards are scored against.
    val period: LeaderboardPeriod = LeaderboardPeriod.MONTHLY,
    val data: LeaderboardData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val stepRepository: StepRepository,
    private val profileRepository: ProfileRepository,
    private val gamePreferences: GamePreferences,
    private val notifications: UmaiaNotifications,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var isRefreshing = false
    private var companyCode: String? = null

    init {
        // Refetch whenever the user's companyCode changes (joining a cohort
        // narrows the leaderboard from public-pool to company-only).
        viewModelScope.launch {
            profileRepository.observeProfile()
                .map { it?.companyCode }
                .distinctUntilChanged()
                .collect {
                    companyCode = it
                    refresh()
                }
        }
        refresh()
    }

    fun refresh() {
        if (isRefreshing) return
        isRefreshing = true
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                stepRepository.getLeaderboard(_uiState.value.period, companyCode)
            }.onSuccess { data ->
                _uiState.update { it.copy(data = data, isLoading = false) }
                detectTopRankDrop(data)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            isRefreshing = false
        }
    }

    fun switchPeriod(period: LeaderboardPeriod) {
        if (period == _uiState.value.period) return
        _uiState.update { it.copy(period = period) }
        isRefreshing = false
        refresh()
    }

    /**
     * v1.4.0: rank-drop notification. Mirrors iOS `notifyTopRankDrop`. We
     * only watch the MONTHLY period — that's the cohort that drives the
     * paid reward — and only fire on the transition top-3 → outside-top-3.
     * GamePreferences keys the last-known status by periodId so a fresh
     * month never spuriously fires.
     */
    private fun detectTopRankDrop(data: LeaderboardData) {
        if (_uiState.value.period != LeaderboardPeriod.MONTHLY) return
        val rank = data.myRank ?: return
        val periodId = gamePreferences.currentMonthPeriodId
        val wasTopThree = gamePreferences.lastWasTopThree(periodId)
        val isTopThree = rank in 1..3
        if (wasTopThree && !isTopThree) {
            notifications.notifyTopRankDrop(rank)
        }
        gamePreferences.setLastTopThreeStatus(periodId, isTopThree)
    }
}
