package app.umaia.android.ui.screens.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.umaia.android.domain.repository.LeaderboardEntry
import app.umaia.android.domain.repository.LeaderboardPeriod
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onDismiss: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TC.bg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TC.muted.copy(alpha = 0.3f)) }
    ) {
        LeaderboardSheetContent(
            state = state,
            onDismiss = onDismiss,
            onPeriodSelect = { viewModel.switchPeriod(it) }
        )
    }
}

@Composable
internal fun LeaderboardSheetContent(
    state: LeaderboardUiState,
    onDismiss: () -> Unit = {},
    onPeriodSelect: (LeaderboardPeriod) -> Unit = {}
) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Title row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                s.leaderboardTitle,
                color = TC.text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("✕", color = TC.muted.copy(alpha = 0.6f), fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Period picker
        PeriodPicker(
            selected = state.period,
            onSelect = onPeriodSelect,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        // My rank banner
        val myRank = state.data?.myRank
        val mySteps = state.data?.mySteps
        if (myRank != null && mySteps != null) {
            MyRankBanner(rank = myRank, steps = mySteps, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
        }

        // Content
        when {
            state.isLoading && state.data == null -> {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Gold)
                }
            }
            state.error != null && state.data == null -> {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Error", color = Ember, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            state.data?.entries.isNullOrEmpty() -> {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(
                        s.leaderboardEmpty,
                        color = TC.muted.copy(alpha = 0.5f), fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.data?.entries ?: emptyList()) { entry ->
                        LeaderboardRowItem(entry)
                    }
                }
            }
        }
    }
}

// ── Period Picker ─────────────────────────────────────────────────────────────

@Composable
private fun PeriodPicker(
    selected: LeaderboardPeriod,
    onSelect: (LeaderboardPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val periods = listOf(
        LeaderboardPeriod.DAILY   to s.periodToday,
        LeaderboardPeriod.WEEKLY  to s.periodWeek,
        LeaderboardPeriod.MONTHLY to s.periodMonth,
        LeaderboardPeriod.ALLTIME to s.periodAllTime
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        periods.forEach { (period, label) ->
            val isSelected = period == selected
            TextButton(
                onClick = { onSelect(period) },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) Gold.copy(alpha = 0.25f) else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TC.text else TC.muted.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── My Rank Banner ────────────────────────────────────────────────────────────

@Composable
private fun MyRankBanner(rank: Int, steps: Int, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TC.card, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(s.yourRank, color = TC.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            "#$rank",
            color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(12.dp))
        Text(
            s.stepsCount(steps),
            color = TC.text, fontSize = 12.sp
        )
    }
}

// ── Leaderboard Row ───────────────────────────────────────────────────────────

@Composable
private fun LeaderboardRowItem(entry: LeaderboardEntry) {
    val s = LocalStrings.current
    val medalEmoji = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (entry.isMe) Gold.copy(alpha = 0.10f) else TC.card,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank circle / medal
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (medalEmoji != null) Gold.copy(alpha = 0.15f) else TC.cardAlt,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (medalEmoji != null) {
                Text(medalEmoji, fontSize = 18.sp)
            } else {
                Text(
                    "#${entry.rank}",
                    color = TC.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Name
        Column(Modifier.weight(1f)) {
            Text(
                entry.fullName ?: s.anonymousName,
                color = if (entry.isMe) Gold else TC.text,
                fontSize = 14.sp,
                fontWeight = if (entry.isMe) FontWeight.Bold else FontWeight.Normal
            )
            if (entry.isMe) {
                Text(
                    s.youIndicator,
                    color = Gold, fontSize = 10.sp,
                    modifier = Modifier
                        .background(Gold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        // Steps
        Text(
            "%,d".format(entry.totalSteps),
            color = if (entry.isMe) Gold else TC.text,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold
        )
    }
}
