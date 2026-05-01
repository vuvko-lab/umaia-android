package app.umaia.android.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ── Palette — exact hex values from iOS Theme.swift ──────────────────────────

val NightBlue         = Color(0xFF0D1523)
val NightMid          = Color(0xFF1E2A3E)
val DeepNavy          = Color(0xFF17233A)
val Gold              = Color(0xFFE8873A)
val GoldLight         = Color(0xFFF4B06B)
val GoldDim           = Color(0xFFA9692E)
val Ember             = Color(0xFFE95A0B)
val Parchment         = Color(0xFFE8E0D2)
val ParchmentDark     = Color(0xFFD4C9B8)
val TerracottaRed     = Color(0xFF8B3A2A)
val SageGreen         = Color(0xFF5A7A5A)
val MistGray          = Color(0xFF8A9BA8)
val OraclePurple      = Color(0xFF7A3D9E)
val OracleLight       = Color(0xFF9F5CC4)
val NutritionGreen    = Color(0xFF2E8559)
val NutritionGreenLight = Color(0xFF4DA675)
val Sky               = Color(0xFF2B5B87)
val Earth             = Color(0xFF734B24)
val SurfaceVariant    = Color(0xFF1E2D3D)

val UmaiaDarkColors = darkColorScheme(
    primary           = Gold,
    onPrimary         = Color.White,
    background        = NightBlue,
    onBackground      = Parchment,
    surface           = NightMid,
    onSurface         = Parchment,
    surfaceVariant    = SurfaceVariant,
    onSurfaceVariant  = ParchmentDark,
    secondary         = GoldLight,
    // Lighter purple on dark navy bg for better contrast (~7:1).
    // Light theme keeps OraclePurple (~6.8:1 on cream).
    tertiary          = OracleLight
)

val UmaiaLightColors = lightColorScheme(
    primary           = Color(0xFFB85C10),
    onPrimary         = Color.White,
    background        = Color(0xFFF5EDE0),
    onBackground      = Color(0xFF1A1208),
    surface           = Color(0xFFEDE2D2),
    onSurface         = Color(0xFF2A1E10),
    surfaceVariant    = Color(0xFFE0D4C3),
    onSurfaceVariant  = Color(0xFF3D2E1A),
    secondary         = Color(0xFFD4801A),
    tertiary          = OraclePurple
)

// ── Theme-aware colors ─────────────────────────────────────────────────────────
// Use TC.bg, TC.card, TC.text, etc. in Composables instead of raw color constants.

@Stable
data class UmaiaColors(
    val bg: Color,
    val card: Color,
    val cardAlt: Color,
    val text: Color,
    val textDim: Color,
    val muted: Color,
)

private val DarkUmaiaColors = UmaiaColors(
    bg       = NightBlue,
    card     = NightMid,
    cardAlt  = SurfaceVariant,
    text     = Parchment,
    textDim  = ParchmentDark,
    muted    = MistGray,
)

private val LightUmaiaColors = UmaiaColors(
    bg       = Color(0xFFF5EDE0),
    card     = Color(0xFFEDE2D2),
    cardAlt  = Color(0xFFE0D4C3),
    text     = Color(0xFF1A1208),
    textDim  = Color(0xFF3D2E1A),
    muted    = Color(0xFF5A6670),
)

val LocalUmaiaColors = staticCompositionLocalOf { DarkUmaiaColors }

/** Short accessor for theme-aware colors inside @Composable functions. */
val TC: UmaiaColors
    @Composable
    @ReadOnlyComposable
    get() = LocalUmaiaColors.current

@Composable
fun UmaiaTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val umaiaColors = if (darkTheme) DarkUmaiaColors else LightUmaiaColors
    CompositionLocalProvider(LocalUmaiaColors provides umaiaColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) UmaiaDarkColors else UmaiaLightColors,
            content = content
        )
    }
}

// ── Button styles — mirrors iOS UmaiaButtonStyle ──────────────────────────────

@Composable
fun UmaiaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled)
                        Brush.linearGradient(listOf(Gold, Ember))
                    else
                        Brush.linearGradient(listOf(GoldDim, GoldDim)),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun UmaiaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Gold.copy(alpha = 0.4f))
    ) {
        Text(text, fontWeight = FontWeight.Medium)
    }
}

// ── Card modifier — mirrors iOS umaiaCard() ───────────────────────────────────

fun Modifier.umaiaCard(color: Color = SurfaceVariant): Modifier =
    this
        .background(color, RoundedCornerShape(14.dp))
        .padding(12.dp)
