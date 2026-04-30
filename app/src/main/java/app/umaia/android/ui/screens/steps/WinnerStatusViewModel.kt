package app.umaia.android.ui.screens.steps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.domain.repository.MonthlyWinnerStatus
import app.umaia.android.domain.repository.RewardClaim
import app.umaia.android.domain.repository.RewardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WinnerStatusUiState(
    val status: MonthlyWinnerStatus? = null,
    val claim: RewardClaim? = null,
    val alreadyClaimed: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Surfaces the server-authoritative monthly podium status for the Walk tab.
 * Drives the `CongratsBanner` (winner / target-hit / podium-full) and gates
 * the T-shirt claim button. Refresh on init and whenever the local monthly
 * Nur display crosses the target (StepsScreen calls [refresh]).
 */
@HiltViewModel
class WinnerStatusViewModel @Inject constructor(
    private val rewardRepository: RewardRepository,
    private val gamePreferences: GamePreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WinnerStatusUiState())
    val uiState: StateFlow<WinnerStatusUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { rewardRepository.getMonthlyWinnerStatus() }
                .onSuccess { status ->
                    val claim = runCatching {
                        rewardRepository.getClaim(REWARD_ID, status.periodId)
                    }.getOrNull()
                    _uiState.update {
                        it.copy(
                            status = status,
                            claim = claim,
                            alreadyClaimed = claim != null,
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    /** Called by RewardClaimSheet after a successful submit so the tile flips
     *  to "claimed" without waiting for the next refresh. */
    fun markLocallyClaimed(claim: RewardClaim, userId: String) {
        gamePreferences.claimReward(REWARD_ID, claim.periodId, userId)
        gamePreferences.recordMonthlyNurSubtract(MONTHLY_REWARD_COST_NUR, userId)
        gamePreferences.recordRewardSpend(MONTHLY_REWARD_COST_NUR, userId)
        _uiState.update { it.copy(claim = claim, alreadyClaimed = true) }
    }

    companion object {
        /** Stable id matching the server-side `reward_targets` row. */
        const val REWARD_ID = "tshirt_monthly"
        /** Server-side `reward_targets.target_nur`. */
        const val MONTHLY_REWARD_COST_NUR = 2000
    }
}
