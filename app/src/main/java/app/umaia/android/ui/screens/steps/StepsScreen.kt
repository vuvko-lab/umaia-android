package app.umaia.android.ui.screens.steps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.umaia.android.domain.model.StepMilestone
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StepsScreen(
    viewModel: StepsViewModel = hiltViewModel(),
    leaderboardViewModel: LeaderboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showLeaderboard by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Google Fit sign-in launcher
    val fitLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.onFitPermissionResult() }

    LaunchedEffect(Unit) { viewModel.onAppear() }

    // Start polling when permission is granted, stop when screen leaves composition
    DisposableEffect(state.isPermissionGranted) {
        if (state.isPermissionGranted) viewModel.startTracking()
        onDispose { viewModel.stopTracking() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TC.bg)
    ) {
        if (!state.isPermissionGranted) {
            PermissionRequestView(onRequest = {
                fitLauncher.launch(viewModel.getSignInIntent(context))
            })
        } else {
            StepsContent(state = state)
        }

        // Trophy / Ranking button overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 16.dp)
        ) {
            val s = LocalStrings.current
            OutlinedButton(
                onClick = { showLeaderboard = true },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("🏆", fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text(s.ranking, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Gold)
            }
        }
    }

    if (showLeaderboard) {
        LeaderboardScreen(
            onDismiss = { showLeaderboard = false },
            viewModel = leaderboardViewModel
        )
    }
}

// ── Permission Request ────────────────────────────────────────────────────────

@Composable
private fun PermissionRequestView(onRequest: () -> Unit) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("👣", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            s.stepPermissionTitle,
            color = TC.text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            s.stepPermissionDesc,
            color = TC.muted, fontSize = 14.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(s.allowAccess, color = NightBlue, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Steps Content ─────────────────────────────────────────────────────────────

@Composable
internal fun StepsContent(state: StepsUiState) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            s.stepsTitle,
            color = TC.text, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )

        // Circular step counter
        val progress = run {
            val target = state.nextMilestone?.steps ?: 10_000
            (state.dailySteps.toDouble() / target).coerceIn(0.0, 1.0).toFloat()
        }
        StepCircle(steps = state.dailySteps, nur = state.nurFromSteps, progress = progress)

        // Narrative
        val narrative = when {
            state.dailySteps == 0 -> s.narrativeZero
            state.dailySteps < 1000 -> s.narrativeUnder1k
            state.dailySteps < 3000 -> s.narrativeUnder3k
            state.dailySteps < 6000 -> s.narrativeUnder6k
            state.dailySteps < 10000 -> s.narrativeUnder10k
            state.dailySteps < 15000 -> s.narrativeUnder15k
            else -> s.narrativeMax
        }
        if (narrative.isNotEmpty()) {
            Text(
                "\"$narrative\"",
                color = TC.muted, fontSize = 14.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Daily limit banner
        if (state.dailySteps >= 20_000) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ember.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🌅", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(s.dailyLimitReached, color = Ember, fontSize = 12.sp)
            }
        }

        // Nur earned card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Gold.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(s.nurEarnedToday, color = TC.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("+${state.nurFromSteps}", color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Share steps
        ShareStepsButton(steps = state.dailySteps, nur = state.nurFromSteps)

        // Step Calendar
        StepCalendar(stepHistory = state.stepHistory, todaySteps = state.dailySteps, totalSteps = state.totalSteps)

        // Share calendar
        ShareCalendarButton(stepHistory = state.stepHistory)

        // Nur conversion grid
        NurConversionGrid(currentSteps = state.dailySteps)

        // Next milestone progress
        state.nextMilestone?.let { next ->
            MilestoneProgress(current = state.dailySteps, milestone = next)
        }

        // Reached milestones
        if (state.reachedMilestones.isNotEmpty()) {
            Text(
                s.milestonesReached,
                color = TC.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
            )
            state.reachedMilestones.forEach { m ->
                MilestoneBadge(milestone = m)
            }
        }

        // How Nur Works
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TC.card, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(s.howNurWorks, color = TC.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            BulletText(s.nurBullet1)
            BulletText(s.nurBullet2)
            BulletText(s.nurBullet3)
            BulletText(s.nurBullet4)
        }
    }
}

// ── Step Circle ───────────────────────────────────────────────────────────────

