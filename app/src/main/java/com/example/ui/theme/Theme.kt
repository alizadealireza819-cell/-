package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = KeyOperatorBg,
    secondary = KeyFunctionBg,
    tertiary = AccentTeal,
    background = SlateDarkBackground,
    surface = CardDisplayBackground,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
  )

private val LightColorScheme =
  darkColorScheme(
    primary = KeyOperatorBg,
    secondary = KeyFunctionBg,
    tertiary = AccentTeal,
    background = SlateDarkBackground,
    surface = CardDisplayBackground,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force premium dark mode for standard mathematical scientific precision instrument look
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
