package tech.dmampro.tinda.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Revue Codex PR nº 4 : le badge d'une bulle sortante lit
 * l'emplacement conservé au moment de l'envoi, pas l'emplacement
 * actuel de la carte. L'ancien code résolvait le subscriptionId
 * dans la correspondance actuelle : une carte déplacée réécrivait
 * l'historique de ses anciens messages.
 */
class MessageSimTest {

    @Test
    fun persistedFirstSlot_isSim1() {
        assertEquals(MessageSim.Known(1), messageSimFor(simSlot = 0))
    }

    @Test
    fun persistedSecondSlot_isSim2() {
        assertEquals(MessageSim.Known(2), messageSimFor(simSlot = 1))
    }

    @Test
    fun simMovedToOtherSlot_oldMessageKeepsItsHistoricalSlot() {
        // La carte utilisée pour ce message était dans le logement 1
        // à l'envoi (slot conservé 0). Elle est aujourd'hui dans le
        // logement 2 : la résolution ne reçoit même plus la
        // correspondance actuelle, l'historique ne peut pas bouger.
        val messageSimSlotAtSendTime = 0
        assertEquals(MessageSim.Known(1), messageSimFor(messageSimSlotAtSendTime))
    }

    @Test
    fun simRemovedAfterSend_persistedSlotStillProvesHistory() {
        // Carte retirée depuis l'envoi : l'ancien code répondait
        // Unknown faute de correspondance actuelle. Le slot conservé
        // continue de prouver l'historique, sans rien inventer.
        assertEquals(MessageSim.Known(2), messageSimFor(simSlot = 1))
    }

    @Test
    fun oldMessageWithoutPersistedSlot_isUnknown() {
        // Message envoyé avant la conservation du slot (base
        // version 11 ou avant), ou slot illisible à l'envoi : on
        // n'attribue pas l'emplacement actuel d'une carte qui a pu
        // être déplacée ou remplacée depuis.
        assertEquals(MessageSim.Unknown, messageSimFor(simSlot = null))
    }

    @Test
    fun negativePersistedSlot_isUnknown() {
        assertEquals(MessageSim.Unknown, messageSimFor(simSlot = -1))
    }
}
