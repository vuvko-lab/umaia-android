package app.umaia.android.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.umaia.android.R
import app.umaia.android.data.local.ThemeMode
import app.umaia.android.domain.model.*
import app.umaia.android.ui.screens.legal.LegalDocument
import app.umaia.android.ui.screens.legal.LegalScreen
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.theme.*
import java.util.Calendar

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    var sheetDocument by remember { mutableStateOf<LegalDocument?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TC.bg)
            .verticalScroll(rememberScrollState())
    ) {
        HeroSection()

        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileCard(
                name = state.profile?.fullName,
                email = state.email,
                nur = state.liveNur,
                streak = state.profile?.currentStreak ?: 0
            )

            // Role panel
            val role = state.profile?.tribalRole
            if (!role.isNullOrBlank() && role != "newcomer") {
                RolePanelView(role = role)
            }

            MythologySection()

            PillarsSection()

            // Settings
            SettingsSection(
                themeMode = themeMode,
                language = language,
                onThemeChange = { viewModel.setThemeMode(it) },
                onLanguageChange = { viewModel.setLanguage(it) },
                onChangePassword = { showChangePassword = true }
            )

            val s = LocalStrings.current

            // Legal links
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TC.card, RoundedCornerShape(14.dp))
            ) {
                Surface(
                    onClick = { sheetDocument = LegalDocument.PRIVACY_POLICY },
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.privacyPolicy, color = TC.text.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("›", color = TC.muted, fontSize = 18.sp)
                    }
                }
                HorizontalDivider(color = TC.text.copy(alpha = 0.06f), thickness = 0.5.dp)
                Surface(
                    onClick = { sheetDocument = LegalDocument.TERMS_OF_SERVICE },
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.termsOfService, color = TC.text.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("›", color = TC.muted, fontSize = 18.sp)
                    }
                }
            }

            // Sign out
            OutlinedButton(
                onClick = { viewModel.signOut() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TC.text.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(s.leaveTheSteppe, color = TC.text.copy(alpha = 0.5f), fontSize = 15.sp)
            }

            // Delete account
            TextButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(s.deleteAccount, color = TerracottaRed.copy(alpha = 0.7f), fontSize = 14.sp)
            }

            state.deleteError?.let { error ->
                Text(error, color = TerracottaRed, fontSize = 12.sp)
            }
        }
    }

    if (showDeleteConfirmation) {
        val ds = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(ds.deleteAccountTitle, color = TC.text) },
            text = { Text(ds.deleteAccountWarning, color = TC.text.copy(alpha = 0.7f), fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false; viewModel.deleteAccount() },
                    enabled = !state.isDeleting
                ) {
                    Text(ds.delete, color = TerracottaRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(ds.cancel, color = TC.text.copy(alpha = 0.6f))
                }
            },
            containerColor = TC.card
        )
    }

    sheetDocument?.let { doc ->
        LegalScreen(document = doc, onDismiss = { sheetDocument = null })
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            isLoading = state.isChangingPassword,
            success = state.passwordChangeSuccess,
            error = state.passwordChangeError,
            onConfirm = { newPass -> viewModel.changePassword(newPass) },
            onDismiss = { showChangePassword = false; viewModel.clearPasswordState() }
        )
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────

@Composable
internal fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Brush.verticalGradient(listOf(Ember.copy(alpha = 0.4f), TC.bg)))
    ) {
        // Watermark logo in center
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.logo_umaia),
            contentDescription = null,
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.Center)
                .alpha(0.06f)
        )
        // Logo + title at bottom-left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.logo_umaia),
                contentDescription = "Umaia",
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(8.dp))
            val heroS = LocalStrings.current
            Text(heroS.goddessUmaia, color = GoldLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(heroS.goddessSubtitle, color = TC.text, fontSize = 13.sp)
        }
    }
}

// ── Profile Card ──────────────────────────────────────────────────────────────

@Composable
internal fun ProfileCard(name: String?, email: String, nur: Int, streak: Int) {
    val s = LocalStrings.current
    val season = when (Calendar.getInstance().get(Calendar.MONTH) + 1) {
        in 3..5 -> s.seasonSpring
        in 6..8 -> s.seasonSummer
        in 9..11 -> s.seasonAutumn
        else -> s.seasonWinter
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Gold.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name ?: s.nomadFallback, color = TC.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(email, color = TC.muted, fontSize = 10.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("$nur", s.totalNur, color = Gold, modifier = Modifier.weight(1f))
            StatBox("$streak", s.streak, icon = "🔥", color = Ember, modifier = Modifier.weight(1f))
            StatBox(season, s.seasonLabel, icon = "📅", color = TC.text.copy(alpha = 0.6f), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, color: androidx.compose.ui.graphics.Color, icon: String? = null, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(TC.text.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Text(icon, fontSize = 10.sp); Spacer(Modifier.width(2.dp)) }
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = TC.muted, fontSize = 9.sp)
    }
}

// ── Role Panel ────────────────────────────────────────────────────────────────

@Composable
internal fun RolePanelView(role: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OraclePurple.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🛡 ", fontSize = 12.sp)
            Text("YOUR ROLE", color = OracleLight, fontSize = 10.sp, letterSpacing = 1.5.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tribalRoleEmoji(role), fontSize = 36.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(tribalRoleTitle(role), color = OracleLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(tribalRoleFocus(role), color = TC.muted, fontSize = 11.sp)
            }
        }
    }
}

