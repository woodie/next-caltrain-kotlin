package com.netpress.nextcaltrain.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// MARK: - App colors exposed via CompositionLocal

data class AppColors(
    val calPast: Color,
    val calArrive: Color,
    val calDepart: Color,
    val calSwapped: Color,
    val iconCircleBackground: Color,
    val appBackground: Color,
    val appText: Color,
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        calPast = CalPastLight,
        calArrive = CalArriveLight,
        calDepart = CalDepartLight,
        calSwapped = CalSwappedLight,
        iconCircleBackground = IconCircleBackgroundLight,
        appBackground = AppBackgroundLight,
        appText = AppTextLight,
    )
}

// MARK: - Font sizes (mirroring iOS AppStyle)

object AppStyle {
    val fontTrain = 18.sp
    val fontOrigin = 22.sp
    val fontBlurb = 28.sp
    val fontStatusBar = 18.sp
    val iconButtonSizeDp = 44
}

// MARK: - Material color schemes (minimal — we use AppColors for app-specific colors)

private val DarkColorScheme = darkColorScheme(
    background = AppBackgroundDark,
    surface = AppBackgroundDark,
    onBackground = AppTextDark,
    onSurface = AppTextDark,
)

private val LightColorScheme = lightColorScheme(
    background = AppBackgroundLight,
    surface = AppBackgroundLight,
    onBackground = AppTextLight,
    onSurface = AppTextLight,
)

@Composable
fun NextCaltrainTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) {
        AppColors(
            calPast = CalPastDark,
            calArrive = CalArriveDark,
            calDepart = CalDepartDark,
            calSwapped = CalSwappedDark,
            iconCircleBackground = IconCircleBackgroundDark,
            appBackground = AppBackgroundDark,
            appText = AppTextDark,
        )
    } else {
        AppColors(
            calPast = CalPastLight,
            calArrive = CalArriveLight,
            calDepart = CalDepartLight,
            calSwapped = CalSwappedLight,
            iconCircleBackground = IconCircleBackgroundLight,
            appBackground = AppBackgroundLight,
            appText = AppTextLight,
        )
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
