package tech.dmampro.tinda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Recherche de contacts du champ destinataire : termes déduits de la
 * saisie, échappement des motifs LIKE et déduplication bornée.
 */
class ContactsSearchTest {

    @Test
    fun `une saisie vide ne cherche rien`() {
        val (name, digits) = contactSearchTerms("   ")
        assertNull(name)
        assertNull(digits)
    }

    @Test
    fun `des lettres cherchent sur le nom seul`() {
        val (name, digits) = contactSearchTerms("nad")
        assertEquals("nad", name)
        assertNull(digits)
    }

    @Test
    fun `des chiffres cherchent sur le numero des deux chiffres`() {
        val (name, digits) = contactSearchTerms("06")
        assertNull(name)
        assertEquals("06", digits)
    }

    @Test
    fun `un seul chiffre ne declenche pas la recherche numero`() {
        val (_, digits) = contactSearchTerms("0")
        assertNull(digits)
    }

    @Test
    fun `une saisie mixte cherche sur le nom et le numero`() {
        val (name, digits) = contactSearchTerms("maman 06")
        assertEquals("maman 06", name)
        assertTrue(digits == "06")
    }

    @Test
    fun `les caracteres speciaux du motif LIKE sont echappes`() {
        assertEquals("50\\%\\_\\\\", escapeLike("50%_\\"))
        assertEquals("abc", escapeLike("abc"))
    }

    @Test
    fun `la deduplication garde la premiere ligne par contact et numero normalise`() {
        val rows = listOf(
            ContactSuggestion(1L, "Awa", "+242 06 111 2233", "Mobile"),
            ContactSuggestion(1L, "Awa", "+242061112233", "Domicile"),
            ContactSuggestion(2L, "Bruno", "+242061112233", "Mobile"),
        )
        val out = dedupeSuggestions(rows)
        assertEquals(2, out.size)
        assertEquals("Mobile", out[0].typeLabel)
        assertEquals(2L, out[1].contactId)
    }

    @Test
    fun `la deduplication borne le nombre de suggestions`() {
        val rows = (1..40).map { i ->
            ContactSuggestion(i.toLong(), "Contact $i", "06000000$i")
        }
        assertEquals(CONTACT_SUGGESTION_LIMIT, dedupeSuggestions(rows).size)
    }

    @Test
    fun `l'ordre d'arrivee est conserve apres deduplication`() {
        val rows = listOf(
            ContactSuggestion(3L, "Zoe", "069999999"),
            ContactSuggestion(1L, "Awa", "061111111"),
        )
        val out = dedupeSuggestions(rows)
        assertEquals(listOf("Zoe", "Awa"), out.map { it.displayName })
    }
}