// ── Mythology ─────────────────────────────────────────────────────────────────

@Composable
internal fun MythologySection() {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(s.mythologyTitle, color = TC.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        s.mythologyText.split("\n\n").forEach { para ->
            Text(para.trim(), color = TC.text.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

// ── Four Pillars ──────────────────────────────────────────────────────────────

@Composable
private fun PillarsSection() {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(s.fourPillarsTitle, color = TC.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        val chunked = s.pillars.chunked(2)
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (emoji, title, desc) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(TC.card, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(emoji, fontSize = 24.sp)
                        Text(title, color = TC.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(desc, color = TC.muted, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ── Settings ──────────────────────────────────────────────────────────────────

@Composable
internal fun SettingsSection(
    themeMode: ThemeMode,
    language: String,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (String) -> Unit,
    onChangePassword: () -> Unit
) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(14.dp))
    ) {
        // Language row
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(s.languageLabel, color = TC.text.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.weight(1f))
            SegmentedToggle(
                options = listOf("EN", "RU", "KZ"),
                selected = when (language) { "ru" -> "RU"; "kk" -> "KZ"; else -> "EN" },
                onSelect = { onLanguageChange(when (it) { "RU" -> "ru"; "KZ" -> "kk"; else -> "en" }) }
            )
        }

        HorizontalDivider(color = TC.text.copy(alpha = 0.06f), thickness = 0.5.dp)

        // Theme row
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(s.themeLabel, color = TC.text.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.weight(1f))
            SegmentedToggle(
                options = listOf(s.themeLight, s.themeDark, s.themeAuto),
                selected = when (themeMode) {
                    ThemeMode.LIGHT  -> s.themeLight
                    ThemeMode.DARK   -> s.themeDark
                    ThemeMode.SYSTEM -> s.themeAuto
                },
                onSelect = {
                    onThemeChange(when (it) {
                        s.themeLight -> ThemeMode.LIGHT
                        s.themeDark  -> ThemeMode.DARK
                        else          -> ThemeMode.SYSTEM
                    })
                }
            )
        }

        HorizontalDivider(color = TC.text.copy(alpha = 0.06f), thickness = 0.5.dp)

        // Change password row
        Surface(
            onClick = onChangePassword,
            color = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.changePassword, color = TC.text.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("›", color = TC.muted, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun SegmentedToggle(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .background(TC.bg, RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                color = if (isSelected) Gold.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
            ) {
                Text(
                    option,
                    color = if (isSelected) TC.text else TC.muted,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Change Password Dialog ────────────────────────────────────────────────────

@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    success: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val mismatch = newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword
    val tooShort = newPassword.isNotEmpty() && newPassword.length < 8

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TC.card,
        title = { Text(s.changePasswordTitle, color = TC.text) },
        text = {
            if (success) {
                Text(s.passwordUpdated, color = SageGreen, fontSize = 14.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(s.newPassword, color = TC.muted, fontSize = 12.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = TC.muted.copy(alpha = 0.3f),
                            focusedTextColor = TC.text,
                            unfocusedTextColor = TC.text
                        ),
                        isError = tooShort
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(s.confirmPassword, color = TC.muted, fontSize = 12.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = TC.muted.copy(alpha = 0.3f),
                            focusedTextColor = TC.text,
                            unfocusedTextColor = TC.text
                        ),
                        isError = mismatch
                    )
                    when {
                        tooShort      -> Text(s.passwordTooShort, color = TerracottaRed, fontSize = 12.sp)
                        mismatch      -> Text(s.passwordMismatch, color = TerracottaRed, fontSize = 12.sp)
                        error != null -> Text(error, color = TerracottaRed, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (success) {
                TextButton(onClick = onDismiss) { Text(s.done, color = Gold) }
            } else {
                TextButton(
                    onClick = { onConfirm(newPassword) },
                    enabled = !isLoading && newPassword.length >= 8 && newPassword == confirmPassword
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Gold, strokeWidth = 2.dp)
                    else Text(s.update, color = Gold)
                }
            }
        },
        dismissButton = {
            if (!success) {
                TextButton(onClick = onDismiss) { Text(s.cancel, color = TC.text.copy(alpha = 0.6f)) }
            }
        }
    )
}

