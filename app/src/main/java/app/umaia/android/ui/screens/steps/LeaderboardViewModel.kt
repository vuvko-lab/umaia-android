package app.umaia.android.ui.screens.steps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.domain.repository.LeaderboardData
import app.umaia.android.domain.repository.LeaderboardPeriod
import app.umaia.android.domain.repository.StepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val period: LeaderboardPeriod = LeaderboardPeriod.WEEKLY,
    val data: LeaderboardData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val stepRepository: StepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var isRefreshing = false

    init { refresh() }

    fun refresh() {
        if (isRefreshing) return
        isRefreshing = true
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                stepRepository.getLeaderboard(_uiState.value.period)
            }.onSuccess { data ->
                _uiState.update { it.copy(data = data, isLoading = false) }
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
}
