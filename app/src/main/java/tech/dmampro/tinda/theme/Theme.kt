package tech.dmampro.tinda.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val TindaLightColors = lightColorScheme(
    primary = TindaGreen40,
    onPrimary = TindaNeutral99,
    primaryContainer = TindaGreen90,
    onPrimaryContainer = TindaGreen10,
    secondary = TindaAmber40,
    onSecondary = TindaNeutral99,
    secondaryContainer = TindaAmber90,
    onSecondaryContainer = TindaGreen10,
    background = TindaNeutral99,
    onBackground = TindaNeutral10,
    surface = TindaNeutral99,
    onSurface = TindaNeutral10,
    error = TindaError40,
    onError = TindaNeutral99,
)

private val TindaDarkColors = darkColorScheme(
    primary = TindaGreen80,
    onPrimary = TindaGreen20,
    primaryContainer = TindaGreen20,
    onPrimaryContainer = TindaGreen90,
    secondary = TindaAmber80,
    onSecondary = TindaGreen20,
    secondaryContainer = TindaAmber40,
    onSecondaryContainer = TindaAmber90,
    background = TindaNeutral10,
    onBackground = TindaNeutral95,
    surface = TindaNeutral10,
    onSurface = TindaNeutral95,
    error = TindaError80,
    onError = TindaGreen20,
)

/**
 * Thème de l'application. Couleurs dynamiques (Material You) sur Android 12+,
 * palette « Tinda » originale sinon. Clair/sombre suivis du système.
 */
@Composable
fun TindaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TindaDarkColors
        else -> TindaLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TindaTypography,
        content = content,
    )
}
