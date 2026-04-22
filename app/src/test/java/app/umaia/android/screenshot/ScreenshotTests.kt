package app.umaia.android.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.umaia.android.domain.model.*
import app.umaia.android.domain.repository.LeaderboardData
import app.umaia.android.domain.repository.LeaderboardEntry
import app.umaia.android.domain.repository.LeaderboardPeriod
import app.umaia.android.ui.screens.dashboard.DashboardContent
import app.umaia.android.ui.screens.dashboard.SessionInfo
import app.umaia.android.ui.screens.profile.HeroSection
import app.umaia.android.ui.screens.profile.MythologySection
import app.umaia.android.ui.screens.profile.ProfileCard
import app.umaia.android.ui.screens.profile.RolePanelView
import app.umaia.android.ui.screens.steps.LeaderboardSheetContent
import app.umaia.android.ui.screens.steps.LeaderboardUiState
import app.umaia.android.ui.screens.steps.StepsContent
import app.umaia.android.ui.screens.steps.StepsUiState
import app.umaia.android.ui.theme.*
import org.junit.Rule
import org.junit.Test

class ScreenshotTests {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        showSystemUi = false
    )

    // ── 1. Login Screen ───────────────────────────────────────────────────────

    @Test
    fun screen_login() {
        paparazzi.snapshot {
            UmaiaTheme {
                LoginScreenshot()
            }
        }
    }

    // ── 2. Dashboard ──────────────────────────────────────────────────────────

    @Test
    fun screen_dashboard() {
        paparazzi.snapshot {
            UmaiaTheme {
                Box(Modifier.fillMaxSize().background(NightBlue)) {
                    DashboardContent(
                        profile = fakeProfile(),
                        gameState = fakeGameState(),
                        dailySteps = 7842,
                        session = SessionInfo(minutes = 4),
                        onBuild = {},
                        onAssignWorker = { _, _ -> },
                        onOpenPanel = {},
                        onNavigateToOracle = {}
                    )
                }
            }
        }
    }

    // ── 3. Steps Screen ───────────────────────────────────────────────────────

    @Test
    fun screen_steps() {
        paparazzi.snapshot {
            UmaiaTheme {
                Box(Modifier.fillMaxSize().background(NightBlue)) {
                    StepsContent(state = fakeStepsState())
                    // Ranking button overlay — matches StepsScreen production layout
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {},
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("🏆", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("Ranking", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Gold)
                        }
                    }
                }
            }
        }
    }

    // ── 4. Leaderboard ────────────────────────────────────────────────────────

    @Test
    fun screen_leaderboard() {
        paparazzi.snapshot {
            UmaiaTheme {
                Box(Modifier.fillMaxSize().background(NightBlue)) {
                    LeaderboardSheetContent(
                        state = LeaderboardUiState(
                            period = LeaderboardPeriod.WEEKLY,
                            data = fakeLeaderboardData(),
                            isLoading = false
                        )
                    )
                }
            }
        }
    }

    // ── 5. Profile Screen ─────────────────────────────────────────────────────

    @Test
    fun screen_profile() {
        paparazzi.snapshot {
            UmaiaTheme {
                ProfileScreenshot()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fake data
// ─────────────────────────────────────────────────────────────────────────────

private fun fakeProfile() = UserProfile(
    userId = "preview",
    fullName = "Aizat Bekova",
    gender = "female",
    age = 28,
    city = "Almaty",
    onboardingComplete = true,
    appOpenCount = 47,
    nurBalance = 312,
    currentStreak = 7,
    longestStreak = 14,
    tribalRole = "warrior",
    oracleSession = 1,
    questionnaireDone = true
)

private fun fakeGameState() = GameState(
    resources = ResourceMap(food = 4.8, water = 3.2, fuel = 2.1, felt = 1.5, wood = 2.8),
    buildings = listOf(
        BuiltBuilding(instanceId = "p1", type = BuildingId.PASTURE,  workers = 2),
        BuiltBuilding(instanceId = "w1", type = BuildingId.WELL,     workers = 1),
        BuiltBuilding(instanceId = "h1", type = BuildingId.HEARTH,   workers = 1),
        BuiltBuilding(instanceId = "c1", type = BuildingId.WOODCAMP, workers = 1)
    ),
    population = 8,
    maxPopulation = 12,
    spirit = 72.0,
    nur = 25.0,
    day = 14,
    stepsSynced = true
)

private fun fakeStepsState() = StepsUiState(
    dailySteps = 7842,
    totalSteps = 284_316,
    nurFromSteps = 34,
    reachedMilestones = listOf(
        allMilestones.first { it.steps == 2_000 },
        allMilestones.first { it.steps == 5_000 }
    ),
    nextMilestone = allMilestones.first { it.steps == 10_000 },
    isSensorAvailable = true,
    isPermissionGranted = true,
    stepHistory = buildMap {
        val base = java.util.Calendar.getInstance()
        repeat(28) { i ->
            val d = base.clone() as java.util.Calendar
            d.add(java.util.Calendar.DAY_OF_MONTH, -i)
            val key = "%04d-%02d-%02d".format(
                d.get(java.util.Calendar.YEAR),
                d.get(java.util.Calendar.MONTH) + 1,
                d.get(java.util.Calendar.DAY_OF_MONTH)
            )
            put(key, (3_000..14_000).random())
        }
    }
)

private fun fakeLeaderboardData() = LeaderboardData(
    entries = listOf(
        LeaderboardEntry("u1", "Aizat Bekova",    32_140, 48, 1,  isMe = false),
        LeaderboardEntry("u2", "Timur Seitkali",  28_950, 42, 2,  isMe = false),
        LeaderboardEntry("u3", "Nuray Ospanova",  24_310, 37, 3,  isMe = false),
        LeaderboardEntry("u4", "Alibek Dzhaksybekov", 19_820, 31, 4, isMe = false),
        LeaderboardEntry("me", "You",             18_440, 29, 5,  isMe = true),
        LeaderboardEntry("u5", "Saya Bekova",     15_600, 24, 6,  isMe = false),
        LeaderboardEntry("u6", "Daulet Seitkali", 12_300, 20, 7,  isMe = false),
        LeaderboardEntry("u7", "Meruyert K.",     10_100, 17, 8,  isMe = false),
    ),
    myRank = 5,
    mySteps = 18_440
)

// ─────────────────────────────────────────────────────────────────────────────
// Screenshot composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginScreenshot() {
    Box(
        modifier = Modifier.fillMaxSize().background(NightBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Logo placeholder (no painterResource in test)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Gold),
                contentAlignment = Alignment.Center
            ) {
                Text("U", color = NightBlue, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            Text("UMAIA", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoldLight,
                modifier = Modifier.padding(top = 8.dp))
            Text(
                "Turn your steps to build a new World of Nomads",
                fontSize = 14.sp, color = Parchment.copy(alpha = 0.45f),
                textAlign = TextAlign.Center, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp, start = 12.dp, end = 12.dp)
            )

            // Form card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NightMid)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✉", fontSize = 15.sp, color = Parchment.copy(alpha = 0.35f), modifier = Modifier.width(24.dp))
                    TextField(
                        value = "aizat@umaia.tech",
                        onValueChange = {},
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            unfocusedTextColor = Parchment
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HorizontalDivider(color = Parchment.copy(alpha = 0.08f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔒", fontSize = 15.sp, color = Parchment.copy(alpha = 0.35f), modifier = Modifier.width(24.dp))
                    TextField(
                        value = "••••••••",
                        onValueChange = {},
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            unfocusedTextColor = Parchment
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            UmaiaButton(
                text = "Sign In",
                onClick = {},
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(Modifier.weight(1f), color = Parchment.copy(alpha = 0.1f), thickness = 0.5.dp)
                Text("  or  ", color = Parchment.copy(alpha = 0.3f), fontSize = 11.sp)
                HorizontalDivider(Modifier.weight(1f), color = Parchment.copy(alpha = 0.1f), thickness = 0.5.dp)
            }

            Button(
                onClick = {},
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NightMid),
                border = androidx.compose.foundation.BorderStroke(1.dp, Parchment.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("G  ", fontWeight = FontWeight.Bold, color = GoldLight, fontSize = 16.sp)
                Text("Continue with Google", color = Parchment, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            TextButton(onClick = {}, modifier = Modifier.padding(top = 18.dp)) {
                Text("Don't have an account? Sign up", color = Parchment.copy(alpha = 0.4f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ProfileScreenshot() {
    Box(
        modifier = Modifier.fillMaxSize().background(NightBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            HeroSection()
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileCard(
                    name = "Aizat Bekova",
                    email = "aizat@umaia.tech",
                    nur = 312,
                    streak = 7
                )
                RolePanelView(role = "warrior")
                MythologySection()
            }
        }
    }
}

