package tech.dmampro.tinda.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Traduction des resultCode radio en verdicts de segment (L3) :
 * le code 0 ne vaut confirmation que sur la ROM où le terrain l'a
 * prouvé (Redmi Note 14 / HyperOS) ; ailleurs il reste non concluant.
 */
class SentVerdictTest {

    @Test
    fun `resultat ok confirme partout`() {
        assertEquals(
            PartSentState.CONFIRMED,
            sentVerdictForResultCode(-1, zeroMeansSentOnThisRom = false).first,
        )
        assertEquals(
            PartSentState.CONFIRMED,
            sentVerdictForResultCode(-1, zeroMeansSentOnThisRom = true).first,
        )
    }

    @Test
    fun `code zero confirme seulement sur la rom observee`() {
        assertEquals(
            PartSentState.CONFIRMED,
            sentVerdictForResultCode(0, zeroMeansSentOnThisRom = true).first,
        )
        assertEquals(
            PartSentState.UNCONFIRMED,
            sentVerdictForResultCode(0, zeroMeansSentOnThisRom = false).first,
        )
    }

    @Test
    fun `les codes d erreur documentes et inconnus sont des echecs avec leur code`() {
        for (code in listOf(1, 2, 3, 4, 5, 8)) {
            val (state, failureCode) =
                sentVerdictForResultCode(code, zeroMeansSentOnThisRom = true)
            assertEquals(PartSentState.FAILED, state)
            assertEquals(code, failureCode)
        }
    }

    @Test
    fun `pas de code d echec pour les verdicts non echec`() {
        assertNull(sentVerdictForResultCode(-1, zeroMeansSentOnThisRom = false).second)
        assertNull(sentVerdictForResultCode(0, zeroMeansSentOnThisRom = false).second)
    }
}
