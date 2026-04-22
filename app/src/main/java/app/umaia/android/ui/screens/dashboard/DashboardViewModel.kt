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
    val welcomeBack: Boolean = false
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

                val lastVisit = gamePrefs.lastVisitDate
                val today = java.time.LocalDate.now().toString()
                val isReturn = lastVisit != null && lastVisit != today
                gamePrefs.lastVisitDate = today

                _uiState.value = DashboardUiState(
                    profile = profile,
                    welcomeBack = isReturn && nurEarned > 0
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
}
