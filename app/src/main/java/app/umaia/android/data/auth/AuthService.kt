package app.umaia.android.data.auth

import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String) : AuthState()
}

sealed class AuthResult {
    data class Success(val userId: String) : AuthResult()
    object NeedsEmailConfirmation : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

@Singleton
class AuthService @Inject constructor(
    val tokenStorage: EncryptedTokenStorage,
    @Named("supabaseUrl") private val supabaseUrl: String,
    @Named("supabaseAnonKey") private val anonKey: String
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private var refreshJob: Job? = null

    val currentUserId: String? get() = tokenStorage.userId

    init { restoreSession() }

    // ── Email / Password ──────────────────────────────────────────────────────

    suspend fun signIn(email: String, password: String): AuthResult =
        performAuth("/auth/v1/token?grant_type=password",
            mapOf("email" to email, "password" to password))

    suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            val data = post("/auth/v1/signup", mapOf("email" to email, "password" to password))
            val response = runCatching { json.decodeFromString<TokenResponse>(data) }.getOrNull()
            if (response != null) {
                saveTokens(response)
                AuthResult.Success(response.user.id)
            } else if (data.contains("\"id\"")) {
                AuthResult.NeedsEmailConfirmation
            } else {
                AuthResult.Failure("Unexpected signup response")
            }
        } catch (e: Exception) {
            AuthResult.Failure(e.message ?: "Unknown error")
        }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────

    suspend fun signInWithGoogleToken(idToken: String): AuthResult =
        performAuth("/auth/v1/token?grant_type=id_token",
            mapOf("provider" to "google", "id_token" to idToken))

    // ── Sign out ──────────────────────────────────────────────────────────────

    suspend fun signOut() {
        runCatching { post("/auth/v1/logout", emptyMap()) }
        tokenStorage.clear()
        refreshJob?.cancel()
        _authState.value = AuthState.Unauthenticated
    }

    // ── Delete account ────────────────────────────────────────────────────────

    /**
     * Calls the `delete_own_account` RPC and clears local session state.
     * The Supabase function must be deployed — see supabase_anticheat_migration.sql.
     */
    suspend fun deleteAccount() {
        post("/rest/v1/rpc/delete_own_account", emptyMap())
        tokenStorage.clear()
        refreshJob?.cancel()
        _authState.value = AuthState.Unauthenticated
    }

    // ── Change password ───────────────────────────────────────────────────────

    /** Updates the password for the currently authenticated user. */
    suspend fun changePassword(newPassword: String) {
        withContext(Dispatchers.IO) {
            val token = tokenStorage.accessToken ?: error("Not authenticated")
            val bodyJson = JSONObject(mapOf("password" to newPassword)).toString()
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/user")
                .addHeader("Content-Type", "application/json")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .put(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val text = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                error(parseErrorMessage(text) ?: "HTTP ${response.code}")
            }
        }
    }

    // ── Token refresh ─────────────────────────────────────────────────────────

    suspend fun refreshSession() {
        val refresh = tokenStorage.refreshToken ?: run {
            _authState.value = AuthState.Unauthenticated
            return
        }
        val result = performAuth("/auth/v1/token?grant_type=refresh_token",
            mapOf("refresh_token" to refresh))
        if (result is AuthResult.Failure) {
            tokenStorage.clear()
            _authState.value = AuthState.Unauthenticated
        }
    }

    // ── Headers for REST calls ────────────────────────────────────────────────

    fun authenticatedHeaders(): Map<String, String> {
        val token = tokenStorage.accessToken ?: error("Not authenticated")
        return mapOf(
            "Authorization" to "Bearer $token",
            "apikey" to anonKey,
            "Content-Type" to "application/json"
        )
    }

    val baseUrl: String get() = supabaseUrl

    // ── Private ───────────────────────────────────────────────────────────────

    private suspend fun performAuth(path: String, body: Map<String, Any>): AuthResult = try {
        val data = post(path, body)
        val response = json.decodeFromString<TokenResponse>(data)
        saveTokens(response)
        AuthResult.Success(response.user.id)
    } catch (e: Exception) {
        AuthResult.Failure(e.message ?: "Unknown error")
    }

    private fun saveTokens(response: TokenResponse) {
        tokenStorage.accessToken  = response.access_token
        tokenStorage.refreshToken = response.refresh_token
        tokenStorage.userId       = response.user.id
        tokenStorage.email        = response.user.email
        _authState.value = AuthState.Authenticated(response.user.id)
        scheduleTokenRefresh()
    }

    private fun restoreSession() {
        val userId = tokenStorage.userId
        if (userId != null) {
            _authState.value = AuthState.Authenticated(userId)
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { refreshSession() }
                    .onFailure { _authState.value = AuthState.Unauthenticated }
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun scheduleTokenRefresh() {
        refreshJob?.cancel()
        refreshJob = CoroutineScope(Dispatchers.IO).launch {
            delay(55 * 60 * 1000L)
            runCatching { refreshSession() }
        }
    }

    private suspend fun post(path: String, body: Map<String, Any>): String =
        withContext(Dispatchers.IO) {
            val bodyJson = if (body.isEmpty()) "{}" else JSONObject(body as Map<*, *>).toString()
            val request = Request.Builder()
                .url("$supabaseUrl$path")
                .addHeader("Content-Type", "application/json")
                .addHeader("apikey", anonKey)
                .apply {
                    tokenStorage.accessToken?.let { addHeader("Authorization", "Bearer $it") }
                }
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val text = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                error(parseErrorMessage(text) ?: "HTTP ${response.code}")
            }
            text
        }

    /** Parses GoTrue error — tries multiple known fields. */
    private fun parseErrorMessage(body: String): String? = runCatching {
        val obj = JSONObject(body)
        obj.optString("error_description").takeIf { it.isNotEmpty() }
            ?: obj.optString("message").takeIf { it.isNotEmpty() }
            ?: obj.optString("msg").takeIf { it.isNotEmpty() }
            ?: obj.optString("error").takeIf { it.isNotEmpty() }
    }.getOrNull()

    @Serializable
    private data class TokenResponse(
        val access_token: String,
        val refresh_token: String,
        val user: UserDto
    )

    @Serializable
    private data class UserDto(val id: String, val email: String? = null)
}
