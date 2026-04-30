package app.umaia.android.ui.screens.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.domain.model.*
import app.umaia.android.domain.repository.NurRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NutritionPhase { MENU, LEARN, QUIZ }

data class NutritionUiState(
    val phase: NutritionPhase = NutritionPhase.MENU,
    val activeCategory: NutritionCategory = NutritionCategory.AVOID,
    val cardIndex: Int = 0,
    val quizIndex: Int = 0,
    val selectedAnswer: Int? = null,
    val quizNurEarned: Int = 0,
    val completedCategories: List<String> = emptyList(),
)

/**
 * Restored in v1.3.3 (was deleted in v1.3 along with the rest of the
 * game-engine surface). Surfaces the nutrition wisdom-test reward path —
 * sheet-presented from Profile + Steps `EarnMoreHints`, mirroring iOS.
 *
 * Anti-farm: each quiz pays Nur exactly once per (userId, quizId) — repeat
 * attempts grant 0 via [GamePreferences.addQuizNurOnce]. Server-side mirror
 * is conditional on local grant > 0 so we don't pump duplicate
 * `user_coin_transactions` rows.
 */
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val gamePreferences: GamePreferences,
    private val nurRepository: NurRepository,
    private val analytics: AnalyticsService,
) : ViewModel() {

    private val _state = MutableStateFlow(
        NutritionUiState(completedCategories = gamePreferences.completedNutritionCategories())
    )
    val uiState: StateFlow<NutritionUiState> = _state.asStateFlow()

    fun isCategoryUnlocked(cat: NutritionCategory): Boolean {
        val cats = NutritionCategory.entries
        val idx = cats.indexOf(cat)
        if (idx <= 0) return true
        return cats[idx - 1].name.lowercase() in _state.value.completedCategories
    }

    fun startLesson(cat: NutritionCategory) {
        analytics.nutritionLessonStarted(cat.name.lowercase())
        _state.update { it.copy(
            phase = NutritionPhase.LEARN,
            activeCategory = cat,
            cardIndex = 0,
            quizIndex = 0,
            selectedAnswer = null,
            quizNurEarned = 0
        )}
    }

    fun nextCard() { _state.update { it.copy(cardIndex = it.cardIndex + 1) } }
    fun prevCard() { _state.update { it.copy(cardIndex = maxOf(0, it.cardIndex - 1)) } }

    fun startQuiz() {
        _state.update { it.copy(phase = NutritionPhase.QUIZ, quizIndex = 0, selectedAnswer = null, quizNurEarned = 0) }
    }

    fun selectAnswer(optionIndex: Int) {
        val st = _state.value
        if (st.selectedAnswer != null) return
        val quizzes = quizzesForCategory(st.activeCategory)
        val quiz = quizzes.getOrNull(st.quizIndex) ?: return
        _state.update { it.copy(selectedAnswer = optionIndex) }
        val correct = quiz.options[optionIndex].correct
        analytics.nutritionQuizAnswered(quiz.id, correct)
        if (correct) {
            // Anti-farm: prefs.addQuizNurOnce returns 0 on subsequent attempts.
            val granted = gamePreferences.addQuizNurOnce(quiz.id, quiz.nurReward)
            if (granted > 0) {
                _state.update { it.copy(quizNurEarned = it.quizNurEarned + granted) }
                viewModelScope.launch {
                    runCatching { nurRepository.addNur(granted, "nutrition_quiz_${quiz.id}") }
                }
            }
        }
    }

    fun nextQuiz() {
        val st = _state.value
        val quizzes = quizzesForCategory(st.activeCategory)
        val isLast = st.quizIndex == quizzes.size - 1
        if (!isLast) {
            _state.update { it.copy(quizIndex = it.quizIndex + 1, selectedAnswer = null) }
        } else {
            val catName = st.activeCategory.name.lowercase()
            analytics.nutritionCategoryCompleted(catName)
            gamePreferences.markNutritionCategoryCompleted(catName)
            _state.update { it.copy(completedCategories = gamePreferences.completedNutritionCategories()) }
            backToMenu()
        }
    }

    fun backToMenu() { _state.update { it.copy(phase = NutritionPhase.MENU) } }
}