@Composable
private fun StepCircle(steps: Int, nur: Int, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        val circleSize = 220.dp
        val trackColor = TC.cardAlt.copy(alpha = 0.6f)
        Canvas(modifier = Modifier.size(circleSize)) {
            val strokeWidth = 10.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f, sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            // Progress arc
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Gold, Ember, Gold)),
                    startAngle = -90f, sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("👣", fontSize = 42.sp)
            Text(
                "$steps",
                color = Gold, fontSize = 40.sp, fontWeight = FontWeight.Bold, maxLines = 1
            )
            val stepsS = LocalStrings.current
            Text(stepsS.stepsToday, color = TC.muted, fontSize = 12.sp)
            if (nur > 0) {
                Text("⚡ $nur ${stepsS.statNur}", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Milestone Progress ────────────────────────────────────────────────────────

@Composable
private fun MilestoneProgress(current: Int, milestone: StepMilestone) {
    val s = LocalStrings.current
    val isRu = s.statNur == "Нур"
    val progress = (current.toFloat() / milestone.steps).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(
                    s.nextMilestone(if (isRu) milestone.nameRu else milestone.nameEn),
                    color = TC.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isRu) milestone.descRu else milestone.descEn,
                    color = TC.muted, fontSize = 11.sp
                )
            }
            Text("+${milestone.nurBonus} ${s.statNur}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Gold, trackColor = TC.cardAlt
        )
        Spacer(Modifier.height(4.dp))
        Text(s.milestoneStepsProgress(current, milestone.steps), color = TC.muted, fontSize = 10.sp)
    }
}

// ── Milestone Badge ───────────────────────────────────────────────────────────

