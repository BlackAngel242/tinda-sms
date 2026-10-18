package tech.dmampro.tinda.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.dmampro.tinda.data.local.ForwardRule

/**
 * Logique pure du transfert automatique (F11) : correspondance source,
 * anti-boucle, texte transféré. Aucune dépendance Android.
 */
class ForwardMatchingTest {

    private fun rule(
        sourceType: String = "NUMBER",
        sourceValue: String = "",
        destination: String = "",
        includeContent: Boolean = true,
    ) = ForwardRule(
        name = "test",
        sourceType = sourceType,
        sourceValue = sourceValue,
        destination = destination,
        includeContent = includeContent,
    )

    @Test
    fun matchesSource_number_normalizesFormats() {
        val r = rule(sourceType = "NUMBER", sourceValue = "+242 06 812 3456")
        assertTrue(matchesSource(r, "068123456", "bonjour"))
        assertTrue(matchesSource(r, "+242068123456", "bonjour"))
        assertFalse(matchesSource(r, "069999999", "bonjour"))
    }

    @Test
    fun matchesSource_keyword_caseInsensitive() {
        val r = rule(sourceType = "KEYWORD", sourceValue = "urgent")
        assertTrue(matchesSource(r, "068123456", "C'est URGENT, rappelle-moi"))
        assertFalse(matchesSource(r, "068123456", "tout va bien"))
    }

    @Test
    fun matchesSource_blankValue_neverMatches() {
        val r = rule(sourceValue = "")
        assertFalse(matchesSource(r, "068123456", "bonjour"))
    }

    @Test
    fun antiLoop_rejectsDestinationEqualToSender() {
        // Boucle A <-> B : le transfert vers l'expéditeur est interdit.
        assertFalse(isForwardableDestination("068123456", "068123456"))
        assertFalse(isForwardableDestination("+242068123456", "068123456"))
    }

    @Test
    fun antiLoop_rejectsUnusableDestinations() {
        assertFalse(isForwardableDestination("1234", "068123456")) // numéro court
        assertFalse(isForwardableDestination("", "068123456")) // vide
    }

    @Test
    fun antiLoop_acceptsDifferentNumber() {
        assertTrue(isForwardableDestination("069999999", "068123456"))
    }

    @Test
    fun buildForwardText_prefixesSender() {
        val withContent = rule(includeContent = true)
        assertEquals(
            "Transfert de 068123456 : bonjour",
            buildForwardText(withContent, "068123456", "bonjour"),
        )
        val notifyOnly = rule(includeContent = false)
        assertEquals(
            "Transfert : message reçu de 068123456",
            buildForwardText(notifyOnly, "068123456", "bonjour"),
        )
    }
}
