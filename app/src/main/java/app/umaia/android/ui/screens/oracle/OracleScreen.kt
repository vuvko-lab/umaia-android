package app.umaia.android.ui.screens.oracle

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import app.umaia.android.domain.model.OracleCitations
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.umaia.android.domain.model.*
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.strings.localizedLabel
import app.umaia.android.ui.strings.localizedOracleText
import app.umaia.android.ui.strings.localizedText
import app.umaia.android.ui.theme.*

@Composable
fun OracleScreen(
    onComplete: () -> Unit,
    viewModel: OracleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val nurEarned by viewModel.nurEarned.collectAsState()
    val nurAlreadyAwarded by viewModel.nurAlreadyAwarded.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TC.bg)
    ) { when (val state = uiState) {
        is OracleUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OraclePurple)
            }
        }
        is OracleUiState.Questionnaire -> {
            val question = viewModel.currentQuestion
            if (question != null) {
                QuestionPane(
                    question = question,
                    questionIndex = state.questionIndex,
                    total = state.total,
                    viewModel = viewModel,
                    nurEarned = nurEarned
                )
            }
        }
        is OracleUiState.Result -> {
            ResultsView(
                result = state.result,
                nurEarned = nurEarned + 50,
                nurAlreadyAwarded = nurAlreadyAwarded,
                onRetake = { viewModel.retake() },
                onContinue = onComplete
            )
        }
        is OracleUiState.Error -> {
            ErrorPane(message = state.message, onRetry = onComplete)
        }
    } }
}

// ── Question Pane ─────────────────────────────────────────────────────────────

