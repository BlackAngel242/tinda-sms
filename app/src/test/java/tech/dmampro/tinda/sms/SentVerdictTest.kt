package tech.dmampro.tinda.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Traduction des resultCode radio en verdicts de segment (L3) :
 * le code 0 ne vaut confirmation que si le drapeau d'émission
 * d'une empreinte Build exacte est prouvé ; sinon il reste non concluant.
 */
class SentVerdictTest {

    @Test
    fun `resultat ok confirme partout`() {
        assertEquals(
            PartSentState.CONFIRMED,
            sentVerdictForResultCode(-1, zeroProvesSentForExactDevice = false).first,
        )
        assertEquals(
            PartSentState.CONFIRMED,
            sentVerdictForResultCode(-1, zeroProvesSentForExactDevice = true).first,
        )
    }

    @Test
    fun `code zero confirme seulement si l emission exacte est prouvee`() {
        assertEquals(
            PartSentState.CONFIRMED,
            sentVerdictForResultCode(0, zeroProvesSentForExactDevice = true).first,
        )
        assertEquals(
            PartSentState.UNCONFIRMED,
            sentVerdictForResultCode(0, zeroProvesSentForExactDevice = false).first,
        )
    }

    @Test
    fun `les codes d erreur documentes et inconnus sont des echecs avec leur code`() {
        for (code in listOf(1, 2, 3, 4, 5, 8)) {
            val (state, failureCode) =
                sentVerdictForResultCode(code, zeroProvesSentForExactDevice = true)
            assertEquals(PartSentState.FAILED, state)
            assertEquals(code, failureCode)
        }
    }

    @Test
    fun `pas de code d echec pour les verdicts non echec`() {
        assertNull(sentVerdictForResultCode(-1, zeroProvesSentForExactDevice = false).second)
        assertNull(sentVerdictForResultCode(0, zeroProvesSentForExactDevice = false).second)
    }
}
