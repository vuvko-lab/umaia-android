package app.umaia.android.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.auth.AuthService
import app.umaia.android.data.local.AppPreferences
import app.umaia.android.data.local.GameStateStore
import app.umaia.android.data.local.StepPreferences
import app.umaia.android.data.local.ThemeMode
import app.umaia.android.domain.model.UserProfile
import app.umaia.android.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val liveNur: Int = 0,
    val email: String = "",
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    // Password change
    val isChangingPassword: Boolean = false,
    val passwordChangeSuccess: Boolean = false,
    val passwordChangeError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authService: AuthService,
    private val gameStateStore: GameStateStore,
    private val stepPreferences: StepPreferences,
    private val analytics: AnalyticsService,
    val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(email = authService.tokenStorage.email ?: ""))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeMode
    val language: StateFlow<String> = appPreferences.language

    init {
        viewModelScope.launch {
            gameStateStore.stateFlow.collect { state ->
                _uiState.update { it.copy(liveNur = state.nur.toInt()) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            val profile = runCatching { profileRepository.getProfile() }.getOrNull()
            _uiState.update { it.copy(profile = profile, email = authService.tokenStorage.email ?: "") }
        }
    }

    fun setThemeMode(mode: ThemeMode) = appPreferences.setThemeMode(mode)

    fun setLanguage(lang: String) = appPreferences.setLanguage(lang)

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true, passwordChangeError = null, passwordChangeSuccess = false) }
            runCatching { authService.changePassword(newPassword) }
                .onSuccess { _uiState.update { it.copy(isChangingPassword = false, passwordChangeSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isChangingPassword = false, passwordChangeError = e.message) } }
        }
    }

    fun clearPasswordState() {
        _uiState.update { it.copy(passwordChangeSuccess = false, passwordChangeError = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            analytics.signedOut()
            authService.signOut()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = null) }
            runCatching {
                gameStateStore.reset()
                stepPreferences.reset()
                authService.deleteAccount()
                analytics.signedOut()
            }.onFailure { e ->
                _uiState.update { it.copy(isDeleting = false, deleteError = e.message) }
            }
        }
    }
}
