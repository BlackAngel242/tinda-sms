package tech.dmampro.tinda.ui.components

/**
 * SIM réellement utilisée par un message sortant (historique propre
 * au message), à distinguer de la SIM sélectionnée dans le compositeur,
 * qui ne décrit que le prochain envoi.
 */
sealed interface MessageSim {
    /** SIM retrouvée parmi les SIM actuelles : numéro d'emplacement (1, 2...). */
    data class Known(val slotNumber: Int) : MessageSim

    /**
     * SIM non identifiable : identifiant absent (-1), ou SIM retirée ou
     * remplacée depuis l'envoi. Aucun numéro d'emplacement inventé.
     */
    data object Unknown : MessageSim
}

/**
 * Résout la SIM d'un message depuis son [subscriptionId] persisté et
 * la correspondance actuelle abonnement vers emplacement (slotIndex).
 */
fun messageSimFor(subscriptionId: Int, slotBySubscriptionId: Map<Int, Int>): MessageSim {
    if (subscriptionId < 0) return MessageSim.Unknown
    val slotIndex = slotBySubscriptionId[subscriptionId] ?: return MessageSim.Unknown
    return MessageSim.Known(slotNumber = slotIndex + 1)
}
