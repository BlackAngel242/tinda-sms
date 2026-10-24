package tech.dmampro.tinda.ui.conversation

import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.sms.PartOutcome
import tech.dmampro.tinda.sms.PartSentState

/**
 * Mot affiché sous un message sortant, déduit uniquement des états
 * réellement persistés (statut du message + verdicts des segments).
 *
 * - EN_COURS : des verdicts de segment sont encore attendus.
 * - ENVOYE : tous les segments sont confirmés émis, la livraison ne
 *   l'est pas (ou pas pour tous les segments).
 * - LIVRE : livraison confirmée pour tous les segments.
 * - ECHEC : un échec documenté existe.
 * - NON_CONFIRME : tous les verdicts reçus sont non concluants (par
 *   exemple un code 0 non prouvé sur cette ROM) ; plus rien n'est
 *   attendu qui puisse trancher, et on ne devine pas.
 *
 * Jamais de « lu » : un SMS classique ne fournit pas de preuve de
 * lecture (RB-05). Jamais de LIVRE déduit de l'appel radio, d'un code
 * 0 non prouvé ou de la simple absence d'erreur.
 */
enum class StatusWord { EN_COURS, ENVOYE, LIVRE, ECHEC, NON_CONFIRME }

fun statusWord(status: MessageStatus, parts: List<PartOutcome>): StatusWord? =
    when (status) {
        MessageStatus.RECEIVED -> null
        MessageStatus.FAILED -> StatusWord.ECHEC
        MessageStatus.DELIVERED -> StatusWord.LIVRE
        MessageStatus.SENT -> StatusWord.ENVOYE
        MessageStatus.SENDING ->
            if (
                parts.isNotEmpty() &&
                parts.none { it.sent == PartSentState.PENDING } &&
                parts.any { it.sent == PartSentState.UNCONFIRMED }
            ) {
                StatusWord.NON_CONFIRME
            } else {
                StatusWord.EN_COURS
            }
    }
