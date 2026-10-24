package tech.dmampro.tinda.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

/**
 * Fabrique un jeu M3 complet à partir d'une palette originale.
 * Tous les rôles utilisés par les écrans sont définis explicitement
 * (lot 1) : fonds, surfaces et conteneurs, textes, contours, accent,
 * secondaire et erreurs. Les neutres sont des ardoises teintées par
 * le fond du thème : fonds sobres, un seul accent mis en avant.
 * Contrastes visés : onPrimary sur primary et onSurface sur
 * background au dessus de 4,5 (vérifiés par TindaThemesTest).
 */
private fun tindaScheme(
    id: String,
    @StringRes labelRes: Int,
    p40: Color,
    p80: Color,
    p90: Color,
    p10: Color,
    s40: Color,
    s80: Color,
    s90: Color,
    s10: Color,
    bgLight: Color,
    bgDark: Color,
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
        tertiary = s40,
        onTertiary = TindaNeutral99,
        tertiaryContainer = s90,
        onTertiaryContainer = s10,
        background = bgLight,
        onBackground = p10,
        surface = bgLight,
        onSurface = p10,
        surfaceVariant = lerp(bgLight, Color.Black, 0.08f),
        onSurfaceVariant = lerp(p10, bgLight, 0.32f),
        surfaceContainerLowest = lerp(bgLight, Color.White, 0.6f),
        surfaceContainerLow = lerp(bgLight, Color.Black, 0.03f),
        surfaceContainer = lerp(bgLight, Color.Black, 0.06f),
        surfaceContainerHigh = lerp(bgLight, Color.Black, 0.10f),
        surfaceContainerHighest = lerp(bgLight, Color.Black, 0.14f),
        outline = lerp(p10, bgLight, 0.48f),
        outlineVariant = lerp(bgLight, Color.Black, 0.18f),
        error = TindaError40,
        onError = TindaNeutral99,
        errorContainer = TindaErrorContainerLight,
        onErrorContainer = TindaOnErrorContainerLight,
        inverseSurface = p10,
        inverseOnSurface = bgLight,
        inversePrimary = p80,
        surfaceTint = p40,
        scrim = Color.Black,
    )
    val dark = darkColorScheme(
        primary = p80,
        onPrimary = p10,
        primaryContainer = lerp(p80, Color.Black, 0.68f),
        onPrimaryContainer = p90,
        secondary = s80,
        onSecondary = s10,
        secondaryContainer = lerp(s80, Color.Black, 0.68f),
        onSecondaryContainer = s90,
        tertiary = s80,
        onTertiary = s10,
        tertiaryContainer = lerp(s80, Color.Black, 0.68f),
        onTertiaryContainer = s90,
        background = bgDark,
        onBackground = p90,
        surface = lerp(bgDark, Color.White, 0.04f),
        onSurface = p90,
        surfaceVariant = lerp(bgDark, Color.White, 0.09f),
        onSurfaceVariant = lerp(p90, bgDark, 0.32f),
        surfaceContainerLowest = lerp(bgDark, Color.Black, 0.3f),
        surfaceContainerLow = lerp(bgDark, Color.White, 0.03f),
        surfaceContainer = lerp(bgDark, Color.White, 0.07f),
        surfaceContainerHigh = lerp(bgDark, Color.White, 0.11f),
        surfaceContainerHighest = lerp(bgDark, Color.White, 0.15f),
        outline = lerp(p90, bgDark, 0.48f),
        outlineVariant = lerp(bgDark, Color.White, 0.18f),
        error = TindaError80,
        onError = TindaOnErrorDark,
        errorContainer = TindaErrorContainerDark,
        onErrorContainer = TindaOnErrorContainerDark,
        inverseSurface = p90,
        inverseOnSurface = p10,
        inversePrimary = p40,
        surfaceTint = p80,
        scrim = Color.Black,
    )
    return AppThemeSpec(id, labelRes, light, dark)
}

