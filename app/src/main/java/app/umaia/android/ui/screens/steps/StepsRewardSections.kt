package app.umaia.android.ui.screens.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.umaia.android.domain.repository.MonthlyWinnerStatus
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.theme.Ember
import app.umaia.android.ui.theme.Gold
import app.umaia.android.ui.theme.NightBlue
import app.umaia.android.ui.theme.SageGreen
import app.umaia.android.ui.theme.TC

// ── Monthly standing hero ────────────────────────────────────────────────────

/**
 * "Rank #N · X Nur this month — Resets on the 1st of each month" widget at
 * the top of the Walk tab. Sourced from the **server-truth** monthly
 * leaderboard RPC (`LeaderboardData.myNur`), so the displayed Nur agrees
 * with the leaderboard sheet and the reward tile progress.
 *
 * `monthlyNur == null` means the leaderboard fetch hasn't returned yet (or
 * the user has zero qualifying server-side step rows this month) — show "—"
 * while loading, "0" otherwise.
 */
@Composable
fun MonthlyStandingHero(rank: Int?, monthlyNur: Int?, isLoading: Boolean) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.15f), TC.card)),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🏆", fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                rank?.let { s.weeklyStandingRank(it) } ?: "—",
                color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            val nurLabel = when {
                monthlyNur != null -> "$monthlyNur"
                isLoading -> "—"
                else -> "0"
            }
            Text(
                "$nurLabel ${s.statNur} · ${s.monthlyResets}",
                color = TC.muted, fontSize = 11.sp
            )
        }
    }
}

// ── Congrats banner (3 mutually exclusive states) ────────────────────────────

/**
 * Renders only when `myMonthlyNur >= targetNur`. The three states map directly
 * to the server-side podium model:
 *   - amWinner == true                                   → "you're rank N"
 *   - !amWinner && spotsRemaining > 0                    → "X of 3 taken"
 *   - !amWinner && spotsRemaining == 0                   → "podium full"
 */
@Composable
fun CongratsBanner(status: MonthlyWinnerStatus) {
    if (status.myMonthlyNur < status.targetNur) return
    val s = LocalStrings.current

    when {
        status.amWinner -> {
            val medal = when (status.myRank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "🏆" }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Gold, Ember)),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(medal, fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s.congratsWinner(status.myRank ?: 0),
                        color = NightBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
                Text(s.claimBeforeNextMonth, color = NightBlue.copy(alpha = 0.85f), fontSize = 12.sp)
            }
        }
        status.spotsRemaining > 0 -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TC.card, RoundedCornerShape(14.dp))
                    .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    s.congratsTargetHitNotFull(status.spotsTaken),
                    color = TC.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TC.card, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏁", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(s.congratsPodiumFull, color = TC.muted, fontSize = 13.sp)
            }
        }
    }
}

// ── Nur rewards strip — single T-shirt tile ──────────────────────────────────

@Composable
fun NurRewardsStrip(
    monthlyServerNur: Int?,
    monthlyNurSubtract: Int,
    status: MonthlyWinnerStatus?,
    alreadyClaimed: Boolean,
    onClaimTapped: () -> Unit
) {
    val s = LocalStrings.current
    val target = status?.targetNur ?: WinnerStatusViewModel.MONTHLY_REWARD_COST_NUR
    val unlocked = !alreadyClaimed && (status?.amWinner == true)
    // v1.3.1: tile reads server-truth monthly Nur (asymptotic per-day formula
    // matches what the leaderboard hero shows) minus the local "claim-subtract"
    // counter so the bar visibly drops after a successful claim.
    val monthlyNur = ((monthlyServerNur ?: 0) - monthlyNurSubtract).coerceAtLeast(0)
    val progress = (monthlyNur.toFloat() / target).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👕", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Umaia T-shirt", color = TC.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("$monthlyNur / $target ${s.statNur}", color = TC.muted, fontSize = 11.sp)
            }
            when {
                alreadyClaimed -> Text("✅", fontSize = 22.sp)
                unlocked -> Button(
                    onClick = onClaimTapped,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) { Text(s.claim, color = NightBlue, fontWeight = FontWeight.Bold) }
                else -> Text("🔒", fontSize = 18.sp, color = TC.muted)
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            color = if (unlocked) SageGreen else Gold,
            trackColor = Gold.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
        Text(s.morePartnersComingSoon, color = TC.muted, fontSize = 10.sp)
    }
}

// ── Earn-more hints ─────────────────────────────────────────────────────────

/**
 * "How to earn more Nur this month" pacing hint.
 *
 * Splits the remaining target across the days left in the calendar month
 * (Asia/Almaty), subtracts the recurring +15 daily bonuses (login +5,
 * share +10), and back-solves the steps needed today via [stepsForDailyNur].
 *
 * Three states:
 *  - already on pace today → "you've hit today's pace"
 *  - reachable → "walk ~N more steps today"
 *  - daily share is past the asymptotic cap → "walk as much as you can"
 */
@Composable
fun EarnMoreHints(
    monthlyNur: Int,
    target: Int,
    todayStepNur: Int,
    todaySteps: Int,
) {
    val s = LocalStrings.current
    val remaining = (target - monthlyNur).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(s.earnMoreThisMonth, color = TC.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        if (remaining == 0) {
            HintRow("✅", s.earnMoreMetForToday)
            return@Column
        }

        val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Almaty"))
        val daysInMonth = today.lengthOfMonth()
        val daysLeftInclToday = (daysInMonth - today.dayOfMonth + 1).coerceAtLeast(1)
        val dailyTotalNurNeeded = kotlin.math.ceil(remaining.toDouble() / daysLeftInclToday).toInt()
        val dailyStepsNurNeeded = (dailyTotalNurNeeded - DAILY_BONUS_NUR).coerceAtLeast(0)

        when {
            todayStepNur >= dailyStepsNurNeeded -> {
                HintRow("✅", s.earnMoreMetForToday)
            }
            else -> {
                val needed = app.umaia.android.domain.stepsForDailyNur(dailyStepsNurNeeded)
                if (needed == null) {
                    HintRow("⚠️", s.earnMoreBeyondCap)
                } else {
                    val extra = (needed - todaySteps).coerceAtLeast(0)
                    HintRow("👣", s.earnMoreToday(extra))
                }
            }
        }
    }
}

/** Daily login (+5) + daily share (+10) — the recurring bonuses every user
 *  can claim on top of walking. EarnMoreHints subtracts this from the
 *  pacing target so the suggestion isn't impossibly high. */
private const val DAILY_BONUS_NUR = 15

@Composable
private fun HintRow(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = TC.muted, fontSize = 12.sp)
    }
}
