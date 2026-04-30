package app.umaia.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.theme.Gold
import app.umaia.android.ui.theme.NightBlue
import app.umaia.android.ui.theme.TC

/**
 * Shown once on first sign-in. The earlier (v1.2) dialog talked about
 * "build for your tribe" and "consult the Oracle"; v1.3 trims to walking-only
 * since the building loop is gone.
 */
@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.welcomeTitle, color = Gold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    "👣 " to s.welcomeRuleWalk,
                    "🌿 " to s.welcomeRuleLearn
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

@Composable
fun ReturnWelcomeDialog(daysAway: Int, nurBonus: Int, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val emoji = when {
        daysAway == 1 -> "👋"
        daysAway in 2..3 -> "🏕️"
        daysAway in 4..7 -> "🐎"
        else -> "🌅"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.welcomeBackHello, color = Gold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(s.daysAway(daysAway), color = TC.muted, fontSize = 14.sp)
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
                                Text(s.returningBonus, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(s.nurAmount(nurBonus), color = TC.muted, fontSize = 11.sp)
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
