package com.example.smartdocsconvert.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class ExtendedColors(
    val surfaceRed: Color,
    val cardRed: Color,
    val filterBackground: Color,
    val filterSurface: Color,
    val filterText: Color,
    val selectedItem: Color,
    val accentTeal: Color,
    val goldColor: Color
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        surfaceRed = Color.Unspecified,
        cardRed = Color.Unspecified,
        filterBackground = Color.Unspecified,
        filterSurface = Color.Unspecified,
        filterText = Color.Unspecified,
        selectedItem = Color.Unspecified,
        accentTeal = Color.Unspecified,
        goldColor = Color.Unspecified
    )
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = SecondaryColor,
    onSecondary = Color(0xFF000000),
    tertiary = TertiaryColor,
    onTertiary = Color(0xFF000000),
    background = DarkBackground,
    onBackground = Color(0xFFE0E0E0),
    surface = DarkSurface,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = DarkCardColor,
    error = ErrorColor,
    onError = Color(0xFF000000),
    outline = DarkBorder
)

private val DarkExtendedColors = ExtendedColors(
    surfaceRed = DarkSurfaceRed,
    cardRed = DarkCardRed,
    filterBackground = DarkFilterBackground,
    filterSurface = DarkFilterSurface,
    filterText = DarkFilterText,
    selectedItem = SelectedItemColor,
    accentTeal = AccentTeal,
    goldColor = GoldColor
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = PrimaryVariant.copy(alpha = 0.9f),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = SecondaryColor,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = TertiaryColor,
    onTertiary = Color(0xFF000000),
    background = LightBackground,
    onBackground = Color(0xFF1A1A1A),
    surface = LightSurface,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = LightCardColor,
    error = ErrorColor,
    onError = Color(0xFFFFFFFF),
    outline = LightBorder
)

// Extended colors for light theme
private val LightExtendedColors = ExtendedColors(
    surfaceRed = LightSurfaceRed,
    cardRed = LightCardRed,
    filterBackground = LightFilterBackground,
    filterSurface = LightFilterSurface,
    filterText = LightFilterText,
    selectedItem = SelectedItemColor,
    accentTeal = AccentTeal,
    goldColor = GoldColor
)

@Composable
fun SmartDocsConvertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Use the appropriate extended colors based on the theme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}