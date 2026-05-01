package app.umaia.android.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.auth.AuthService
import app.umaia.android.data.local.AppPreferences
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.data.local.StepPreferences
import app.umaia.android.data.local.ThemeMode
import app.umaia.android.domain.model.UserProfile
import app.umaia.android.domain.repository.LoginRepository
import app.umaia.android.domain.repository.NurRepository
import app.umaia.android.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val liveNur: Int = 0,
    val weekNur: Int = 0,
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
    private val loginRepository: LoginRepository,
    private val nurRepository: NurRepository,
    private val authService: AuthService,
    private val gamePreferences: GamePreferences,
    private val stepPreferences: StepPreferences,
    private val analytics: AnalyticsService,
    val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(email = authService.tokenStorage.email ?: ""))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeMode
    val language: StateFlow<String> = appPreferences.language

    init {
        // v1.3.2: mirror the repository's profile flow into our UI state so that
        // mutations made elsewhere (e.g. CompanyCodeViewModel.setCompanyCode →
        // implicit getProfile() refresh) propagate to all observers — including
        // the BottomNavBar's `showSeer` gate. Without this, joining a company
        // code only updates the repo cache; the NavGraph-level VM stays stale
        // until the next explicit `load()`, hiding the Seer tab on first paint.
        viewModelScope.launch {
            profileRepository.observeProfile().collect { fresh ->
                if (fresh != null) {
                    _uiState.update { it.copy(profile = fresh) }
                }
            }
        }
        // v1.3.3: refresh Total Nur (server-truth balance) whenever any
        // non-step Nur grant lands (daily-share, wisdom-test quizNur, Oracle
        // bonus). Without this the Profile tab keeps showing the pre-grant
        // balance until the user navigates away and back.
        viewModelScope.launch {
            gamePreferences.bonusNurGranted.collect {
                runCatching { nurRepository.getBalance() }.getOrNull()?.let { balance ->
                    _uiState.update { it.copy(liveNur = balance) }
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            val profile = runCatching { profileRepository.getProfile() }.getOrNull()
            // v1.3.3: Total Nur is now sourced from `user_coins.balance`
            // (server-truth) instead of the local `gamePreferences.totalNur`
            // approximation. The server handles the asymptotic step→Nur
            // formula plus all bonus Nur (login, share, wisdom-test, oracle)
            // in one consistent place — local was drifting from server, e.g.
            // showing +30 for Oracle when the actual server credit was ~+69.
            // Falls back to local derivation while the server fetch is in
            // flight so the UI never shows "0".
            val localNur = profile?.userId?.let {
                gamePreferences.totalNur(includingDaily = gamePreferences.dailySteps, userId = it)
            } ?: 0
            val serverNur = runCatching { nurRepository.getBalance() }.getOrNull() ?: localNur
            val weekNur = gamePreferences.weeklySteps(gamePreferences.dailySteps) / 100
            _uiState.update { it.copy(
                profile = profile,
                liveNur = serverNur,
                weekNur = weekNur,
                email = authService.tokenStorage.email ?: ""
            ) }
        }
        // v1.3.2: claim today's daily-login Nur (idempotent server-side via
        // `claim_daily_login` RPC). Mirrors the iOS DashboardViewModel call
        // site that was lost when we deleted Dashboard in v1.3. Failures
        // are silent — the server has its own per-Almaty-day dedupe.
        viewModelScope.launch {
            runCatching { loginRepository.recordDailyLogin() }
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
                // Wipe ALL local per-account state so a fresh sign-in on the
                // same device doesn't inherit the prior account's "share
                // already claimed today", "oracle bonus already granted",
                // step history, accumulators, etc. Without gamePreferences
                // here, the next user sees stale UI flags that map to no
                // server-side row → frustration + lost bonuses.
                stepPreferences.reset()
                gamePreferences.reset()
                authService.deleteAccount()
                analytics.signedOut()
            }.onFailure { e ->
                _uiState.update { it.copy(isDeleting = false, deleteError = e.message) }
            }
        }
    }
}
