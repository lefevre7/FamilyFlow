package com.debanshu.xcalendar.ui.screen.onboardingScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.ui.theme.XCalendarTheme

private data class OnboardingPage(
    val title: String,
    val body: String,
    val bullets: List<String>,
)

private val pages =
    listOf(
        OnboardingPage(
            title = "Welcome to Family Flow",
            body = "This app helps Mom offload the family mental load.",
            bullets =
                listOf(
                    "People-first planning, not just a calendar.",
                    "Less overwhelm from multiple schedules.",
                    "Designed for fast decisions and low friction.",
                ),
        ),
        OnboardingPage(
            title = "Three Simple Modes",
            body = "Move between execution views without hunting for information.",
            bullets =
                listOf(
                    "Today: Survival mode with what matters now.",
                    "Week: Reality check by day and by person.",
                    "Plan: Brain dump and suggestions for flexible tasks.",
                ),
        ),
        OnboardingPage(
            title = "Capture First, Organize Later",
            body = "Add tasks quickly, then let the app help structure them.",
            bullets =
                listOf(
                    "Quick task from the add button.",
                    "Voice capture for fast brain dumps.",
                    "Scan schedules and confirm suggested events.",
                ),
        ),
    )

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    var pageIndex by rememberSaveable { mutableStateOf(0) }
    val page = pages[pageIndex]
    val isLastPage = pageIndex == pages.lastIndex

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(XCalendarTheme.colorScheme.surfaceContainerLow)
                .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Family Flow",
                style = XCalendarTheme.typography.headlineLarge,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            Text(
                text = "Mental offloading for ADHD moms",
                style = XCalendarTheme.typography.bodyLarge,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )

            Card(shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = page.title,
                        style = XCalendarTheme.typography.headlineSmall,
                        color = XCalendarTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = page.body,
                        style = XCalendarTheme.typography.bodyLarge,
                        color = XCalendarTheme.colorScheme.onSurfaceVariant,
                    )
                    page.bullets.forEach { bullet ->
                        Text(
                            text = "• $bullet",
                            style = XCalendarTheme.typography.bodyMedium,
                            color = XCalendarTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                pages.forEachIndexed { index, _ ->
                    val selected = index == pageIndex
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (selected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        XCalendarTheme.colorScheme.primary
                                    } else {
                                        XCalendarTheme.colorScheme.outlineVariant
                                    },
                                ),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSkip) {
                    Text("Skip")
                }
                Spacer(modifier = Modifier.weight(1f))
                if (pageIndex > 0) {
                    TextButton(onClick = { pageIndex -= 1 }) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            pageIndex += 1
                        }
                    },
                ) {
                    Text(if (isLastPage) "Open Today" else "Next")
                }
            }
        }
    }
}
