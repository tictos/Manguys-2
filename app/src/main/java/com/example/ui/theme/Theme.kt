package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val primaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color
) {
    SOPHISTICATED_DARK(
        id = "sophisticated_dark",
        displayName = "Sombre Élégant",
        description = "Gris violet sombre moderne",
        primaryColor = SophisticatedDarkPrimary,
        backgroundColor = SophisticatedDarkBackground,
        surfaceColor = SophisticatedDarkSurfaceVariant
    ),
    OLED_BLACK(
        id = "oled_black",
        displayName = "Noir OLED / Manga",
        description = "Noir pur avec contraste élevé Sakura",
        primaryColor = OledPrimary,
        backgroundColor = OledBackground,
        surfaceColor = OledSurfaceVariant
    ),
    SAKURA_WARM(
        id = "sakura_warm",
        displayName = "Rose Sakura",
        description = "Thème sombre aux nuances rose doux",
        primaryColor = SakuraPrimary,
        backgroundColor = SakuraBackground,
        surfaceColor = SakuraSurfaceVariant
    ),
    OCEAN_BLUE(
        id = "ocean_blue",
        displayName = "Bleu Océan",
        description = "Bleu nuit immersif et apaisant",
        primaryColor = OceanPrimary,
        backgroundColor = OceanBackground,
        surfaceColor = OceanSurfaceVariant
    ),
    LIGHT_MINIMAL(
        id = "light_minimal",
        displayName = "Clair Épuré",
        description = "Thème clair lumineux et très lisible",
        primaryColor = LightPrimary,
        backgroundColor = LightBackground,
        surfaceColor = LightSurfaceVariant
    )
}

fun getAppColorScheme(theme: AppTheme): ColorScheme {
    return when (theme) {
        AppTheme.SOPHISTICATED_DARK -> darkColorScheme(
            primary = SophisticatedDarkPrimary,
            onPrimary = SophisticatedDarkOnPrimary,
            background = SophisticatedDarkBackground,
            onBackground = SophisticatedDarkOnBackground,
            surface = SophisticatedDarkBackground,
            onSurface = SophisticatedDarkOnBackground,
            surfaceVariant = SophisticatedDarkSurfaceVariant,
            onSurfaceVariant = SophisticatedDarkOnSurfaceVariant,
            primaryContainer = SophisticatedDarkSurface,
            onPrimaryContainer = SophisticatedDarkOnBackground,
            secondaryContainer = SophisticatedDarkSurfaceVariant,
            onSecondaryContainer = SophisticatedDarkOnBackground
        )
        AppTheme.OLED_BLACK -> darkColorScheme(
            primary = OledPrimary,
            onPrimary = OledOnPrimary,
            background = OledBackground,
            onBackground = OledOnBackground,
            surface = OledBackground,
            onSurface = OledOnBackground,
            surfaceVariant = OledSurfaceVariant,
            onSurfaceVariant = OledOnSurfaceVariant,
            primaryContainer = OledSurface,
            onPrimaryContainer = OledOnBackground,
            secondaryContainer = OledSurfaceVariant,
            onSecondaryContainer = OledOnBackground
        )
        AppTheme.SAKURA_WARM -> darkColorScheme(
            primary = SakuraPrimary,
            onPrimary = SakuraOnPrimary,
            background = SakuraBackground,
            onBackground = SakuraOnBackground,
            surface = SakuraBackground,
            onSurface = SakuraOnBackground,
            surfaceVariant = SakuraSurfaceVariant,
            onSurfaceVariant = SakuraOnSurfaceVariant,
            primaryContainer = SakuraSurface,
            onPrimaryContainer = SakuraOnBackground,
            secondaryContainer = SakuraSurfaceVariant,
            onSecondaryContainer = SakuraOnBackground
        )
        AppTheme.OCEAN_BLUE -> darkColorScheme(
            primary = OceanPrimary,
            onPrimary = OceanOnPrimary,
            background = OceanBackground,
            onBackground = OceanOnBackground,
            surface = OceanBackground,
            onSurface = OceanOnBackground,
            surfaceVariant = OceanSurfaceVariant,
            onSurfaceVariant = OceanOnSurfaceVariant,
            primaryContainer = OceanSurface,
            onPrimaryContainer = OceanOnBackground,
            secondaryContainer = OceanSurfaceVariant,
            onSecondaryContainer = OceanOnBackground
        )
        AppTheme.LIGHT_MINIMAL -> lightColorScheme(
            primary = LightPrimary,
            onPrimary = LightOnPrimary,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnBackground,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            primaryContainer = LightSurfaceVariant,
            onPrimaryContainer = LightOnBackground,
            secondaryContainer = LightSurfaceVariant,
            onSecondaryContainer = LightOnBackground
        )
    }
}

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.SOPHISTICATED_DARK,
    content: @Composable () -> Unit,
) {
    val colorScheme = getAppColorScheme(appTheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val isDark = appTheme != AppTheme.LIGHT_MINIMAL
                val insetsController = WindowCompat.getInsetsController(window, view)
                // isAppearanceLightStatusBars = true means dark icons for light background
                // isAppearanceLightStatusBars = false means white/light icons for dark background
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

