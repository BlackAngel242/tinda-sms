package tech.dmampro.tinda.sms

import tech.dmampro.tinda.data.local.MessageStatus

/**
 * Verdict d'émission d'un segment de SMS multipart.
 *
 * - PENDING : aucun accusé reçu pour ce segment.
 * - CONFIRMED : l'opérateur a confirmé l'émission du segment.
 * - FAILED : échec documenté (code d'erreur radio connu).
 * - UNCONFIRMED : un accusé est arrivé mais ne prouve ni le succès ni
 *   l'échec (par exemple resultCode 0 hors ROM où ce comportement est
 *   observé et documenté). Le message reste « non confirmé ».
 */
enum class PartSentState { PENDING, CONFIRMED, FAILED, UNCONFIRMED }

/** État connu d'un segment : verdict d'émission et livraison éventuelle. */
data class PartOutcome(
    val sent: PartSentState = PartSentState.PENDING,
    val delivered: Boolean = false,
)

/**
 * Agrégation des accusés segment par segment (L3, corrige C4).
 *
 * Règles :
 * - un échec documenté d'un segment fait échouer le message entier ;
 * - SENT exige la confirmation de tous les segments ;
 * - DELIVERED exige en plus la livraison de tous les segments ;
 * - tant qu'un verdict manque ou n'est pas concluant, le message reste
 *   SENDING (non confirmé), jamais promu par supposition ;
 * - le statut est monotone : DELIVERED et FAILED sont terminaux, un
 *   accusé tardif ou en double ne fait jamais régresser le statut.
 */
/**
 * Traduit un resultCode radio d'accusé d'émission en verdict de segment.
 *
 * - `-1` (Activity.RESULT_OK) : confirmé.
 * - `0` (Activity.RESULT_CANCELED) : observé sur le terrain le
 *   18 octobre 2026 sur un Redmi Note 14 (HyperOS, fabricant Xiaomi)
 *   pour des envois réellement partis : il y vaut confirmation.
 *   Partout ailleurs, ce code ne prouve ni succès ni échec : le
 *   segment reste non confirmé. Les autres ROM (Tecno, Infinix,
 *   Itel...) ne sont pas prouvées (hypothèse HYP-03 du PRD).
 * - tout autre code : échec documenté, conservé pour le diagnostic.
 *   La documentation Android expose des erreurs au-delà de 1 à 4 ;
 *   aucun code inconnu n'est ignoré.
 */
fun sentVerdictForResultCode(
    resultCode: Int,
    zeroMeansSentOnThisRom: Boolean,
): Pair<PartSentState, Int?> = when {
    resultCode == -1 -> PartSentState.CONFIRMED to null
    resultCode == 0 ->
        if (zeroMeansSentOnThisRom) {
            PartSentState.CONFIRMED to null
        } else {
            PartSentState.UNCONFIRMED to null
        }
    else -> PartSentState.FAILED to resultCode
}

object MultipartStatus {

    fun aggregate(parts: List<PartOutcome>, current: MessageStatus): MessageStatus {
        if (current == MessageStatus.DELIVERED || current == MessageStatus.FAILED) {
            return current
        }
        if (parts.isEmpty()) return current
        if (parts.any { it.sent == PartSentState.FAILED }) return MessageStatus.FAILED
        val allConfirmed = parts.all { it.sent == PartSentState.CONFIRMED }
        if (!allConfirmed) return MessageStatus.SENDING
        if (parts.all { it.delivered }) return MessageStatus.DELIVERED
        return MessageStatus.SENT
    }
}
