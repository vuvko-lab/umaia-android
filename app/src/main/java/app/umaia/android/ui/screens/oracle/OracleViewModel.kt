package app.umaia.android.ui.screens.oracle

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.domain.model.*
import app.umaia.android.domain.repository.NurRepository
import app.umaia.android.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

sealed class OracleUiState {
    object Loading : OracleUiState()
    data class Questionnaire(val questionIndex: Int, val total: Int) : OracleUiState()
    data class Result(val result: RiskAssessmentResult) : OracleUiState()
    data class Error(val message: String) : OracleUiState()
}

@HiltViewModel
class OracleViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val nurRepository: NurRepository,
    private val gamePreferences: GamePreferences,
    private val analytics: AnalyticsService,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<OracleUiState>(OracleUiState.Loading)
    val uiState: StateFlow<OracleUiState> = _uiState.asStateFlow()

    private val _healthData = MutableStateFlow(HealthData())
    val healthData: StateFlow<HealthData> = _healthData.asStateFlow()

    private val _numberInput = MutableStateFlow("")
    val numberInput: StateFlow<String> = _numberInput.asStateFlow()
    fun setNumberInput(v: String) { _numberInput.value = v }

    private val _selectedMulti = MutableStateFlow<Set<String>>(emptySet())
    val selectedMulti: StateFlow<Set<String>> = _selectedMulti.asStateFlow()

    private val _nurEarned = MutableStateFlow(0)
    val nurEarned: StateFlow<Int> = _nurEarned.asStateFlow()

    private val _nurAlreadyAwarded = MutableStateFlow(prefs.getBoolean(NUR_AWARDED_KEY, false))
    val nurAlreadyAwarded: StateFlow<Boolean> = _nurAlreadyAwarded.asStateFlow()

    private var currentIndex = 0
    private val allQ = oracleQuestions

    val activeQuestions: List<OracleQuestion>
        get() = allQ.filter { q -> q.conditional?.invoke(_healthData.value) ?: true }

    val currentQuestion: OracleQuestion?
        get() = activeQuestions.getOrNull(currentIndex)

    val canGoBack: Boolean get() = currentIndex > 0

    fun load() {
        viewModelScope.launch {
            try {
                val profile = profileRepository.getProfile()
                val savedData = loadHealthData()
                if (profile.questionnaireDone && savedData != null) {
                    _healthData.value = savedData
                    val result = calculateRiskAssessment(savedData)
                    saveTribalRole(result.tribalRole)
                    runCatching { profileRepository.saveOracleResult(result.tribalRole) }
                    gamePreferences.oracleNurAwarded = true
                    _uiState.value = OracleUiState.Result(result)
                } else {
                    analytics.oracleStarted()
                    if (savedData != null) {
                        _healthData.value = savedData
                        val active = activeQuestions
                        currentIndex = active.indexOfFirst { q ->
                            valueForKey(q.dataKey).isEmpty() && !(q.dataKey == "symptoms" && _healthData.value.symptoms.isNotEmpty())
                        }.takeIf { it >= 0 } ?: 0
                    } else {
                        currentIndex = 0
                    }
                    _uiState.value = OracleUiState.Questionnaire(currentIndex, activeQuestions.size)
                }
            } catch (e: Exception) {
                _uiState.value = OracleUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun answerSingle(value: String) {
        val q = currentQuestion ?: return
        analytics.oracleQuestionAnswered(currentIndex, q.dataKey)
        _healthData.value = _healthData.value.set(q.dataKey, value)
        if (!_nurAlreadyAwarded.value) _nurEarned.value += q.nurReward
        saveHealthData()
        advance()
    }

    fun submitNumber() {
        val q = currentQuestion ?: return
        val input = _numberInput.value
        if (input.isBlank()) return
        _healthData.value = _healthData.value.set(q.dataKey, input)
        if (!_nurAlreadyAwarded.value) _nurEarned.value += q.nurReward
        _numberInput.value = ""
        saveHealthData()
        advance()
    }

    fun submitMulti() {
        val q = currentQuestion ?: return
        _healthData.value = _healthData.value.setMulti(q.dataKey, _selectedMulti.value.toList())
        if (!_nurAlreadyAwarded.value) _nurEarned.value += q.nurReward
        _selectedMulti.value = emptySet()
        saveHealthData()
        advance()
    }

    fun toggleMulti(value: String) {
        val current = _selectedMulti.value.toMutableSet()
        if (value == "none") {
            _selectedMulti.value = setOf("none")
        } else {
            current.remove("none")
            if (current.contains(value)) current.remove(value) else current.add(value)
            _selectedMulti.value = current
        }
    }

    fun goBack() {
        if (currentIndex <= 0) return
        currentIndex--
        val active = activeQuestions
        val q = active[currentIndex]
        if (q.type == QuestionType.NUMBER) _numberInput.value = valueForKey(q.dataKey)
        else if (q.type == QuestionType.MULTI) _selectedMulti.value = _healthData.value.symptoms.toSet()
        if (!_nurAlreadyAwarded.value) _nurEarned.value = maxOf(0, _nurEarned.value - q.nurReward)
        _uiState.value = OracleUiState.Questionnaire(currentIndex, active.size)
    }

    fun retake() {
        analytics.oracleRetaken()
        currentIndex = 0
        _healthData.value = HealthData()
        _numberInput.value = ""
        _selectedMulti.value = emptySet()
        _nurEarned.value = 0
        clearHealthData()
        val active = activeQuestions
        _uiState.value = OracleUiState.Questionnaire(0, active.size)
    }

    private fun advance() {
        currentIndex++
        val active = activeQuestions
        if (currentIndex >= active.size) {
            submitAssessment()
        } else {
            _uiState.value = OracleUiState.Questionnaire(currentIndex, active.size)
        }
    }

    private fun submitAssessment() {
        val result = calculateRiskAssessment(_healthData.value)
        saveHealthData()
        saveTribalRole(result.tribalRole)
        viewModelScope.launch {
            _uiState.value = OracleUiState.Loading
            try {
                profileRepository.saveOracleResult(result.tribalRole)
                analytics.oracleCompleted(result.tribalRole, _nurEarned.value)
                if (!_nurAlreadyAwarded.value) {
                    val bonus = 50
                    runCatching { nurRepository.addNur(_nurEarned.value + bonus, "oracle_health_assessment") }
                    _nurAlreadyAwarded.value = true
                    prefs.edit().putBoolean(NUR_AWARDED_KEY, true).apply()
                    // v1.3.3: surface the +Nur to ProfileViewModel + StepsViewModel
                    // observers so the UMAIA-tab Total Nur reflects the bonus
                    // immediately (no app restart needed).
                    gamePreferences.notifyBonusGranted()
                }
                _uiState.value = OracleUiState.Result(result)
            } catch (e: Exception) {
                _uiState.value = OracleUiState.Error(e.message ?: "Submission failed")
            }
        }
    }

    private fun valueForKey(key: String): String = with(_healthData.value) {
        when (key) {
            "goal" -> goal; "gender" -> gender; "age" -> age
            "height" -> height; "weight" -> weight; "activity" -> activity
            "smoking" -> smoking; "alcohol" -> alcohol
            "menstrualCycle" -> menstrualCycle; "chronicDiseases" -> chronicDiseases
            "sleepHours" -> sleepHours; "sleepQuality" -> sleepQuality
            "pressureLevel" -> pressureLevel; "sugarLevel" -> sugarLevel
            "eatsVegetables" -> eatsVegetables; "eatsJunkFood" -> eatsJunkFood
            "familyHistory" -> familyHistory
            else -> ""
        }
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun saveHealthData() {
        prefs.edit().putString(HEALTH_DATA_KEY, json.encodeToString(HealthData.serializer(), _healthData.value)).apply()
    }

    private fun loadHealthData(): HealthData? {
        val raw = prefs.getString(HEALTH_DATA_KEY, null) ?: return null
        return runCatching { json.decodeFromString(HealthData.serializer(), raw) }.getOrNull()
    }

    private fun clearHealthData() {
        prefs.edit().remove(HEALTH_DATA_KEY).apply()
    }

    fun saveTribalRole(role: String) {
        prefs.edit().putString(TRIBAL_ROLE_KEY, role).apply()
    }

    fun loadTribalRole(): String? = prefs.getString(TRIBAL_ROLE_KEY, null)

    companion object {
        private const val HEALTH_DATA_KEY = "umaia_oracle_health_data"
        private const val TRIBAL_ROLE_KEY = "umaia_tribal_role"
        private const val NUR_AWARDED_KEY  = "umaia_oracle_nur_awarded"
    }
}