@Composable
private fun MilestoneBadge(milestone: StepMilestone) {
    val s = LocalStrings.current
    val isRu = s.statNur == "Нур"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Gold.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🏆", fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(if (isRu) milestone.nameRu else milestone.nameEn, color = GoldLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(if (isRu) milestone.descRu else milestone.descEn, color = TC.muted, fontSize = 11.sp)
        }
        Text("+${milestone.nurBonus} ${s.statNur}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Bullet Text ───────────────────────────────────────────────────────────────

@Composable
private fun BulletText(text: String) {
    Row {
        Text("• ", color = TC.muted, fontSize = 12.sp)
        Text(text, color = TC.muted, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

// ── Nur Conversion Grid ───────────────────────────────────────────────────────

@Composable
private fun NurConversionGrid(currentSteps: Int) {
    val milestones = listOf(2_000 to 15, 5_000 to 28, 10_000 to 37, 20_000 to 50)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val s = LocalStrings.current
        Text(s.nurFromSteps, color = TC.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            milestones.forEach { (steps, nur) ->
                val reached = currentSteps >= steps
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (reached) Gold.copy(alpha = 0.12f) else TC.cardAlt,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Text("${steps / 1000}k", color = TC.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("+$nur", color = if (reached) Gold else TC.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Step Calendar ─────────────────────────────────────────────────────────────

@Composable
private fun StepCalendar(stepHistory: Map<String, Int>, todaySteps: Int, totalSteps: Int) {
    var expanded by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val today = remember { fmt.format(Date()) }
    val displayHistory = remember(stepHistory, todaySteps) {
        stepHistory.toMutableMap().also { if (todaySteps > 0) it[today] = todaySteps }
    }
    val historyTotal = displayHistory.values.sum()

    var displayYear  by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(14.dp))
    ) {
        // Header row — tap to expand
        val s = LocalStrings.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    expanded = !expanded
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(s.stepCalendar, color = TC.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(s.calendarTotal(historyTotal), color = TC.muted, fontSize = 12.sp)
            }
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null, tint = TC.muted, modifier = Modifier.size(16.dp)
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                   verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Month navigation
                val monthName = SimpleDateFormat("MMMM yyyy", Locale.US)
                    .format(Calendar.getInstance().also {
                        it.set(Calendar.YEAR, displayYear); it.set(Calendar.MONTH, displayMonth - 1)
                    }.time)
                val nowCal = Calendar.getInstance()
                val canForward = displayYear < nowCal.get(Calendar.YEAR) ||
                    (displayYear == nowCal.get(Calendar.YEAR) && displayMonth < nowCal.get(Calendar.MONTH) + 1)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        if (displayMonth == 1) { displayMonth = 12; displayYear-- }
                        else displayMonth--
                    }) { Text("<", color = TC.text, fontWeight = FontWeight.Bold) }
                    Text(monthName, color = TC.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                         modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    TextButton(
                        onClick = {
                            if (displayMonth == 12) { displayMonth = 1; displayYear++ }
                            else displayMonth++
                        },
                        enabled = canForward
                    ) { Text(">", color = if (canForward) TC.text else TC.muted.copy(alpha = 0.3f), fontWeight = FontWeight.Bold) }
                }

                // Day-of-week headers
                Row {
                    s.dayHeaders.forEach { d ->
                        Text(d, color = TC.muted, fontSize = 10.sp,
                             modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }

                // Calendar grid
                val grid = buildCalendarGrid(displayYear, displayMonth)
                grid.forEach { week ->
                    Row {
                        week.forEach { day ->
                            if (day > 0) {
                                val dateStr = "%04d-%02d-%02d".format(displayYear, displayMonth, day)
                                val steps = displayHistory[dateStr]
                                val isToday = dateStr == today
                                val isFuture = run {
                                    val d = fmt.parse(dateStr)
                                    d != null && d.after(Date())
                                }
                                CalendarDayCell(day = day, steps = steps, isToday = isToday, isFuture = isFuture,
                                               modifier = Modifier.weight(1f))
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Month total
                val prefix = "%04d-%02d".format(displayYear, displayMonth)
                val monthTotal = displayHistory.entries.filter { it.key.startsWith(prefix) }.sumOf { it.value }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TC.cardAlt.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(s.monthTotal, color = TC.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                         modifier = Modifier.weight(1f))
                    Text("%,d".format(monthTotal), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(day: Int, steps: Int?, isToday: Boolean, isFuture: Boolean, modifier: Modifier = Modifier) {
    val hasSteps = (steps ?: 0) > 0
    val bgColor = when {
        isToday -> Gold
        hasSteps -> Gold.copy(alpha = ((steps ?: 0).toFloat() / 10_000f).coerceIn(0.15f, 0.4f))
        else -> TC.cardAlt.copy(alpha = 0.3f)
    }
    val textColor = when {
        isToday -> NightBlue
        isFuture -> TC.text.copy(alpha = 0.2f)
        else -> TC.text.copy(alpha = 0.8f)
    }
    Box(
        modifier = modifier.aspectRatio(1f).padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$day", color = textColor, fontSize = 11.sp,
                     fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                if (hasSteps) {
                    val label = if ((steps ?: 0) >= 10_000) "${(steps ?: 0) / 1000}k"
                                else "%.1fk".format((steps ?: 0) / 1000.0)
                    Text(label, color = if (isToday) NightBlue.copy(alpha = 0.8f) else Gold,
                         fontSize = 7.sp, maxLines = 1)
                }
            }
        }
    }
}

private fun buildCalendarGrid(year: Int, month: Int): List<List<Int>> {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(year, month - 1, 1)
    val firstDayOfWeek = ((cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val totalCells = firstDayOfWeek + daysInMonth
    val rows = (totalCells + 6) / 7
    return (0 until rows).map { row ->
        (0 until 7).map { col ->
            val day = row * 7 + col - firstDayOfWeek + 1
            if (day in 1..daysInMonth) day else 0
        }
    }
}

// ── Share Buttons ─────────────────────────────────────────────────────────────

@Composable
private fun ShareStepsButton(steps: Int, nur: Int) {
    val context = LocalContext.current
    val s = LocalStrings.current
    OutlinedButton(
        onClick = {
            val bitmap = StepShareUtils.createTodayStepsImage(steps, nur)
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val fileName = "umaia_steps_${dateFormat.format(java.util.Date())}.png"
            StepShareUtils.saveBitmapAndShare(context, bitmap, fileName)
            bitmap.recycle()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
    ) {
        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(s.shareSteps, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ShareCalendarButton(stepHistory: Map<String, Int>) {
    val context = LocalContext.current
    val s = LocalStrings.current
    OutlinedButton(
        onClick = {
            val calendar = java.util.Calendar.getInstance()
            val bitmap = StepShareUtils.createCalendarImage(stepHistory, calendar)
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
            val fileName = "umaia_calendar_${dateFormat.format(java.util.Date())}.png"
            StepShareUtils.saveBitmapAndShare(context, bitmap, fileName)
            bitmap.recycle()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
    ) {
        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(s.shareCalendar, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
