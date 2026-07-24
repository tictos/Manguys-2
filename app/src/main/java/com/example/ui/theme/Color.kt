package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.data.MediaStatus

// Sophisticated Dark
val SophisticatedDarkBackground = Color(0xFF1C1B1F)
val SophisticatedDarkSurface = Color(0xFF2B2930)
val SophisticatedDarkSurfaceVariant = Color(0xFF36343B)
val SophisticatedDarkPrimary = Color(0xFFD0BCFF)
val SophisticatedDarkOnPrimary = Color(0xFF381E72)
val SophisticatedDarkOnBackground = Color(0xFFE6E1E5)
val SophisticatedDarkOnSurfaceVariant = Color(0xFFCAC4D0)

// OLED Black / Manga
val OledBackground = Color(0xFF000000)
val OledSurface = Color(0xFF121212)
val OledSurfaceVariant = Color(0xFF1E1E1E)
val OledPrimary = Color(0xFFFF4081)
val OledOnPrimary = Color(0xFFFFFFFF)
val OledOnBackground = Color(0xFFFFFFFF)
val OledOnSurfaceVariant = Color(0xFFE0E0E0)

// Sakura Warm
val SakuraBackground = Color(0xFF1A1115)
val SakuraSurface = Color(0xFF281A21)
val SakuraSurfaceVariant = Color(0xFF38252E)
val SakuraPrimary = Color(0xFFF06292)
val SakuraOnPrimary = Color(0xFFFFFFFF)
val SakuraOnBackground = Color(0xFFFDF0F5)
val SakuraOnSurfaceVariant = Color(0xFFE8C5D3)

// Deep Ocean
val OceanBackground = Color(0xFF0B132B)
val OceanSurface = Color(0xFF1C2541)
val OceanSurfaceVariant = Color(0xFF2A3A5C)
val OceanPrimary = Color(0xFF48CAE4)
val OceanOnPrimary = Color(0xFF002B36)
val OceanOnBackground = Color(0xFFEDF2F4)
val OceanOnSurfaceVariant = Color(0xFF8D99AE)

// Light Minimal
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFECEFF1)
val LightPrimary = Color(0xFF6200EE)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF212121)
val LightOnSurfaceVariant = Color(0xFF546E7A)

// Media Status Colors
val StatusOngoing = Color(0xFFD0BCFF)
val StatusOngoingText = Color(0xFF381E72)
val StatusOnHold = Color(0xFFFFB74D)
val StatusOnHoldText = Color(0xFF4A2800)
val StatusCompleted = Color(0xFFB6EEA6)
val StatusCompletedText = Color(0xFF005049)

data class StatusColors(
    val dotColor: Color,
    val textColor: Color
)

@Composable
fun MediaStatus.getStatusColors(): StatusColors {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return when (this) {
        MediaStatus.COMPLETED -> if (isLight) {
            StatusColors(
                dotColor = Color(0xFF2E7D32),
                textColor = Color(0xFF1B5E20)
            )
        } else {
            StatusColors(
                dotColor = Color(0xFFB6EEA6),
                textColor = Color(0xFFB6EEA6)
            )
        }
        MediaStatus.ON_HOLD -> if (isLight) {
            StatusColors(
                dotColor = Color(0xFFE65100),
                textColor = Color(0xFFBF360C)
            )
        } else {
            StatusColors(
                dotColor = Color(0xFFFFB74D),
                textColor = Color(0xFFFFB74D)
            )
        }
        MediaStatus.ONGOING -> if (isLight) {
            StatusColors(
                dotColor = Color(0xFF673AB7),
                textColor = Color(0xFF4A148C)
            )
        } else {
            StatusColors(
                dotColor = Color(0xFFD0BCFF),
                textColor = Color(0xFFD0BCFF)
            )
        }
    }
}

