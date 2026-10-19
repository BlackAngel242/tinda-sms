package tech.dmampro.tinda.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import tech.dmampro.tinda.R

/** Mode d'affichage : suit le système, ou forcé clair / sombre. */
enum class ThemeMode(val id: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun byId(id: String): ThemeMode = entries.find { it.id == id } ?: SYSTEM
    }
}

/** Un thème original : ses jeux de couleurs clair et sombre. */
data class AppThemeSpec(
    val id: String,
    @StringRes val labelRes: Int,
    val light: ColorScheme,
    val dark: ColorScheme,
)

/** Fabrique un jeu M3 complet à partir d'une palette originale. */
private fun tindaScheme(
    id: String,
    @StringRes labelRes: Int,
    p40: androidx.compose.ui.graphics.Color,
    p80: androidx.compose.ui.graphics.Color,
    p90: androidx.compose.ui.graphics.Color,
    p10: androidx.compose.ui.graphics.Color,
    s40: androidx.compose.ui.graphics.Color,
    s80: androidx.compose.ui.graphics.Color,
    s90: androidx.compose.ui.graphics.Color,
    s10: androidx.compose.ui.graphics.Color,
    bgLight: androidx.compose.ui.graphics.Color,
    bgDark: androidx.compose.ui.graphics.Color,
    errorLight: androidx.compose.ui.graphics.Color = TindaError40,
    errorDark: androidx.compose.ui.graphics.Color = TindaError80,
): AppThemeSpec {
    val light = lightColorScheme(
        primary = p40,
        onPrimary = TindaNeutral99,
        primaryContainer = p90,
        onPrimaryContainer = p10,
        secondary = s40,
        onSecondary = TindaNeutral99,
        secondaryContainer = s90,
        onSecondaryContainer = s10,
        background = bgLight,
        onBackground = p10,
        surface = bgLight,
        onSurface = p10,
        error = errorLight,
        onError = TindaNeutral99,
    )
    val dark = darkColorScheme(
        primary = p80,
        onPrimary = p10,
        primaryContainer = p10,
        onPrimaryContainer = p90,
        secondary = s80,
        onSecondary = s10,
        secondaryContainer = s10,
        onSecondaryContainer = s90,
        background = bgDark,
        onBackground = p90,
        surface = bgDark,
        onSurface = p90,
        error = errorDark,
        onError = p10,
    )
    return AppThemeSpec(id, labelRes, light, dark)
}

/** Les thèmes originaux de Tinda (F4). Aucun asset repris d'une autre app. */
val TINDA_THEMES: List<AppThemeSpec> = listOf(
    // Radiant Slate en premier : thème phare (design Stitch, 19 oct. 2026).
    AppThemeSpec(
        id = "radiant",
        labelRes = R.string.theme_radiant,
        light = lightColorScheme(
            primary = RadiantPrimaryDeep,
            onPrimary = RadiantText,
            primaryContainer = RadiantPrimary,
            onPrimaryContainer = RadiantOnPrimary,
            secondary = RadiantSky,
            background = RadiantText,
            onBackground = RadiantBg,
            surface = RadiantText,
            onSurface = RadiantBg,
            surfaceVariant = RadiantSurface,
            onSurfaceVariant = RadiantTextSecondary,
            error = TindaError40,
        ),
        dark = darkColorScheme(
            primary = RadiantPrimary,
            onPrimary = RadiantOnPrimary,
            primaryContainer = RadiantPrimaryDeep,
            onPrimaryContainer = RadiantText,
            secondary = RadiantSky,
            onSecondary = RadiantOnPrimary,
            tertiary = RadiantSky,
            background = RadiantBg,
            onBackground = RadiantText,
            surface = RadiantSurfaceLow,
            onSurface = RadiantText,
            surfaceVariant = RadiantSurface,
            onSurfaceVariant = RadiantTextSecondary,
            surfaceContainerLowest = RadiantBg,
            surfaceContainerLow = RadiantSurfaceLow,
            surfaceContainer = RadiantSurface,
            surfaceContainerHigh = RadiantSurfaceHigh,
            surfaceContainerHighest = RadiantSurfaceHigh,
            outline = RadiantTextTertiary,
            outlineVariant = RadiantSurfaceHigh,
            error = TindaError80,
        ),
    ),
    tindaScheme(
        "lagoon", R.string.theme_lagoon,
        TindaGreen40, TindaGreen80, TindaGreen90, TindaGreen10,
        TindaAmber40, TindaAmber80, TindaAmber90, TindaGreen10,
        TindaNeutral99, TindaNeutral10,
    ),
    tindaScheme(
        "sahel", R.string.theme_sahel,
        Sahel40, Sahel80, Sahel90, Sahel10,
        SahelS40, SahelS80, SahelS90, SahelS10,
        SahelBgLight, SahelBgDark,
    ),
    tindaScheme(
        "fleuve", R.string.theme_fleuve,
        Fleuve40, Fleuve80, Fleuve90, Fleuve10,
        FleuveS40, FleuveS80, FleuveS90, FleuveS10,
        FleuveBgLight, FleuveBgDark,
    ),
    tindaScheme(
        "baobab", R.string.theme_baobab,
        Baobab40, Baobab80, Baobab90, Baobab10,
        BaobabS40, BaobabS80, BaobabS90, BaobabS10,
        BaobabBgLight, BaobabBgDark,
    ),
    tindaScheme(
        "nuit", R.string.theme_nuit,
        Nuit40, Nuit80, Nuit90, Nuit10,
        NuitS40, NuitS80, NuitS90, NuitS10,
        NuitBgLight, NuitBgDark,
    ),
    tindaScheme(
        "orchidee", R.string.theme_orchidee,
        Orchi40, Orchi80, Orchi90, Orchi10,
        OrchiS40, OrchiS80, OrchiS90, OrchiS10,
        OrchiBgLight, OrchiBgDark,
    ),
)

/** Thème par identifiant, "lagoon" par défaut. */
fun themeById(id: String): AppThemeSpec =
    TINDA_THEMES.find { it.id == id } ?: TINDA_THEMES.first()
