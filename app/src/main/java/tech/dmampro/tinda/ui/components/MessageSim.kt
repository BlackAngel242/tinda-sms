package tech.dmampro.tinda.ui.components

/**
 * SIM réellement utilisée par un message sortant (historique propre
 * au message), à distinguer de la SIM sélectionnée dans le compositeur,
 * qui ne décrit que le prochain envoi.
 */
sealed interface MessageSim {
    /** Emplacement conservé au moment de l'envoi (1, 2...). */
    data class Known(val slotNumber: Int) : MessageSim

    /**
     * Aucun emplacement historique conservé pour ce message : rien
     * n'est affiché comme certain, aucun numéro n'est inventé.
     */
    data object Unknown : MessageSim
}

/**
 * Résout la SIM d'un message depuis l'emplacement conservé au moment
 * de son envoi (`messages.simSlot`), et rien d'autre.
 *
 * Revue Codex PR nº 4 : la correspondance actuelle abonnement vers
 * emplacement ne prouve pas le passé. Si la même carte change de
 * logement, ses anciens messages affichaient son nouvel emplacement ;
 * si elle était retirée puis remplacée, un ancien message pouvait
 * hériter de l'emplacement d'une autre carte. Le slot conservé à
 * l'envoi tranche : il survit au déplacement, au retrait et au
 * remplacement. Les messages sans slot conservé (envoyés avant la
 * version 12, ou slot illisible à l'envoi) restent
 * [MessageSim.Unknown].
 */
fun messageSimFor(simSlot: Int?): MessageSim {
    if (simSlot == null || simSlot < 0) return MessageSim.Unknown
    return MessageSim.Known(slotNumber = simSlot + 1)
}
