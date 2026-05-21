package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = BoldBluePrimaryLight,
    secondary = BoldBluePrimaryLight,
    tertiary = BoldAmberTertiary,
    background = BoldDarkBg,
    surface = BoldDarkSurface,
    onPrimary = BoldDarkBg,
    onSecondary = BoldDarkBg,
    onBackground = BoldDarkText,
    onSurface = BoldDarkText,
    surfaceVariant = BoldDarkBorder,
    outline = BoldDarkBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BoldBluePrimary,
    secondary = BoldSlateSecondary,
    tertiary = BoldAmberTertiary,
    background = BoldLightBg,
    surface = BoldLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = BoldDarkSlateText,
    onSurface = BoldDeepBlackText,
    surfaceVariant = Color(0xFFF8FAFC), // Slate 50
    outline = BoldLightBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve the exact brand aesthetic of Bold Typography
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
