package tech.dmampro.tinda.data

import tech.dmampro.tinda.data.local.MessageStatus

/** État minimal d'un message nécessaire à la synchronisation des accusés. */
data class StoredMessageStatus(
    val status: MessageStatus,
    val providerId: Long?,
    val failureCode: Int? = null,
)

/** Port du magasin local utilisé par [MessageStatusSync]. */
interface LocalMessageStatusStore {
    suspend fun find(token: String): StoredMessageStatus?

    /** Tente la transition conditionnelle ; retourne les lignes modifiées. */
    suspend fun tryTransition(
        token: String,
        status: MessageStatus,
        failureCode: Int?,
    ): Int
}

/** Port d'écriture du statut dans le fournisseur SMS système. */
interface ProviderMessageStatusWriter {
    suspend fun write(providerId: Long, status: MessageStatus)
}

/** Résultat d'une demande de transition après alignement du fournisseur. */
sealed interface MessageStatusSyncResult {
    data class Applied(val persistedStatus: MessageStatus) : MessageStatusSyncResult

    data class RefusedAligned(val persistedStatus: MessageStatus) : MessageStatusSyncResult

    data object NotFound : MessageStatusSyncResult
}

/**
 * Applique une transition de statut puis aligne le fournisseur SMS
 * sur l'état réellement persisté.
 *
 * Si la transition conditionnelle est refusée, le statut demandé ne
 * doit jamais être présenté au fournisseur comme un succès : on
 * relit le message et on écrit son statut persisté. Si le message
 * n'existe plus, aucune écriture fournisseur n'est tentée.
 */
class MessageStatusSync(
    private val localStore: LocalMessageStatusStore,
    private val providerWriter: ProviderMessageStatusWriter,
) {
    suspend fun update(
        token: String,
        requestedStatus: MessageStatus,
        failureCode: Int? = null,
    ): MessageStatusSyncResult {
        val changedRows = localStore.tryTransition(token, requestedStatus, failureCode)
        val stored = localStore.find(token) ?: return MessageStatusSyncResult.NotFound
        stored.providerId?.let { providerId ->
            providerWriter.write(providerId, stored.status)
        }
        return if (changedRows > 0) {
            MessageStatusSyncResult.Applied(stored.status)
        } else {
            MessageStatusSyncResult.RefusedAligned(stored.status)
        }
    }
}
