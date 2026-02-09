package com.debanshu.xcalendar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.ui.theme.XCalendarTheme

@Composable
fun ScreenPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = XCalendarTheme.typography.headlineMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = XCalendarTheme.typography.bodyMedium,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
    }
}
