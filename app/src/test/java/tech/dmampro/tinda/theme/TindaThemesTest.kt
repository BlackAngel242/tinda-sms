package tech.dmampro.tinda.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Moteur de thèmes : thèmes originaux, identifiants uniques,
 * jeux clair/sombre distincts. Aucune dépendance Android (Color est
 * une simple valeur).
 */
class TindaThemesTest {

    @Test
    fun sevenThemes() {
        // 6 thèmes V1 + Radiant Slate (design Stitch, oct. 2026).
        assertEquals(7, TINDA_THEMES.size)
    }

    @Test
    fun uniqueIds() {
        val ids = TINDA_THEMES.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun unknownId_fallsBackToFirst() {
        assertEquals(TINDA_THEMES.first(), themeById("inexistant"))
    }

    @Test
    fun defaultTheme_isRadiantSharedAndFirst() {
        // M4 : une seule source pour le thème par défaut (premier
        // affichage, préférences, repli). « lagoon » ne doit plus
        // apparaître comme valeur initiale nulle part.
        assertEquals("radiant", DEFAULT_THEME_ID)
        assertEquals(DEFAULT_THEME_ID, TINDA_THEMES.first().id)
        assertEquals(DEFAULT_THEME_ID, themeById("inexistant").id)
    }

    @Test
    fun lightAndDarkDiffer() {
        TINDA_THEMES.forEach { theme ->
            assertNotEquals(
                "Le thème ${theme.id} doit avoir deux jeux distincts",
                theme.light.primary,
                theme.dark.primary,
            )
        }
    }

    @Test
    fun roles_explicitAndDistinctLightDark() {
        // Lot 1 : chaque thème définit explicitement les rôles utilisés
        // par les écrans, et les conteneurs se distinguent du fond.
        TINDA_THEMES.forEach { theme ->
            listOf(theme.light, theme.dark).forEach { scheme ->
                assertNotEquals(
                    "surfaceContainer doit différer du fond (${theme.id})",
                    scheme.background,
                    scheme.surfaceContainer,
                )
                assertNotEquals(
                    "surfaceContainerHigh doit différer de surfaceContainer (${theme.id})",
                    scheme.surfaceContainer,
                    scheme.surfaceContainerHigh,
                )
                assertNotEquals(
                    "onSurfaceVariant doit différer de onSurface (${theme.id})",
                    scheme.onSurface,
                    scheme.onSurfaceVariant,
                )
                assertNotEquals(
                    "outlineVariant doit différer du fond (${theme.id})",
                    scheme.background,
                    scheme.outlineVariant,
                )
                assertNotEquals(
                    "primary doit différer de onPrimary (${theme.id})",
                    scheme.primary,
                    scheme.onPrimary,
                )
            }
            assertNotEquals("fonds clair/sombre distincts (${theme.id})", theme.light.background, theme.dark.background)
            assertNotEquals("conteneurs clair/sombre distincts (${theme.id})", theme.light.surfaceContainer, theme.dark.surfaceContainer)
            assertNotEquals("primary clair/sombre distincts (${theme.id})", theme.light.primary, theme.dark.primary)
        }
    }

    @Test
    fun contrast_onPrimaryOnPrimary_atLeast45() {
        TINDA_THEMES.forEach { theme ->
            assertTrue(
                "onPrimary/primary clair ${theme.id} = ${contrast(theme.light.onPrimary, theme.light.primary)}",
                contrast(theme.light.onPrimary, theme.light.primary) >= 4.5,
            )
            assertTrue(
                "onPrimary/primary sombre ${theme.id} = ${contrast(theme.dark.onPrimary, theme.dark.primary)}",
                contrast(theme.dark.onPrimary, theme.dark.primary) >= 4.5,
            )
        }
    }

    @Test
    fun contrast_onSurfaceOnBackground_atLeast45() {
        TINDA_THEMES.forEach { theme ->
            assertTrue(
                "onSurface/background clair ${theme.id} = ${contrast(theme.light.onSurface, theme.light.background)}",
                contrast(theme.light.onSurface, theme.light.background) >= 4.5,
            )
            assertTrue(
                "onSurface/background sombre ${theme.id} = ${contrast(theme.dark.onSurface, theme.dark.background)}",
                contrast(theme.dark.onSurface, theme.dark.background) >= 4.5,
            )
        }
    }

    @Test
    fun themeMode_byId() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.byId("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.byId("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.byId("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.byId("inconnu"))
    }
}

/** Luminance relative WCAG d'une couleur sRGB. */
private fun luminance(color: Color): Double {
    fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) +
        0.0722 * channel(color.blue)
}

/** Rapport de contraste WCAG entre un texte et son fond. */
private fun contrast(foreground: Color, background: Color): Double {
    val l1 = luminance(foreground)
    val l2 = luminance(background)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}
