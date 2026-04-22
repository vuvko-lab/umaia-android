package app.umaia.android.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.umaia.android.R
import app.umaia.android.data.auth.AuthState
import app.umaia.android.ui.screens.legal.LegalDocument
import app.umaia.android.ui.screens.legal.LegalScreen
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.theme.UmaiaButton
import app.umaia.android.ui.theme.*

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val error     by viewModel.error.collectAsStateWithLifecycle()
    val loading   by viewModel.loading.collectAsStateWithLifecycle()

    var email         by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var isSignUp      by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var sheetDocument by remember { mutableStateOf<LegalDocument?>(null) }
    var termsError    by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val context      = LocalContext.current
    val s            = LocalStrings.current

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) onLoggedIn()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TC.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Logo + title
            Image(
                painter = painterResource(R.drawable.logo_umaia),
                contentDescription = "Umaia",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Text(
                "UMAIA",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                s.loginTagline,
                fontSize = 14.sp,
                color = TC.text.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp, start = 12.dp, end = 12.dp)
            )

            // Form card — nightMid background, transparent TextFields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TC.card)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✉", fontSize = 15.sp, color = TC.text.copy(alpha = 0.35f), modifier = Modifier.width(24.dp))
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text(s.email, color = TC.text.copy(alpha = 0.35f), fontSize = 15.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = transparentFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = TC.text, fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = TC.text.copy(alpha = 0.08f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔒", fontSize = 15.sp, color = TC.text.copy(alpha = 0.35f), modifier = Modifier.width(24.dp))
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text(s.password, color = TC.text.copy(alpha = 0.35f), fontSize = 15.sp) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            doAuth(email, password, isSignUp, agreedToTerms, s.termsError, viewModel) { termsError = it }
                        }),
                        colors = transparentFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = TC.text, fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            UmaiaButton(
                text = if (isSignUp) s.signUp else s.signIn,
                enabled = !loading,
                onClick = { termsError = null; doAuth(email, password, isSignUp, agreedToTerms, s.termsError, viewModel) { termsError = it } },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )

            // OR divider
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(Modifier.weight(1f), color = TC.text.copy(alpha = 0.1f), thickness = 0.5.dp)
                Text("  ${s.orSeparator}  ", color = TC.text.copy(alpha = 0.3f), fontSize = 11.sp)
                HorizontalDivider(Modifier.weight(1f), color = TC.text.copy(alpha = 0.1f), thickness = 0.5.dp)
            }

            // Google sign-in
            Button(
                onClick = { viewModel.signInWithGoogle(context) },
                enabled = !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TC.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, TC.text.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("G  ", fontWeight = FontWeight.Bold, color = GoldLight, fontSize = 16.sp)
                Text(s.continueWithGoogle, color = TC.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            // T&C checkbox — only shown when signing up
            if (isSignUp) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Gold, uncheckedColor = TC.muted.copy(alpha = 0.5f)
                        )
                    )
                    Text(s.agreeToTerms, color = TC.text.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
                    TextButton(onClick = { sheetDocument = LegalDocument.PRIVACY_POLICY }) {
                        Text(s.privacyPolicy, color = Gold, fontSize = 12.sp)
                    }
                    TextButton(onClick = { sheetDocument = LegalDocument.TERMS_OF_SERVICE }) {
                        Text(s.termsOfService, color = Gold, fontSize = 12.sp)
                    }
                }
            }

            TextButton(
                onClick = { isSignUp = !isSignUp; viewModel.clearError(); agreedToTerms = false },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    if (isSignUp) s.alreadyHaveAccount else s.noAccount,
                    color = TC.text.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                MessageBanner(text = it, color = TerracottaRed, icon = "!")
            }
            termsError?.let {
                Spacer(Modifier.height(8.dp))
                MessageBanner(text = it, color = TerracottaRed, icon = "!")
            }

            if (loading) {
                CircularProgressIndicator(color = Gold, modifier = Modifier.padding(top = 14.dp).size(24.dp))
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // Legal document sheet
    sheetDocument?.let { doc ->
        LegalScreen(document = doc, onDismiss = { sheetDocument = null })
    }
}

private fun doAuth(
    email: String,
    password: String,
    isSignUp: Boolean,
    agreedToTerms: Boolean,
    termsErrorMsg: String,
    viewModel: AuthViewModel,
    onError: (String) -> Unit
) {
    val e = email.trim().lowercase()
    val p = password.trim()
    if (e.isEmpty() || p.isEmpty()) return
    if (isSignUp && !agreedToTerms) {
        onError(termsErrorMsg)
        return
    }
    if (isSignUp) viewModel.signUp(e, p) else viewModel.signIn(e, p)
}

@Composable
private fun MessageBanner(text: String, color: Color, icon: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = color, fontSize = 12.sp)
        Text(text, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor   = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor  = Color.Transparent,
    focusedTextColor        = TC.text,
    unfocusedTextColor      = TC.text,
    cursorColor             = Gold
)
