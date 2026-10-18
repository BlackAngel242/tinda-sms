package tech.dmampro.tinda.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Requête FTS : la saisie utilisateur est convertie en expression MATCH sûre
 * (jetons alphanumériques, préfixes, ET implicite). Aucune dépendance Android.
 */
class FtsMatchTest {

    @Test
    fun emptyInput_emptyMatch() {
        assertEquals("", toFtsMatch(""))
        assertEquals("", toFtsMatch("   "))
    }

    @Test
    fun singleWord_prefixQuery() {
        assertEquals("\"bonjour\"*", toFtsMatch("bonjour"))
    }

    @Test
    fun twoWords_implicitAnd() {
        assertEquals("\"bonjour\"* \"maman\"*", toFtsMatch("bonjour maman"))
    }

    @Test
    fun specialChars_stripped() {
        // Guillemets, parenthèses et astérisques ne doivent pas casser MATCH :
        // seuls les jetons alphanumériques sont gardés, cités littéralement.
        assertEquals("\"bonjour\"* \"test\"*", toFtsMatch("\"bonjour\" (test) *"))
    }

    @Test
    fun singleLetters_ignored() {
        // Les jetons d'une lettre sont ignorés (bruit), mais le reste passe.
        assertEquals("\"bonjour\"*", toFtsMatch("a bonjour b"))
        assertEquals("", toFtsMatch("a b c"))
    }

    @Test
    fun accentedWords_kept() {
        assertEquals("\"crème\"* \"brûlée\"*", toFtsMatch("crème brûlée"))
    }

    @Test
    fun tooManyTokens_capped() {
        val many = (1..20).joinToString(" ") { "mot$it" }
        val tokens = toFtsMatch(many).split(" ")
        assertEquals(8, tokens.size)
    }

    @Test
    fun duplicates_removed() {
        assertEquals("\"bonjour\"*", toFtsMatch("bonjour bonjour"))
    }

    @Test
    fun matchQuery_embedsMarkers() {
        val q = searchMatchQuery("bonjour")
        assertTrue(q.sql.contains("messages_fts MATCH ?"))
        assertTrue(q.sql.contains("is_private = 0"))
        assertEquals(1, q.argCount)
    }
}
