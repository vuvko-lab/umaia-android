package app.umaia.android.ui.screens.companycode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.auth.AuthService
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.domain.repository.InvalidCompanyCodeException
import app.umaia.android.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompanyCodeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false,
)

@HiltViewModel
class CompanyCodeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val gamePreferences: GamePreferences,
    private val authService: AuthService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyCodeUiState())
    val uiState: StateFlow<CompanyCodeUiState> = _uiState.asStateFlow()

    fun submit(rawCode: String) {
        val code = rawCode.trim().uppercase()
        if (code.isEmpty()) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { profileRepository.setCompanyCode(code) }
                .onSuccess {
                    val uid = authService.currentUserId
                    if (uid != null) gamePreferences.markCompanyChoiceMade(uid)
                    _uiState.update { it.copy(isLoading = false, completed = true) }
                }
                .onFailure { e ->
                    val msg = when (e) {
                        is InvalidCompanyCodeException -> "INVALID"
                        else -> e.message ?: "UNKNOWN"
                    }
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                }
        }
    }

    fun skip() {
        val uid = authService.currentUserId ?: return
        gamePreferences.markCompanyChoiceMade(uid)
        _uiState.update { it.copy(completed = true) }
    }
}
