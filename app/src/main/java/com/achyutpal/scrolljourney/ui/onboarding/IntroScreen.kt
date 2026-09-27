package com.achyutpal.scrolljourney.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import kotlinx.coroutines.launch

private data class IntroPage(
    val emoji: String,
    @StringRes val title: Int,
    @StringRes val body: Int,
)

private val introPages = listOf(
    IntroPage("🚀", R.string.intro1_title, R.string.intro1_body),
    IntroPage("🔒", R.string.intro2_title, R.string.intro2_body),
)

/** Intro 1 → Intro 2, swipeable. [onFinished] fires from the last page's button. */
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == introPages.lastIndex

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                IntroPageContent(introPages[index])
            }

            PageDots(count = introPages.size, selected = pagerState.currentPage)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(if (isLastPage) R.string.action_continue else R.string.action_next))
            }
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = page.emoji, fontSize = 72.sp)
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageDots(count: Int, selected: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == selected) 10.dp else 8.dp)
                    .background(
                        color = if (index == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun IntroScreenPreview() {
    ScrollJourneyTheme(darkTheme = true) { IntroScreen(onFinished = {}) }
}
