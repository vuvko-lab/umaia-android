package app.umaia.android.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.umaia.android.R
import app.umaia.android.domain.model.*
import app.umaia.android.ui.components.BuildingPanel
import app.umaia.android.ui.components.BuildingsPanelWithTabs
import app.umaia.android.ui.components.ResourceBar
import app.umaia.android.ui.components.VillageScene
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigateToOracle: () -> Unit,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    gameViewModel: GameViewModel = hiltViewModel()
) {
    val dashState by dashboardViewModel.uiState.collectAsState()
    val gameState by gameViewModel.gameState.collectAsState()
    val session by gameViewModel.session.collectAsState()
    val populationEvent by gameViewModel.populationEvent.collectAsState()

    var showDecayDialog by remember { mutableStateOf(false) }
    var showPopToast by remember { mutableStateOf(false) }
    var popToastGrew by remember { mutableStateOf(true) }
    var popToastCount by remember { mutableStateOf(0) }

    LaunchedEffect(session.isDecaying) {
        if (session.isDecaying) showDecayDialog = true
    }

    LaunchedEffect(populationEvent) {
        val event = populationEvent ?: return@LaunchedEffect
        when (event) {
            is PopulationEvent.Grew -> { popToastGrew = true; popToastCount = event.count }
            is PopulationEvent.Declined -> { popToastGrew = false; popToastCount = event.count }
        }
        showPopToast = true
        kotlinx.coroutines.delay(3000)
        showPopToast = false
        gameViewModel.clearPopulationEvent()
    }

    DisposableEffect(Unit) {
        dashboardViewModel.loadProfile()
        gameViewModel.start()
        onDispose { gameViewModel.stop() }
    }

    Box(modifier = Modifier.fillMaxSize().background(TC.bg)) {
        if (dashState.profile == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Gold
            )
        } else {
            DashboardContent(
                profile = dashState.profile!!,
                gameState = gameState,
                dailySteps = dashState.dailySteps,
                session = session,
                onBuild = { gameViewModel.buildBuilding(it) },
                onAssignWorker = { instanceId, delta -> gameViewModel.assignWorker(instanceId, delta) },
                onOpenPanel = { gameViewModel.markPanelOpened(it) },
                onNavigateToOracle = onNavigateToOracle
            )
        }

        // First-time welcome dialog
        if (dashState.showWelcome) {
            WelcomeDialog { dashboardViewModel.dismissWelcome() }
        }

        // Return-after-gap welcome dialog
        if (dashState.showReturnWelcome) {
            ReturnWelcomeDialog(
                daysAway = dashState.daysAway,
                nurBonus = dashState.returnNurBonus,
                onDismiss = { dashboardViewModel.dismissReturnWelcome() }
            )
        }

        // Welcome-back overlay
        if (dashState.welcomeBack) {
            WelcomeBackDialog { dashboardViewModel.dismissWelcomeBack() }
        }

        // Population toast
        AnimatedVisibility(
            visible = showPopToast,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PopulationToast(grew = popToastGrew, count = popToastCount)
        }
    }

    if (showDecayDialog) {
        val s = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showDecayDialog = false },
            title = { Text(s.lifeIsMovement, color = TC.text) },
            text = { Text(s.nurDecayMessage(session.minutes), color = TC.muted) },
            confirmButton = {
                TextButton(onClick = { showDecayDialog = false }) { Text(s.ok, color = Gold) }
            },
            containerColor = TC.card
        )
    }

    if (session.showSessionComplete && dashState.profile != null) {
        SessionCompleteDialog(
            questsCompleted = gameState.completedQuests.size,
            buildingsBuilt = gameState.buildings.size,
            populationCount = gameState.population,
            onDismiss = { gameViewModel.dismissSessionComplete() }
        )
    }
}

// ── Population Toast ─────────────────────────────────────────────────────────

@Composable
private fun PopulationToast(grew: Boolean, count: Int) {
    val s = LocalStrings.current
    val borderColor = if (grew) SageGreen else TerracottaRed
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(borderColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (grew) "🎉" else "😔", fontSize = 24.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (grew) s.populationGrew else s.populationDeclined,
                color = if (grew) GoldLight else TC.text.copy(alpha = 0.7f),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
            Text("${s.statTribe}: $count", color = TC.muted, fontSize = 11.sp)
        }
        Text(if (grew) "↑" else "↓", color = borderColor, fontSize = 18.sp)
    }
}

// ── Welcome Dialog ────────────────────────────────────────────────────────────

@Composable
private fun WelcomeDialog(onDismiss: () -> Unit) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Welcome to UMAIA", color = Gold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    "👣 " to "Walk to earn Nur",
                    "🏕️ " to "Build structures for your tribe",
                    "🔮 " to "Consult the Oracle to discover your role",
                    "🌿 " to "Learn the wisdom of the steppe"
                ).forEach { (emoji, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(text, color = TC.muted, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) { Text(s.letsGo, color = NightBlue, fontWeight = FontWeight.Bold) }
        },
        containerColor = TC.card
    )
}

