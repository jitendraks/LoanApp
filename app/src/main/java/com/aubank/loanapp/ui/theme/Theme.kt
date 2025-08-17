package com.aubank.loanapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoanAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> darkColorScheme(
            primary = Color(0xFF825500),
            primaryContainer = Color(0xFFFFDDB3),
            surface = Color(0xFF1F1B16),
            onSurface = Color(0xFFEAE1D9)
        )
        else -> lightColorScheme(
            primary = Color(0xFF825500),
            primaryContainer = Color(0xFFFFDDB3),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF1F1B16)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LoanAppTypography,
        shapes = LoanAppShapes,
        content = content
    )
}
