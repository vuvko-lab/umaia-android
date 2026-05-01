package app.umaia.android.ui.screens.auth

import android.content.Context
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.BuildConfig
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.auth.AuthResult
import app.umaia.android.data.auth.AuthService
import app.umaia.android.data.auth.AuthState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    /** Exposed (not private) so NavGraph's CompanyGateRoute can read
     *  `currentUserId` for the stale-profile defensive guard. */
    val authService: AuthService,
    private val analytics: AnalyticsService
) : ViewModel() {

    val authState: StateFlow<AuthState> = authService.authState

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    authService.currentUserEmail?.let { analytics.identify(it) }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val r = authService.signIn(email, password)) {
                is AuthResult.Success  -> {
                    analytics.identify(email)
                    analytics.signedIn("email")
                }
                is AuthResult.Failure  -> _error.value = r.message
                else -> {}
            }
            _loading.value = false
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val r = authService.signUp(email, password)) {
                is AuthResult.Success              -> {
                    analytics.identify(email)
                    analytics.signedIn("email_signup")
                }
                is AuthResult.NeedsEmailConfirmation -> _error.value = "Check your email to confirm."
                is AuthResult.Failure              -> _error.value = r.message
            }
            _loading.value = false
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val idToken: String? = when (val cred = result.credential) {
                    is GoogleIdTokenCredential -> cred.idToken
                    is CustomCredential ->
                        if (cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            try { GoogleIdTokenCredential.createFrom(cred.data).idToken }
                            catch (e: GoogleIdTokenParsingException) { null }
                        } else null
                    else -> null
                }
                if (idToken != null) {
                    when (val r = authService.signInWithGoogleToken(idToken)) {
                        is AuthResult.Success -> {
                            authService.currentUserEmail?.let { analytics.identify(it) }
                            analytics.signedIn("google")
                        }
                        is AuthResult.Failure -> _error.value = r.message
                        else -> {}
                    }
                } else {
                    _error.value = "Google sign-in failed: unrecognized credential type"
                }
            } catch (e: GetCredentialCancellationException) {
                // User dismissed the picker — not an error, just stop loading
            } catch (e: Exception) {
                _error.value = e.message ?: "Google sign-in failed"
            }
            _loading.value = false
        }
    }

    fun clearError() { _error.value = null }
}