// ── Return Welcome Dialog ──────────────────────────────────────────────────────

@Composable
private fun ReturnWelcomeDialog(daysAway: Int, nurBonus: Int, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val emoji = when {
        daysAway == 1 -> "👋"
        daysAway in 2..3 -> "🏕️"
        daysAway in 4..7 -> "🐎"
        else -> "🌅"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Welcome back!", color = Gold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "You've been away for $daysAway day" + if (daysAway != 1) "s" else "",
                        color = TC.muted, fontSize = 14.sp
                    )
                }
                if (nurBonus > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Gold.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎁", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Returning bonus", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("$nurBonus Nur", color = TC.muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) { Text(s.letsGo, color = NightBlue, fontWeight = FontWeight.Bold) }
        },
        containerColor = TC.card
    )
}

// ── Welcome Back Dialog ───────────────────────────────────────────────────────

@Composable
private fun WelcomeBackDialog(onDismiss: () -> Unit) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.welcomeBackTitle, color = Gold, fontWeight = FontWeight.Bold) },
        text = { Text(s.welcomeBackMessage, color = TC.muted) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) { Text(s.letsGo, color = NightBlue, fontWeight = FontWeight.Bold) }
        },
        containerColor = TC.card
    )
}

// ── Session Complete Dialog ───────────────────────────────────────────────────

@Composable
private fun SessionCompleteDialog(
    questsCompleted: Int,
    buildingsBuilt: Int,
    populationCount: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session Complete", color = Gold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple("🎯", "Quests Completed", "$questsCompleted"),
                    Triple("🏗️", "Buildings Built", "$buildingsBuilt"),
                    Triple("👥", "Population", "$populationCount")
                ).forEach { (emoji, label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TC.cardAlt, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(label, color = TC.text, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        }
                        Text(value, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) { Text("Close", color = NightBlue, fontWeight = FontWeight.Bold) }
        },
        containerColor = TC.card
    )
}

// ── Dashboard Content ─────────────────────────────────────────────────────────

@Composable
internal fun DashboardContent(
    profile: app.umaia.android.domain.model.UserProfile,
    gameState: GameState,
    dailySteps: Int,
    session: SessionInfo,
    onBuild: (BuildingId) -> Unit,
    onAssignWorker: (String, Int) -> Unit,
    onOpenPanel: (String) -> Unit,
    onNavigateToOracle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        val s = LocalStrings.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.logo_umaia),
                contentDescription = "Umaia",
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.welcome, color = TC.muted, fontSize = 11.sp)
                Text(
                    profile.fullName ?: "Umaia",
                    color = TC.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            if (session.isWarning) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Ember.copy(alpha = 0.1f)
                ) {
                    Text(
                        "⚠ ${session.minutes}m",
                        color = Ember, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Stats HUD
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatPill("⚡", "${gameState.nur.toInt()}", s.statNur, Gold, Modifier.weight(1f))
            StatPill("👣", "$dailySteps", s.statSteps, Ember, Modifier.weight(1f))
            StatPill("👥", "${gameState.population}", s.statTribe, Sky, Modifier.weight(1f))
            StatPill("🔥", "${profile.currentStreak}", s.statStreak, GoldLight, Modifier.weight(1f))
        }

        // Village scene
        VillageScene(
            state = gameState,
            onPopulationClick = { onOpenPanel("population_panel") },
            onSpiritClick = { onOpenPanel("spirit_panel") }
        )

        // Spirit & population bar
        SpiritBar(state = gameState, onOpenPanel = onOpenPanel)

        // Resources
        ResourceBar(
            resources = gameState.resources,
            defs = ResourceId.entries.map { resourceDef(it) }
        )

        // Active quest
        currentQuest(completedIds = gameState.completedQuests)?.let { quest ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TC.card, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(quest.nameEn, color = TC.text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(quest.descEn, color = TC.muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }

        // Buildings with tabs
        BuildingsPanelWithTabs(
            state = gameState,
            onBuild = onBuild,
            onAssignWorker = onAssignWorker,
            onNavigateToOracle = onNavigateToOracle
        )

        // Rewards
        RewardsCard(state = gameState)

        // Oracle CTA
        OracleCard(
            tribalRole = null,
            onTap = onNavigateToOracle
        )
    }
}

// ── Stat Pill ─────────────────────────────────────────────────────────────────

@Composable
private fun StatPill(icon: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(TC.cardAlt.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 15.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = TC.muted, fontSize = 9.sp, maxLines = 1)
    }
}

// ── Spirit Bar ────────────────────────────────────────────────────────────────

