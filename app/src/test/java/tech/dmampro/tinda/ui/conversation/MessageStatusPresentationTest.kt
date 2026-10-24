package tech.dmampro.tinda.ui.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.sms.PartOutcome
import tech.dmampro.tinda.sms.PartSentState

/**
 * Mots d'état affichés sous un message sortant : uniquement ce que
 * les états persistés prouvent. Jamais « livré » sans accusé confirmé,
 * jamais « lu » en SMS classique.
 */
class MessageStatusPresentationTest {

    @Test
    fun `un message recu n'a pas de mot d'etat`() {
        assertNull(statusWord(MessageStatus.RECEIVED, emptyList()))
    }

    @Test
    fun `un echec documente affiche Echec`() {
        val parts = listOf(
            PartOutcome(PartSentState.CONFIRMED),
            PartOutcome(PartSentState.FAILED),
        )
        assertEquals(StatusWord.ECHEC, statusWord(MessageStatus.FAILED, parts))
    }

    @Test
    fun `une livraison confirmee sur tous les segments affiche Livre`() {
        val parts = listOf(
            PartOutcome(PartSentState.CONFIRMED, delivered = true),
            PartOutcome(PartSentState.CONFIRMED, delivered = true),
        )
        assertEquals(StatusWord.LIVRE, statusWord(MessageStatus.DELIVERED, parts))
    }

    @Test
    fun `un accuse de livraison tardif garde Livre`() {
        // Le statut DELIVERED est terminal : le mot ne régresse pas.
        assertEquals(StatusWord.LIVRE, statusWord(MessageStatus.DELIVERED, emptyList()))
    }

    @Test
    fun `emis confirme sans livraison affiche Envoye`() {
        val parts = listOf(PartOutcome(PartSentState.CONFIRMED, delivered = false))
        assertEquals(StatusWord.ENVOYE, statusWord(MessageStatus.SENT, parts))
    }

    @Test
    fun `sans aucun segment suivi, envoi affiche En cours`() {
        assertEquals(StatusWord.EN_COURS, statusWord(MessageStatus.SENDING, emptyList()))
    }

    @Test
    fun `des verdicts encore attendus affichent En cours`() {
        val parts = listOf(
            PartOutcome(PartSentState.CONFIRMED),
            PartOutcome(PartSentState.PENDING),
        )
        assertEquals(StatusWord.EN_COURS, statusWord(MessageStatus.SENDING, parts))
    }

    @Test
    fun `verdicts recus mais non concluants affichent Livraison non confirmee`() {
        // Cas du code 0 non prouvé : l'accusé est arrivé, il ne prouve
        // ni l'émission ni l'échec, et plus rien n'est attendu.
        val parts = listOf(PartOutcome(PartSentState.UNCONFIRMED))
        assertEquals(StatusWord.NON_CONFIRME, statusWord(MessageStatus.SENDING, parts))
    }

    @Test
    fun `sms long avec un segment non concluant et aucun en attente affiche non confirme`() {
        val parts = listOf(
            PartOutcome(PartSentState.CONFIRMED),
            PartOutcome(PartSentState.CONFIRMED),
            PartOutcome(PartSentState.UNCONFIRMED),
        )
        assertEquals(StatusWord.NON_CONFIRME, statusWord(MessageStatus.SENDING, parts))
    }

    @Test
    fun `un segment encore en attente empeche le non confirme meme si un autre est non concluant`() {
        val parts = listOf(
            PartOutcome(PartSentState.UNCONFIRMED),
            PartOutcome(PartSentState.PENDING),
        )
        assertEquals(StatusWord.EN_COURS, statusWord(MessageStatus.SENDING, parts))
    }
}