@Composable
private fun QuestionPane(
    question: OracleQuestion,
    questionIndex: Int,
    total: Int,
    viewModel: OracleViewModel,
    nurEarned: Int
) {
    val selectedMulti by viewModel.selectedMulti.collectAsState()
    val numberInput by viewModel.numberInput.collectAsState()
    val canGoBack = viewModel.canGoBack

    Column(modifier = Modifier.fillMaxSize()) {
        // Progress bar
        LinearProgressIndicator(
            progress = { ((questionIndex + 1).toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).padding(horizontal = 20.dp),
            color = OraclePurple, trackColor = TC.cardAlt
        )

        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canGoBack) {
                TextButton(onClick = { viewModel.goBack() }) {
                    Text("‹", color = OracleLight, fontSize = 18.sp)
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Text(
                "${questionIndex + 1} / $total",
                color = TC.muted, fontSize = 12.sp,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            Text("⚡ +$nurEarned", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val s = LocalStrings.current
            Spacer(Modifier.height(8.dp))
            Text("🔮", fontSize = 52.sp)
            Text(
                question.localizedOracleText(s.code),
                color = OracleLight.copy(alpha = 0.7f),
                fontSize = 13.sp, fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center, lineHeight = 18.sp
            )
            Text(
                question.localizedText(s.code),
                color = TC.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, lineHeight = 24.sp
            )

            when (question.type) {
                QuestionType.SINGLE -> SingleSelectView(
                    options = question.options,
                    onSelect = { viewModel.answerSingle(it) }
                )
                QuestionType.NUMBER -> NumberInputView(
                    input = numberInput,
                    dataKey = question.dataKey,
                    onInputChange = { viewModel.setNumberInput(it) },
                    onSubmit = { viewModel.submitNumber() }
                )
                QuestionType.MULTI -> MultiSelectView(
                    options = question.options,
                    selected = selectedMulti,
                    onToggle = { viewModel.toggleMulti(it) },
                    onSubmit = { viewModel.submitMulti() }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SingleSelectView(options: List<OracleOption>, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OraclePurple.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
    ) {
        options.forEachIndexed { idx, opt ->
            Surface(
                onClick = { onSelect(opt.id) },
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(OraclePurple.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            ('A' + idx).toString(),
                            color = OraclePurple, fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(opt.localizedLabel(s.code), color = TC.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text("›", color = TC.muted.copy(alpha = 0.3f), fontSize = 14.sp)
                }
            }
            if (idx < options.size - 1) {
                HorizontalDivider(
                    color = TC.text.copy(alpha = 0.05f),
                    modifier = Modifier.padding(start = 56.dp)
                )
            }
        }
    }
}

@Composable
private fun NumberInputView(
    input: String,
    dataKey: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val defaultValue = when (dataKey) { "height" -> 160; "weight" -> 60; else -> 0 }
    var pickerValue by remember { mutableIntStateOf(if (input.isNotEmpty()) input.toIntOrNull() ?: defaultValue else defaultValue) }

    LaunchedEffect(dataKey) {
        val v = input.toIntOrNull() ?: defaultValue
        pickerValue = v
        onInputChange("$v")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Simple number picker via Slider
        Text(
            "$pickerValue",
            color = Gold, fontSize = 48.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        val range = when (dataKey) { "height" -> 100f..250f; "weight" -> 20f..300f; "age" -> 10f..100f; else -> 1f..250f }
        Slider(
            value = pickerValue.toFloat(),
            onValueChange = { v ->
                pickerValue = v.toInt()
                onInputChange("${v.toInt()}")
            },
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = OraclePurple, activeTrackColor = OraclePurple)
        )
        Button(
            onClick = onSubmit,
            enabled = pickerValue > 0,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(LocalStrings.current.oracleContinue, color = NightBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MultiSelectView(
    options: List<OracleOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(OraclePurple.copy(alpha = 0.03f), RoundedCornerShape(14.dp))
        ) {
            options.forEachIndexed { idx, opt ->
                val isOn = selected.contains(opt.id)
                Surface(
                    onClick = { onToggle(opt.id) },
                    color = if (isOn) OraclePurple.copy(alpha = 0.06f) else Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isOn) "●" else "○",
                            color = if (isOn) OraclePurple else TC.muted.copy(alpha = 0.3f),
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(opt.localizedLabel(s.code), color = TC.text, fontSize = 15.sp)
                    }
                }
                if (idx < options.size - 1) {
                    HorizontalDivider(
                        color = TC.text.copy(alpha = 0.05f),
                        modifier = Modifier.padding(start = 48.dp)
                    )
                }
            }
        }

        Button(
            onClick = onSubmit,
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(LocalStrings.current.oracleContinue, color = NightBlue, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Results View ──────────────────────────────────────────────────────────────

@Composable
private fun ResultsView(
    result: RiskAssessmentResult,
    nurEarned: Int,
    nurAlreadyAwarded: Boolean,
    onRetake: () -> Unit,
    onContinue: () -> Unit
) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(OraclePurple.copy(alpha = 0.12f), OraclePurple.copy(alpha = 0.03f), Color.Transparent)
                    )
                )
                .padding(top = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val s = LocalStrings.current
            Text(tribalRoleEmojiLocal(result.tribalRole), fontSize = 56.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                s.oracleSeerRevealed,
                color = OracleLight.copy(alpha = 0.7f),
                fontSize = 11.sp, fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(tribalRoleTitleLocal(result.tribalRole, s.code), color = OracleLight, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                tribalRoleDescriptionLocal(result.tribalRole, s.code),
                color = TC.muted, fontSize = 13.sp,
                textAlign = TextAlign.Center, lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Key metrics
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                result.bmi?.let { bmi ->
                    MetricCard(
                        label = "BMI", value = "%.1f".format(bmi),
                        badge = bmiCategoryLabel(result.bmiCategory),
                        badgeColor = bmiCategoryColor(result.bmiCategory),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (result.totalImpact != 0.0) {
                    MetricCard(
                        label = "Life Impact",
                        value = "${if (result.totalImpact > 0) "+" else ""}${"%.1f".format(result.totalImpact)}",
                        badge = "${s.oracleYears} (net)",
                        badgeColor = if (result.totalImpact > 0) SageGreen else TerracottaRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // v1.3 — explicit positive/negative pills under the Life Impact metric.
            // Lets the user see the math behind the net number (transparency).
            if (result.riskFactors.isNotEmpty() || result.positiveFactors.isNotEmpty()) {
                LifeImpactBreakdown(
                    positiveSum = result.positiveFactors.sumOf { it.impact },
                    negativeSum = result.riskFactors.sumOf { it.impact }
                )
            }

            // Nur earned
            if (nurAlreadyAwarded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TC.cardAlt, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(s.oracleNurAlreadyEarned, color = TC.muted, fontSize = 13.sp)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Gold.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(s.oracleNurEarned(nurEarned), color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Risk factors
            if (result.riskFactors.isNotEmpty()) {
                ReportSection(title = s.oracleRiskFactors, iconTint = TerracottaRed) {
                    result.riskFactors.forEachIndexed { idx, factor ->
                        FactorRow(factor = factor, isPositive = false)
                        if (idx < result.riskFactors.size - 1) HorizontalDivider(color = TC.text.copy(alpha = 0.05f))
                    }
                }
            }

            // Positive factors
            if (result.positiveFactors.isNotEmpty()) {
                ReportSection(title = s.oraclePositiveFactors, iconTint = SageGreen) {
                    result.positiveFactors.forEachIndexed { idx, factor ->
                        FactorRow(factor = factor, isPositive = true)
                        if (idx < result.positiveFactors.size - 1) HorizontalDivider(color = TC.text.copy(alpha = 0.05f))
                    }
                }
            }

            // Deficiencies
            if (result.deficiencies.isNotEmpty()) {
                ReportSection(title = s.oraclePotentialDeficiencies, iconTint = OraclePurple) {
                    result.deficiencies.forEachIndexed { idx, def ->
                        DeficiencyRow(def)
                        if (idx < result.deficiencies.size - 1) HorizontalDivider(color = TC.text.copy(alpha = 0.05f))
                    }
                }
            }

            // CTAs
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) { Text(s.oracleRetake, color = Gold) }

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(12.dp)
            ) { Text(s.oracleReturnToMain, color = NightBlue, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, badge: String, badgeColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TC.cardAlt, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label.uppercase(), color = TC.muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, color = TC.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            badge,
            color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(badgeColor.copy(alpha = 0.12f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ReportSection(title: String, iconTint: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.cardAlt, RoundedCornerShape(14.dp))
    ) {
        Text(
            title, color = iconTint, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        )
        HorizontalDivider(color = TC.text.copy(alpha = 0.05f))
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            content()
        }
    }
}

@Composable
private fun FactorRow(factor: RiskFactor, isPositive: Boolean) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(factor.name, color = TC.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            val impactStr = if (factor.impact > 0) "+${"%.1f".format(factor.impact)}y" else "${"%.1f".format(factor.impact)}y"
            Text(
                impactStr,
                color = if (isPositive) SageGreen else TerracottaRed,
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        (if (isPositive) SageGreen else TerracottaRed).copy(alpha = 0.1f),
                        CircleShape
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Text(factor.recommendation, color = TC.muted, fontSize = 11.sp, lineHeight = 15.sp)
        Text(factor.source, color = TC.muted.copy(alpha = 0.4f), fontSize = 10.sp)
        InlineCitations(ids = factor.citationIds)
    }
}

@Composable
private fun DeficiencyRow(def: DeficiencyRisk) {
    val s = LocalStrings.current
    Column(modifier = Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(def.nutrient, color = OracleLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(def.description, color = TC.muted, fontSize = 11.sp, lineHeight = 15.sp)
        Row(verticalAlignment = Alignment.Top) {
            Text("🍴 ", fontSize = 10.sp)
            Column {
                Text(s.oracleFoodsRichIn(def.nutrient), color = TC.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(def.advice, color = Gold, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        InlineCitations(ids = def.citationIds)
    }
}

/**
 * Renders citation ids as a row of clickable links. Replaces the old
 * centralized References block: each row carries its own evidence trail so a
 * curious user can tap straight through to the underlying study.
 */
@Composable
private fun InlineCitations(ids: List<String>) {
    if (ids.isEmpty()) return
    val context = LocalContext.current
    Row(modifier = Modifier.padding(top = 2.dp)) {
        ids.mapNotNull { OracleCitations.byId(it) }.forEachIndexed { idx, citation ->
            if (idx > 0) Text(" · ", color = TC.muted.copy(alpha = 0.4f), fontSize = 10.sp)
            Text(
                "[${citation.id.removePrefix("ref_")}]",
                color = Gold,
                fontSize = 10.sp,
                style = androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(citation.url)))
                    }
                }
            )
        }
    }
}

@Composable
private fun LifeImpactBreakdown(positiveSum: Double, negativeSum: Double) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (positiveSum > 0) {
            Pill(
                text = s.oracleHealthyHabits(positiveSum),
                bg = SageGreen.copy(alpha = 0.15f),
                fg = SageGreen,
                modifier = Modifier.weight(1f)
            )
        }
        if (negativeSum < 0) {
            Pill(
                text = s.oracleRiskFactorsPill(negativeSum),
                bg = TerracottaRed.copy(alpha = 0.15f),
                fg = TerracottaRed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Error Pane ────────────────────────────────────────────────────────────────

@Composable
private fun ErrorPane(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))
        Text(LocalStrings.current.oracleSomethingWrong, color = TC.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = TC.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(12.dp)
        ) { Text(LocalStrings.current.oracleGoBack, color = NightBlue, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.weight(1f))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun tribalRoleEmojiLocal(role: String): String = tribalRoles[role]?.icon ?: "🔮"
private fun tribalRoleTitleLocal(role: String, code: String): String = when (code) {
    "en" -> tribalRoles[role]?.nameEn
    else -> tribalRoles[role]?.nameRu // "ru", "kk", and unknown fall back to Russian
} ?: role.replaceFirstChar { it.uppercase() }
private fun tribalRoleDescriptionLocal(role: String, code: String): String = when (code) {
    "en" -> tribalRoles[role]?.descEn
    else -> tribalRoles[role]?.descRu
} ?: ""
private fun bmiCategoryLabel(cat: String): String = when (cat) {
    "underweight" -> "Underweight"; "normal" -> "Normal"; "overweight" -> "Overweight"
    "obese" -> "Obese"; "severely_obese" -> "Severe"
    else -> "—"
}
private fun bmiCategoryColor(cat: String): Color = when (cat) {
    "normal" -> SageGreen; "underweight", "overweight" -> Ember
    else -> TerracottaRed
}
