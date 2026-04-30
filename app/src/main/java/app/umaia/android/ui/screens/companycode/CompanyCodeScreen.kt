package app.umaia.android.ui.screens.companycode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.theme.Gold
import app.umaia.android.ui.theme.NightBlue
import app.umaia.android.ui.theme.TC

/**
 * Shown once after first sign-in (and any time `companyCode == null` and the
 * user hasn't tapped Skip yet). Two paths:
 *  - Join: enters an invite code → `set_company_code` RPC → cohort leaderboard
 *    + Seer tab unlock.
 *  - Skip: stays in the public pool. We persist `companyChoiceMade(uid)` so
 *    the screen doesn't reappear next launch.
 */
@Composable
fun CompanyCodeScreen(
    onDone: () -> Unit,
    viewModel: CompanyCodeViewModel = hiltViewModel()
) {
    val s = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(state.completed) { if (state.completed) onDone() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TC.bg)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Text("🏢", fontSize = 48.sp, modifier = Modifier.align(Alignment.CenterHorizontally))

        Text(
            s.companyCodeJoinTitle,
            color = TC.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            s.companyCodeBody,
            color = TC.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase().take(20) },
            label = { Text(s.companyCodeFieldLabel) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            isError = state.error != null,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = TC.muted,
                focusedLabelColor = Gold,
                unfocusedLabelColor = TC.muted,
                focusedTextColor = TC.text,
                unfocusedTextColor = TC.text,
                cursorColor = Gold
            )
        )

        state.error?.let { err ->
            val text = if (err == "INVALID") s.companyCodeInvalid else err
            Text(text, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Button(
            onClick = { viewModel.submit(code) },
            enabled = code.isNotBlank() && !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = NightBlue, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(s.companyCodeJoin, color = NightBlue, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.weight(1f))

        TextButton(
            onClick = { viewModel.skip() },
            modifier = Modifier.fillMaxWidth()
        ) { Text(s.companyCodeSkip, color = TC.muted) }
    }
}
