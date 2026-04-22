package app.umaia.android.ui.screens.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.umaia.android.R
import app.umaia.android.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private data class OnboardingPage(
    val imageRes: Int,
    val titleRes: Int,
    val bodyRes: Int,
    val showLogo: Boolean = false
)

private val pages = listOf(
    OnboardingPage(R.drawable.onboarding1, R.string.onboarding_title_1, R.string.onboarding_body_1, showLogo = true),
    OnboardingPage(R.drawable.onboarding2, R.string.onboarding_title_2, R.string.onboarding_body_2),
    OnboardingPage(R.drawable.onboarding3, R.string.onboarding_title_3, R.string.onboarding_body_3),
    OnboardingPage(R.drawable.onboarding5, R.string.onboarding_title_4, R.string.onboarding_body_4),
    OnboardingPage(R.drawable.onboarding4, R.string.onboarding_title_5, R.string.onboarding_body_5),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TC.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pager takes most of the screen
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Capsule dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { i ->
                        val isActive = i == pagerState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (isActive) 20.dp else 8.dp,
                            animationSpec = tween(250),
                            label = "dot"
                        )
                        Box(
                            modifier = Modifier
                                .width(width)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isActive) Gold else Gold.copy(alpha = 0.25f))
                        )
                    }
                }

                UmaiaButton(
                    text = if (isLast) stringResource(R.string.onboarding_get_started)
                           else        stringResource(R.string.onboarding_next),
                    onClick = {
                        if (isLast) onDone()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isLast) {
                    androidx.compose.material3.TextButton(onClick = onDone) {
                        Text(
                            stringResource(R.string.onboarding_skip),
                            color = Gold.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val imgHeight = maxHeight * 0.55f

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image card with gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(imgHeight)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(page.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient fade at bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.5f to Color.Transparent,
                                0.75f to TC.bg.copy(alpha = 0.5f),
                                1f to TC.bg
                            )
                        )
                )
                // Subtle gold border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                )
            }

            // Text content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (page.showLogo) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.logo_umaia),
                        contentDescription = "Umaia",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Text(
                    stringResource(page.titleRes),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(page.bodyRes),
                    fontSize = 14.sp,
                    color = TC.text.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
