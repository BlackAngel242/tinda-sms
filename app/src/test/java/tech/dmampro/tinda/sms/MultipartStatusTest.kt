package tech.dmampro.tinda.sms

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.dmampro.tinda.data.local.MessageStatus

/**
 * Agrégation des accusés multipart (L3) : le statut du message se
 * déduit de l'ensemble des segments, jamais du dernier accusé reçu.
 * Callbacks désordonnés, doublons, segment manquant, échec partiel,
 * livraison tardive : le statut reste monotone et honnête (non
 * confirmé tant que les preuves manquent).
 */
class MultipartStatusTest {

    private fun part(
        sent: PartSentState = PartSentState.PENDING,
        delivered: Boolean = false,
    ) = PartOutcome(sent = sent, delivered = delivered)

    @Test
    fun `un segment confirme donne envoye`() {
        val parts = listOf(part(sent = PartSentState.CONFIRMED))
        assertEquals(
            MessageStatus.SENT,
            MultipartStatus.aggregate(parts, MessageStatus.SENDING),
        )
    }

    @Test
    fun `callbacks desordonnes - statut en attente tant qu un segment manque`() {
        val parts = listOf(
            part(sent = PartSentState.CONFIRMED),
            part(sent = PartSentState.PENDING),
            part(sent = PartSentState.CONFIRMED),
        )
        assertEquals(
            MessageStatus.SENDING,
            MultipartStatus.aggregate(parts, MessageStatus.SENDING),
        )
    }

    @Test
    fun `tous les segments confirmes puis livres donnent livre`() {
        val sent = listOf(
            part(sent = PartSentState.CONFIRMED),
            part(sent = PartSentState.CONFIRMED),
        )
        assertEquals(MessageStatus.SENT, MultipartStatus.aggregate(sent, MessageStatus.SENDING))
        val delivered = sent.map { it.copy(delivered = true) }
        assertEquals(
            MessageStatus.DELIVERED,
            MultipartStatus.aggregate(delivered, MessageStatus.SENT),
        )
    }

    @Test
    fun `livraison recue avant la fin des confirmations ne donne pas livre`() {
        val parts = listOf(
            part(sent = PartSentState.CONFIRMED, delivered = true),
            part(sent = PartSentState.PENDING, delivered = true),
        )
        assertEquals(
            MessageStatus.SENDING,
            MultipartStatus.aggregate(parts, MessageStatus.SENDING),
        )
    }

    @Test
    fun `un segment en echec documente donne echec meme si un autre est confirme`() {
        val parts = listOf(
            part(sent = PartSentState.CONFIRMED),
            part(sent = PartSentState.FAILED),
        )
        assertEquals(
            MessageStatus.FAILED,
            MultipartStatus.aggregate(parts, MessageStatus.SENDING),
        )
    }

    @Test
    fun `verdicts non concluants gardent le statut non confirme`() {
        val parts = listOf(
            part(sent = PartSentState.CONFIRMED),
            part(sent = PartSentState.UNCONFIRMED),
        )
        assertEquals(
            MessageStatus.SENDING,
            MultipartStatus.aggregate(parts, MessageStatus.SENDING),
        )
    }

    @Test
    fun `le statut ne regresse jamais`() {
        val parts = listOf(part(sent = PartSentState.CONFIRMED))
        assertEquals(
            MessageStatus.DELIVERED,
            MultipartStatus.aggregate(parts, MessageStatus.DELIVERED),
        )
        val failedParts = listOf(part(sent = PartSentState.FAILED))
        assertEquals(
            MessageStatus.FAILED,
            MultipartStatus.aggregate(failedParts, MessageStatus.FAILED),
        )
    }

    @Test
    fun `sans segment suivi le statut est inchange`() {
        assertEquals(
            MessageStatus.SENDING,
            MultipartStatus.aggregate(emptyList(), MessageStatus.SENDING),
        )
    }

    @Test
    fun `livraison partielle apres envoi confirme donne envoye`() {
        val parts = listOf(
            part(sent = PartSentState.CONFIRMED, delivered = true),
            part(sent = PartSentState.CONFIRMED, delivered = false),
        )
        assertEquals(
            MessageStatus.SENT,
            MultipartStatus.aggregate(parts, MessageStatus.SENDING),
        )
    }
}
