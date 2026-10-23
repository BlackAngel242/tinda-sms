package tech.dmampro.tinda.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Revue Codex PR nº 4 : la SIM affichée dans une bulle sortante est
 * celle du message (subscriptionId persisté), jamais celle du
 * compositeur. Une SIM retirée ou inconnue reste explicite.
 */
class MessageSimTest {

    private val slots = mapOf(11 to 0, 22 to 1)

    @Test
    fun firstSlot_isSim1() {
        assertEquals(MessageSim.Known(1), messageSimFor(11, slots))
    }

    @Test
    fun secondSlot_isSim2() {
        assertEquals(MessageSim.Known(2), messageSimFor(22, slots))
    }

    @Test
    fun missingSubscription_isUnknown() {
        // SIM retirée ou remplacée depuis l'envoi : aucun slot inventé.
        assertEquals(MessageSim.Unknown, messageSimFor(99, slots))
    }

    @Test
    fun negativeSubscription_isUnknown() {
        // -1 = identifiant non persisté pour ce message.
        assertEquals(MessageSim.Unknown, messageSimFor(-1, slots))
    }

    @Test
    fun noCurrentSim_isUnknown() {
        assertEquals(MessageSim.Unknown, messageSimFor(11, emptyMap()))
    }
}
