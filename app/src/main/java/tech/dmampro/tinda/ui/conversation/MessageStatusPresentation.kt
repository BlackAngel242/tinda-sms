package tech.dmampro.tinda.ui.conversation

import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.sms.PartOutcome
import tech.dmampro.tinda.sms.PartSentState

/**
 * Délai au-delà duquel un SMS confirmé émis mais sans aucun accusé de
 * livraison cesse de s'afficher « Envoyé » et s'affiche « Livraison
 * non confirmée ». Sur les réseaux concernés, un accusé qui existe
 * arrive en quelques secondes ou minutes ; passé une heure, garder
 * « Envoyé » laisserait croire que tout est connu alors que la
 * livraison reste sans preuve. L'état reste réversible : un accusé
 * tardif fait repasser le message à « Livré » à tout moment.
 */
const val DELIVERY_UNCONFIRMED_AFTER_MS: Long = 60L * 60L * 1000L

/**
 * Mot affiché sous un message sortant, déduit uniquement des états
 * réellement persistés (statut du message + verdicts des segments)
 * et, pour le délai de livraison, de l'heure d'envoi persistée.
 *
 * - EN_COURS : des verdicts de segment sont encore attendus.
 * - ENVOYE : tous les segments sont confirmés émis, la livraison ne
 *   l'est pas (ou pas pour tous les segments), et le délai de
 *   confirmation n'est pas dépassé.
 * - LIVRE : livraison confirmée pour tous les segments. Un accusé
 *   tardif gagne à tout âge : DELIVERED ne retombe jamais.
 * - ECHEC : un échec documenté existe.
 * - NON_CONFIRME : deux cas. Verdicts de segments tous non concluants
 *   (par exemple un code 0 non prouvé sur cette ROM) et plus rien à
 *   attendre ; ou message confirmé émis depuis plus longtemps que
 *   [DELIVERY_UNCONFIRMED_AFTER_MS] sans accusé de livraison. Dans
 *   les deux cas, le mot ne dit ni « livré » ni « échoué » : il dit
 *   que la livraison n'est pas confirmée.
 *
 * La fonction est pure : mêmes entrées persistées, même mot, y
 * compris après un redémarrage de l'application (rien n'est gardé en
 * mémoire). Sans heure d'envoi connue, aucun dépassement de délai
 * n'est déduit.
 *
 * Jamais de « lu » : un SMS classique ne fournit pas de preuve de
 * lecture (RB-05). Jamais de LIVRE déduit de l'appel radio, d'un code
 * 0 non prouvé ou de la simple absence d'erreur.
 */
enum class StatusWord { EN_COURS, ENVOYE, LIVRE, ECHEC, NON_CONFIRME }

fun statusWord(
    status: MessageStatus,
    parts: List<PartOutcome>,
    sentAtMillis: Long? = null,
    nowMillis: Long = 0L,
): StatusWord? =
    when (status) {
        MessageStatus.RECEIVED -> null
        MessageStatus.FAILED -> StatusWord.ECHEC
        MessageStatus.DELIVERED -> StatusWord.LIVRE
        MessageStatus.SENT ->
            if (
                sentAtMillis != null &&
                nowMillis - sentAtMillis >= DELIVERY_UNCONFIRMED_AFTER_MS
            ) {
                StatusWord.NON_CONFIRME
            } else {
                StatusWord.ENVOYE
            }
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
