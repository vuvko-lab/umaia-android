package app.umaia.android.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.domain.model.UserProfile
import app.umaia.android.domain.repository.LoginRepository
import app.umaia.android.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val profile: UserProfile? = null,
    val dailySteps: Int = 0,
    val welcomeBack: Boolean = false,
    val showWelcome: Boolean = false,
    val showReturnWelcome: Boolean = false,
    val daysAway: Int = 0,
    val returnNurBonus: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val loginRepository: LoginRepository,
    private val gamePrefs: GamePreferences,
    private val analytics: AnalyticsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            runCatching {
                val profile = profileRepository.getProfile()
                val nurEarned = loginRepository.recordDailyLogin()

                val uid = profile.userId
                val isFirstVisit = !gamePrefs.isWelcomeShown(uid)
                val daysAway = gamePrefs.daysSinceLastVisit()
                val isReturnAfterGap = daysAway >= 1

                if (isFirstVisit) {
                    gamePrefs.markWelcomeShown(uid)
                }
                gamePrefs.recordVisit()

                val returnBonus = if (isReturnAfterGap) minOf(daysAway * 5, 50) else 0

                _uiState.value = DashboardUiState(
                    profile = profile,
                    welcomeBack = false,
                    showWelcome = isFirstVisit,
                    showReturnWelcome = isReturnAfterGap && !isFirstVisit,
                    daysAway = daysAway,
                    returnNurBonus = returnBonus
                )
            }
        }
    }

    fun refreshSteps() {
        _uiState.update { it.copy(dailySteps = gamePrefs.dailySteps) }
    }

    fun dismissWelcomeBack() {
        _uiState.update { it.copy(welcomeBack = false) }
    }

    fun dismissWelcome() {
        _uiState.update { it.copy(showWelcome = false) }
    }

    fun dismissReturnWelcome() {
        _uiState.update { it.copy(showReturnWelcome = false) }
    }
}
