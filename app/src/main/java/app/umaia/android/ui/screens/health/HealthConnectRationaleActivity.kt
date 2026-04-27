package app.umaia.android.ui.screens.health

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.umaia.android.data.local.AppPreferences
import app.umaia.android.ui.strings.EnStrings
import app.umaia.android.ui.strings.KkStrings
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.strings.RuStrings
import app.umaia.android.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity that handles `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`. Health Connect
 * launches this when the user is granting permissions to our app — showing what data we
 * read, why, and where to find our privacy policy. Required by Health Connect to consider
 * an app a valid permission requester (otherwise its UI silently closes).
 */
@AndroidEntryPoint
class HealthConnectRationaleActivity : AppCompatActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPreferences.applyLocale()
        enableEdgeToEdge()
        val strings = when (appPreferences.language.value) {
            "en" -> EnStrings
            "kk" -> KkStrings
            else -> RuStrings // "ru" or unknown falls back to Russian
        }
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalStrings provides strings) {
                UmaiaTheme(darkTheme = false) {
                    HealthConnectRationaleScreen(onDone = { finish() })
                }
            }
        }
    }
}

@Composable
private fun HealthConnectRationaleScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val openLink: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TC.bg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text(s.hcRationaleTitle, color = TC.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            s.hcRationaleBody,
            color = TC.muted, fontSize = 14.sp, lineHeight = 20.sp
        )

        Spacer(Modifier.height(8.dp))

        DataTypeRow(emoji = "👣", label = s.hcStepsLabel, description = s.hcStepsDesc)

        Spacer(Modifier.height(8.dp))

        Text(s.hcNoWriteAssurance,
            color = TC.muted, fontSize = 13.sp, lineHeight = 18.sp)

        Spacer(Modifier.height(8.dp))

        LinkText(s.privacyPolicy, "https://www.umaia.tech/privacy", openLink)
        LinkText(s.termsOfService, "https://www.umaia.tech/terms", openLink)

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(s.gotIt, color = NightBlue, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DataTypeRow(emoji: String, label: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = TC.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = TC.muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun LinkText(label: String, url: String, onClick: (String) -> Unit) {
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = Gold, textDecoration = TextDecoration.Underline)) {
            append(label)
        }
    }
    Text(
        annotated,
        fontSize = 14.sp,
        modifier = Modifier.clickable { onClick(url) }
    )
}
