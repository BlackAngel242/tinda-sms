package tech.dmampro.tinda.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

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
    fun themeMode_byId() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.byId("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.byId("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.byId("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.byId("inconnu"))
    }
}
