package tech.dmampro.tinda.data

import kotlinx.coroutines.flow.Flow
import tech.dmampro.tinda.data.local.BlockEvent
import tech.dmampro.tinda.data.local.BlockedNumber
import tech.dmampro.tinda.data.local.TindaDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Liste noire (F7) : numéros bloqués + journal des blocages.
 * Le journal ne contient que des événements (numéro + date),
 * jamais le contenu des messages.
 */
@Singleton
class BlockRepository @Inject constructor(
    private val db: TindaDatabase,
) {
    /** Normalise un numéro pour la comparaison (espaces supprimés). */
    private fun normalize(number: String): String =
        number.filter { !it.isWhitespace() }

    suspend fun isBlocked(number: String): Boolean =
        db.blockDao().isBlocked(normalize(number)) > 0

    suspend fun block(number: String) {
        val clean = normalize(number)
        if (clean.isBlank()) return
        db.blockDao().block(BlockedNumber(clean, System.currentTimeMillis()))
    }

    suspend fun unblock(number: String) {
        db.blockDao().unblock(normalize(number))
    }

    fun observeBlocked(): Flow<List<BlockedNumber>> =
        db.blockDao().observeBlocked()

    fun observeEvents(limit: Int = 50): Flow<List<BlockEvent>> =
        db.blockDao().observeEvents(limit)

    /** Journalise un SMS bloqué : numéro + date uniquement, pas le contenu. */
    suspend fun logBlockedEvent(number: String) {
        db.blockDao().logEvent(BlockEvent(number = normalize(number), date = System.currentTimeMillis()))
    }
}
