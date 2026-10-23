package tech.dmampro.tinda.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Thème de l'application (F4) : sept thèmes originaux + couleurs
 * dynamiques (Material You) sur Android 12+, désactivées par défaut
 * (lot 1 : la palette fixe choisie prime, Material You est un choix
 * explicite). Clair/sombre suivis du système ou forcés.
 */
@Composable
fun TindaTheme(
    appTheme: AppThemeSpec = TINDA_THEMES.first(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> appTheme.dark
        else -> appTheme.light
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TindaTypography,
        content = content,
    )
}