@Composable
private fun SpiritBar(state: GameState, onOpenPanel: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val spiritTarget = run {
        var t = 50.0
        if (state.resources[ResourceId.FOOD] > 0 && state.resources[ResourceId.WATER] > 0) t += 12
        if (state.resources[ResourceId.FUEL] > 0) t += 4
        if (state.resources[ResourceId.KUMIS] > 0.5) t += 10
        if (state.resources[ResourceId.SONGS] > 0.5) t += 8
        if (state.resources[ResourceId.CRAFTS] > 0.5) t += 7
        if (state.nur > 0) t += 4
        if (state.resources[ResourceId.FOOD] <= 0) t -= 25
        if (state.resources[ResourceId.WATER] <= 0) t -= 20
        if (state.resources[ResourceId.KUMIS] <= 0.3 && state.resources[ResourceId.SONGS] <= 0.3 && state.resources[ResourceId.CRAFTS] <= 0.3) t -= 12
        t.coerceIn(0.0, 100.0).toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(14.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                expanded = !expanded
                if (!expanded) {
                    onOpenPanel("spirit_panel")
                    onOpenPanel("population_panel")
                }
            }
            .padding(12.dp)
    ) {
        val s = LocalStrings.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.spiritLabel(state.spirit.toInt()),
                color = TC.text, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👥", fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    "${state.population}/${state.maxPopulation}",
                    color = TC.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null, tint = TC.muted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        val spiritColor = when {
            state.spirit >= 80 -> Gold
            state.spirit >= 50 -> Ember
            else -> TerracottaRed
        }
        LinearProgressIndicator(
            progress = { (state.spirit / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = spiritColor, trackColor = TC.cardAlt
        )

        if (!expanded) {
            Spacer(Modifier.height(4.dp))
            Text(s.tapToSeeDetails, color = Gold.copy(alpha = 0.6f), fontSize = 9.sp)
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = TC.text.copy(alpha = 0.1f))
            Spacer(Modifier.height(8.dp))

            // Spirit factors
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(s.spiritFactors, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    s.spiritTarget(spiritTarget),
                    color = if (spiritTarget >= 80) SageGreen else Ember,
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))

            SpiritFactorRow(s.factorFoodWater, state.resources[ResourceId.FOOD] > 0 && state.resources[ResourceId.WATER] > 0, "+12")
            SpiritFactorRow(s.factorFuel, state.resources[ResourceId.FUEL] > 0, "+4")
            SpiritFactorRow(s.factorKumis, state.resources[ResourceId.KUMIS] > 0.5, "+10",
                hint = if (state.resources[ResourceId.KUMIS] <= 0.5) s.hintBuildHearthOrBrewery else null)
            SpiritFactorRow(s.factorSongs, state.resources[ResourceId.SONGS] > 0.5, "+8",
                hint = if (state.resources[ResourceId.SONGS] <= 0.5) s.hintBuildCircle else null)
            SpiritFactorRow(s.factorCrafts, state.resources[ResourceId.CRAFTS] > 0.5, "+7",
                hint = if (state.resources[ResourceId.CRAFTS] <= 0.5) s.hintBuildWorkshop else null)
            SpiritFactorRow(s.factorNurActive, state.nur > 0, "+4",
                hint = if (state.nur <= 0) s.hintWalkForNur else null)
            if (state.resources[ResourceId.FOOD] <= 0) SpiritFactorRow(s.warnNoFood, false, "−25")
            if (state.resources[ResourceId.WATER] <= 0) SpiritFactorRow(s.warnNoWater, false, "−20")
            if (state.resources[ResourceId.KUMIS] <= 0.3 && state.resources[ResourceId.SONGS] <= 0.3 && state.resources[ResourceId.CRAFTS] <= 0.3)
                SpiritFactorRow(s.warnNoGoods, false, "−12")

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = TC.text.copy(alpha = 0.1f))
            Spacer(Modifier.height(8.dp))

            // Population info
            Text(s.populationSection, color = Sky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(s.populationCurrent, color = TC.muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("${state.population} / ${state.maxPopulation}", color = TC.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(s.populationGrowth, color = TC.muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                when {
                    state.population >= state.maxPopulation ->
                        Text(s.atCapacity, color = Ember, fontSize = 11.sp)
                    state.spirit >= 80 ->
                        Text(s.activeCanJoin, color = SageGreen, fontSize = 11.sp)
                    else ->
                        Text(s.needSpiritForGrowth(state.spirit.toInt()), color = TC.muted, fontSize = 11.sp)
                }
            }
            if (state.population >= state.maxPopulation) {
                Spacer(Modifier.height(4.dp))
                Text(s.buildYurtForCapacity, color = Ember, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SpiritFactorRow(label: String, met: Boolean, bonus: String, hint: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(if (met) "✓" else "✗", color = if (met) SageGreen else TerracottaRed.copy(alpha = 0.6f), fontSize = 11.sp)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = if (met) TC.text else TC.muted, fontSize = 11.sp)
            if (!hint.isNullOrBlank() && !met) {
                Text("→ $hint", color = Gold.copy(alpha = 0.7f), fontSize = 9.sp)
            }
        }
        Text(bonus, color = if (met) SageGreen else TerracottaRed.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}


// ── Rewards Card ─────────────────────────────────────────────────────────────

private data class RewardDef(
    val id: String,
    val icon: String,
    val name: String,
    val partner: String,
    val desc: String,
    val targetSteps: Int,
    val period: String  // "weekly" or "monthly"
)

private val allRewards = listOf(
    RewardDef("shopper_weekly",  "🛍️", "Shopper",    "Umaia Store", "Shopper voucher",     50_000,  "weekly"),
    RewardDef("tshirt_monthly",  "👕", "T-Shirt",    "Umaia Store", "T-shirt reward",     200_000, "monthly"),
    RewardDef("explorer_weekly", "🥾", "Explorer",   "Umaia Store", "Explorer voucher",   100_000,  "weekly"),
    RewardDef("champion_monthly","🏆", "Champion",   "Umaia Store", "Champion reward",    400_000, "monthly")
)

@Composable
private fun RewardsCard(state: GameState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { app.umaia.android.data.local.GamePreferences(context) }
    var refreshKey by remember { mutableStateOf(0) }

    val hasBasicBuildings = remember(state.buildings) {
        state.buildings.any { it.type == BuildingId.PASTURE } &&
        state.buildings.any { it.type == BuildingId.WELL } &&
        state.buildings.any { it.type == BuildingId.HEARTH }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val s = LocalStrings.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎁", fontSize = 18.sp)
            Spacer(Modifier.width(6.dp))
            Text(s.rewards, color = TC.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (!hasBasicBuildings) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔒", fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Text(s.buildToUnlockRewards, color = TC.muted, fontSize = 12.sp)
            }
        } else {
            allRewards.forEach { reward ->
                key(refreshKey) {
                    RewardRow(
                        reward = reward,
                        prefs = prefs,
                        onClaimed = { refreshKey++ }
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardRow(
    reward: RewardDef,
    prefs: app.umaia.android.data.local.GamePreferences,
    onClaimed: () -> Unit
) {
    val currentSteps = remember {
        val daily = prefs.dailySteps
        if (reward.period == "weekly") prefs.weeklySteps(daily) else prefs.effectiveMonthlySteps(daily)
    }
    val alreadyClaimed = remember { prefs.isRewardClaimed(reward.id) }
    val unlocked = currentSteps >= reward.targetSteps && !alreadyClaimed
    val progress = (currentSteps.toFloat() / reward.targetSteps).coerceIn(0f, 1f)
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        val s = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(s.rewardClaimed, color = Gold, fontWeight = FontWeight.Bold) },
            text = { Text(s.rewardInstruction(reward.partner, reward.desc), color = TC.muted) },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.claimReward(reward.id, reward.period)
                        showDialog = false
                        onClaimed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                ) { Text(s.ok, color = Color.White) }
            },
            containerColor = TC.card
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(reward.icon, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(reward.name, color = TC.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text("· ${reward.partner}", color = Gold.copy(alpha = 0.6f), fontSize = 9.sp)
                }
                Text(
                    "${"%,d".format(reward.targetSteps)} steps · ${reward.period}",
                    color = TC.muted, fontSize = 10.sp
                )
            }
            when {
                alreadyClaimed -> Text("✅", fontSize = 14.sp)
                unlocked -> Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(50)
                ) { Text(LocalStrings.current.claim, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                else -> Text(
                    "%,d/%,d".format(currentSteps, reward.targetSteps),
                    color = TC.muted, fontSize = 10.sp
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = if (alreadyClaimed || unlocked) SageGreen else Gold,
            trackColor = TC.cardAlt
        )
    }
}

// ── Oracle CTA ────────────────────────────────────────────────────────────────

@Composable
private fun OracleCard(tribalRole: String?, onTap: () -> Unit) {
    val s = LocalStrings.current
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(14.dp),
        color = OraclePurple.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, OraclePurple.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔮", fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.speakToTheSeer, color = OracleLight, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    if (tribalRole != null && tribalRole != "newcomer") s.yourRole(tribalRole)
                    else s.discoverRoleEarnNur,
                    color = TC.muted, fontSize = 12.sp, maxLines = 1
                )
            }
            Text("›", color = OraclePurple.copy(alpha = 0.5f), fontSize = 18.sp)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun currentQuest(completedIds: List<Int>): QuestDef? =
    allQuests.firstOrNull { it.id !in completedIds }
