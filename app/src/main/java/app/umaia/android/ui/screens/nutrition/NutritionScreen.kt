package app.umaia.android.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.umaia.android.domain.model.*
import app.umaia.android.ui.theme.*

private fun categoryColor(cat: NutritionCategory): Color = when (cat) {
    NutritionCategory.AVOID -> Color(0xFF802020)
    NutritionCategory.SWAP  -> Color(0xFF805020)
    NutritionCategory.ORDER -> Color(0xFF205060)
    NutritionCategory.EAT   -> Color(0xFF204020)
    NutritionCategory.HOW   -> Color(0xFF502060)
    NutritionCategory.WHEN  -> Color(0xFF604010)
}

@Composable
fun NutritionScreen(
    onDismiss: () -> Unit = {},
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TC.bg)
    ) {
        when (state.phase) {
            NutritionPhase.MENU  -> MenuPhase(state = state, viewModel = viewModel)
            NutritionPhase.LEARN -> LearnPhase(state = state, viewModel = viewModel)
            NutritionPhase.QUIZ  -> QuizPhase(state = state, viewModel = viewModel)
        }
        // v1.3.3: dismiss handle at the top-right so callers can sheet-present
        // the screen and let the user back out without finishing a category.
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Text("✕", color = TC.muted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Menu Phase ────────────────────────────────────────────────────────────────

@Composable
private fun MenuPhase(state: NutritionUiState, viewModel: NutritionViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Wisdom of the Steppe",
            color = TC.text, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Ancient knowledge, proven by the healers of the tribe",
            color = TC.muted, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TC.card, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                "\"A nomad who knows what to eat survives the harshest winter. A nomad who knows what to avoid protects the whole tribe.\"",
                color = TC.muted, fontSize = 12.sp, textAlign = TextAlign.Center,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }

        NutritionCategory.entries.forEach { cat ->
            val done = cat.name.lowercase() in state.completedCategories
            val unlocked = viewModel.isCategoryUnlocked(cat)
            CategoryRow(
                category = cat, done = done, unlocked = unlocked,
                onClick = { if (unlocked) viewModel.startLesson(cat) }
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: NutritionCategory,
    done: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit
) {
    val catColor = categoryColor(category)
    val cards = cardsForCategory(category)
    val quizzes = quizzesForCategory(category)

    Surface(
        onClick = onClick,
        enabled = unlocked,
        shape = RoundedCornerShape(14.dp),
        color = when {
            done -> catColor.copy(alpha = 0.06f)
            !unlocked -> SurfaceVariant.copy(alpha = 0.15f)
            else -> SurfaceVariant
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        when {
                            done -> catColor.copy(alpha = 0.2f)
                            !unlocked -> SurfaceVariant.copy(alpha = 0.3f)
                            else -> catColor.copy(alpha = 0.1f)
                        },
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (done) "✅" else if (!unlocked) "🔒" else category.icon,
                    fontSize = 26.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        category.labelEn, color = if (unlocked) TC.text else TC.muted,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (done) {
                        Text("Mastered", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "${cards.size} Study · ${quizzes.size} Test",
                    color = TC.muted, fontSize = 12.sp
                )
            }
        }
    }
}

// ── Learn Phase ───────────────────────────────────────────────────────────────

@Composable
private fun LearnPhase(state: NutritionUiState, viewModel: NutritionViewModel) {
    val cards = cardsForCategory(state.activeCategory)
    val card = cards.getOrNull(state.cardIndex) ?: return
    val isLast = state.cardIndex == cards.size - 1

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.backToMenu() }) {
                Text("‹", color = Gold, fontSize = 20.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("${state.cardIndex + 1} of ${cards.size}", color = TC.muted, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp)) // balance
        }

        LinearProgressIndicator(
            progress = { ((state.cardIndex + 1).toFloat() / cards.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).padding(horizontal = 16.dp),
            color = categoryColor(state.activeCategory), trackColor = SurfaceVariant
        )

        // Card content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(card.icon, fontSize = 40.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(card.title, color = TC.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(card.body, color = TC.muted, fontSize = 14.sp, lineHeight = 20.sp)
            card.swap?.let { swap ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SageGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("✅ ", fontSize = 14.sp)
                    Text(swap, color = SageGreen, fontSize = 13.sp)
                }
            }
            if (card.scienceBased) {
                Text("📚 ${card.source}", color = TC.muted.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.cardIndex > 0) {
                OutlinedButton(
                    onClick = { viewModel.prevCard() },
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
                ) { Text("← Back", color = Gold) }
            } else {
                Spacer(Modifier.weight(1f))
            }
            Button(
                onClick = { if (isLast) viewModel.startQuiz() else viewModel.nextCard() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isLast) "Take Quiz →" else "Next →",
                    color = NightBlue, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Quiz Phase ────────────────────────────────────────────────────────────────

@Composable
private fun QuizPhase(state: NutritionUiState, viewModel: NutritionViewModel) {
    val quizzes = quizzesForCategory(state.activeCategory)
    val quiz = quizzes.getOrNull(state.quizIndex) ?: return
    val isLast = state.quizIndex == quizzes.size - 1
    val selected = state.selectedAnswer

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.backToMenu() }) {
                Text("‹", color = Gold, fontSize = 20.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("Quiz ${state.quizIndex + 1} of ${quizzes.size}", color = TC.muted, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text("⚡ +${state.quizNurEarned}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        LinearProgressIndicator(
            progress = { ((state.quizIndex + 1).toFloat() / quizzes.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).padding(horizontal = 16.dp),
            color = Gold, trackColor = SurfaceVariant
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(quiz.question, color = TC.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

            quiz.options.forEachIndexed { idx, option ->
                val bgColor = when {
                    selected == null -> SurfaceVariant
                    idx == selected && option.correct -> SageGreen.copy(alpha = 0.2f)
                    idx == selected && !option.correct -> TerracottaRed.copy(alpha = 0.2f)
                    option.correct && selected != null -> SageGreen.copy(alpha = 0.1f)
                    else -> SurfaceVariant
                }
                val borderColor = when {
                    selected == null -> Color.Transparent
                    idx == selected && option.correct -> SageGreen
                    idx == selected && !option.correct -> TerracottaRed
                    option.correct && selected != null -> SageGreen.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
                Surface(
                    onClick = { viewModel.selectAnswer(idx) },
                    enabled = selected == null,
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            ('A' + idx).toString(),
                            color = OraclePurple, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .size(28.dp)
                                .background(OraclePurple.copy(alpha = 0.1f), RoundedCornerShape(50))
                                .wrapContentSize()
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(option.text, color = TC.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (selected != null) {
                            Text(if (option.correct) "✓" else if (idx == selected) "✗" else "", fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        if (selected != null) {
            Button(
                onClick = { viewModel.nextQuiz() },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isLast) "Complete ✓" else "Next Quiz →",
                    color = NightBlue, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
