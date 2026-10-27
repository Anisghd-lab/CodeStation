package com.ghdinteractivestudio.synapse.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SynapseDarkPrimary,
    onPrimary = SynapseDarkOnPrimary,
    primaryContainer = SynapseDarkPrimaryVariant,
    secondary = SynapseDarkSecondary,
    onSecondary = SynapseDarkOnSecondary,
    secondaryContainer = SynapseDarkSecondaryVariant,
    background = SynapseDarkBackground,
    onBackground = SynapseDarkTextPrimary,
    surface = SynapseDarkSurface,
    onSurface = SynapseDarkTextPrimary,
    surfaceVariant = SynapseDarkSurfaceVariant,
    onSurfaceVariant = SynapseDarkTextSecondary,
    outline = SynapseDarkOutline,
    outlineVariant = SynapseDarkSurfaceHighlight,
    error = SynapseDarkError
)

private val LightColorScheme = lightColorScheme(
    primary = SynapseLightPrimary,
    onPrimary = SynapseLightOnPrimary,
    secondary = SynapseLightSecondary,
    onSecondary = SynapseLightOnSecondary,
    background = SynapseLightBackground,
    onBackground = SynapseLightTextPrimary,
    surface = SynapseLightSurface,
    onSurface = SynapseLightTextPrimary,
    surfaceVariant = SynapseLightSurfaceVariant,
    onSurfaceVariant = SynapseLightTextSecondary,
    outline = SynapseLightOutline,
    outlineVariant = SynapseLightSurfaceHighlight,
    error = SynapseLightError
)

@Composable
fun SynapseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSynapseSpacing provides SynapseSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SynapseTypography,
            shapes = SynapseShapes,
            content = content
        )
    }
}
