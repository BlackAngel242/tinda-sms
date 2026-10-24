package tech.dmampro.tinda.sms

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.dmampro.tinda.data.local.MessageStatus

/**
 * Frontière radio de l'envoi : avant le retour de l'appel radio,
 * une exception vaut échec de soumission ; après son retour, une
 * panne d'écriture fournisseur ne prouve pas que le SMS n'est pas
 * parti et ne doit ni marquer FAILED ni suggérer un renvoi.
 */
class SendFlowTest {

    @Test
    fun radioFailureIsARealFailureAndProviderIsNeverTouched() = runBlocking {
        var providerCalls = 0
        val outcome = SendFlow.execute(
            submitToRadio = { throw IllegalStateException("radio injoignable") },
            writeToProvider = {
                providerCalls++
                42L
            },
        )

        assertTrue(outcome is SendFlow.Outcome.RadioFailed)
        assertEquals(0, providerCalls)
    }

    @Test
    fun providerFailureAfterAcceptedRadioStaysSubmitted() = runBlocking {
        var signaled: Exception? = null
        val outcome = SendFlow.execute(
            submitToRadio = { /* appel radio accepté */ },
            writeToProvider = { throw RuntimeException("fournisseur en panne") },
            onProviderFailure = { signaled = it },
        )

        assertTrue(outcome is SendFlow.Outcome.Submitted)
        assertNull((outcome as SendFlow.Outcome.Submitted).providerId)
        assertTrue(signaled is RuntimeException)
    }

    @Test
    fun acceptedRadioAndProviderWriteReturnProviderId() = runBlocking {
        val outcome = SendFlow.execute(
            submitToRadio = { },
            writeToProvider = { 42L },
        )

        assertEquals(42L, (outcome as SendFlow.Outcome.Submitted).providerId)
    }

    @Test
    fun postRadioFailureKeepsUncertainStateUntilLateReceipt() = runBlocking {
        // Après une panne fournisseur post-radio, l'appelant conserve
        // l'état SENDING : pas de FAILED, pas de renvoi. L'accusé
        // tardif tranche ensuite via l'agrégation des segments.
        var status = MessageStatus.SENDING
        val outcome = SendFlow.execute(
            submitToRadio = { },
            writeToProvider = { throw RuntimeException("écriture impossible") },
        )
        if (outcome is SendFlow.Outcome.RadioFailed) {
            status = MessageStatus.FAILED
        }

        assertTrue(outcome is SendFlow.Outcome.Submitted)
        assertEquals(MessageStatus.SENDING, status)
        assertFalse(status == MessageStatus.FAILED)

        val resolved = MultipartStatus.aggregate(
            parts = listOf(PartOutcome(PartSentState.CONFIRMED, delivered = false)),
            current = status,
        )
        assertEquals(MessageStatus.SENT, resolved)
    }
}
