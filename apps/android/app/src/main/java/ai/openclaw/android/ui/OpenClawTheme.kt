package ai.openclaw.android.ui

import ai.openclaw.android.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

@Composable
fun OpenClawTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
  val context = LocalContext.current
  val systemDark = isSystemInDarkTheme()
  val isDark =
    when (themeMode) {
      ThemeMode.System -> systemDark
      ThemeMode.Light -> false
      ThemeMode.Dark -> true
    }
  val colorScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

  MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun overlayContainerColor(): Color {
  val scheme = MaterialTheme.colorScheme
  val isDark = scheme.background.luminance() < 0.5f
  val base = if (isDark) scheme.surfaceContainerLow else scheme.surfaceContainerHigh
  return if (isDark) base else base.copy(alpha = 0.88f)
}

@Composable
fun overlayIconColor(): Color {
  return MaterialTheme.colorScheme.onSurfaceVariant
}