/**
 * Thème par défaut unique (M4) : premier affichage, valeur par défaut
 * des préférences et repli d'un identifiant inconnu partagent cette
 * seule source. Radiant Slate, la base graphique Stitch du projet.
 */
const val DEFAULT_THEME_ID = "radiant"

/** Les sept thèmes originaux de Tinda (F4). Aucun asset repris d'une autre app. */
val TINDA_THEMES: List<AppThemeSpec> = listOf(
    // Radiant Slate en premier : thème phare (design Stitch, 19 oct. 2026).
    // Sombre : palette d'origine, inchangée. Clair : corrigé au lot 1
    // (l'ancien jeu clair inversait fond et texte). Fond ardoise très
    // léger, texte foncé, accent #00836F.
    AppThemeSpec(
        id = "radiant",
        labelRes = R.string.theme_radiant,
        light = lightColorScheme(
            primary = RadiantPrimaryLight,
            onPrimary = TindaNeutral99,
            primaryContainer = RadiantPrimaryContainerLight,
            onPrimaryContainer = RadiantOnPrimary,
            secondary = RadiantSkyLight,
            onSecondary = TindaNeutral99,
            secondaryContainer = RadiantSecondaryContainerLight,
            onSecondaryContainer = RadiantOnSecondaryContainerLight,
            tertiary = RadiantSkyLight,
            onTertiary = TindaNeutral99,
            tertiaryContainer = RadiantSecondaryContainerLight,
            onTertiaryContainer = RadiantOnSecondaryContainerLight,
            background = RadiantBgLight,
            onBackground = RadiantBg,
            surface = RadiantBgLight,
            onSurface = RadiantBg,
            surfaceVariant = RadiantSurfaceLight,
            onSurfaceVariant = RadiantOnVariantLight,
            surfaceContainerLowest = RadiantSurfaceLightLowest,
            surfaceContainerLow = RadiantSurfaceLightLow,
            surfaceContainer = RadiantSurfaceLight,
            surfaceContainerHigh = RadiantSurfaceLightHigh,
            surfaceContainerHighest = RadiantSurfaceLightHigh,
            outline = RadiantTextTertiary,
            outlineVariant = RadiantSurfaceLightHigh,
            error = TindaError40,
            onError = TindaNeutral99,
            errorContainer = TindaErrorContainerLight,
            onErrorContainer = TindaOnErrorContainerLight,
            inverseSurface = RadiantBg,
            inverseOnSurface = RadiantText,
            inversePrimary = RadiantPrimary,
            surfaceTint = RadiantPrimaryLight,
            scrim = Color.Black,
        ),
        dark = darkColorScheme(
            primary = RadiantPrimary,
            onPrimary = RadiantOnPrimary,
            primaryContainer = RadiantPrimaryDeep,
            onPrimaryContainer = RadiantText,
            secondary = RadiantSky,
            onSecondary = RadiantOnSecondaryDark,
            secondaryContainer = RadiantSecondaryContainerDark,
            onSecondaryContainer = RadiantOnSecondaryContainerDark,
            tertiary = RadiantSky,
            onTertiary = RadiantOnSecondaryDark,
            tertiaryContainer = RadiantSecondaryContainerDark,
            onTertiaryContainer = RadiantOnSecondaryContainerDark,
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
            onError = TindaOnErrorDark,
            errorContainer = TindaErrorContainerDark,
            onErrorContainer = TindaOnErrorContainerDark,
            inverseSurface = RadiantText,
            inverseOnSurface = RadiantBg,
            inversePrimary = RadiantPrimaryDeep,
            surfaceTint = RadiantPrimary,
            scrim = Color.Black,
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

/** Thème par identifiant, [DEFAULT_THEME_ID] en repli. */
fun themeById(id: String): AppThemeSpec =
    TINDA_THEMES.find { it.id == id }
        ?: TINDA_THEMES.find { it.id == DEFAULT_THEME_ID }
        ?: TINDA_THEMES.first()
