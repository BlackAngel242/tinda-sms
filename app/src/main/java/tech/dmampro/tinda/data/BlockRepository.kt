package tech.dmampro.tinda.data

import kotlinx.coroutines.flow.Flow
import tech.dmampro.tinda.data.local.BlockEvent
import tech.dmampro.tinda.data.local.BlockedNumber
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.util.PhoneNumbers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Liste noire (F7) : numéros bloqués + journal des blocages.
 * Le journal ne contient que des événements (numéro + date),
 * jamais le contenu des messages.
 *
 * La comparaison passe par [PhoneNumbers] : un numéro bloqué au format
 * local bloque aussi sa forme internationale, et réciproquement (P0-7).
 * Les entrées déjà stockées dans d'anciens formats restent efficaces sans
 * migration, car la correspondance se fait par équivalence à la lecture ;
 * les nouvelles entrées sont écrites sous leur clé canonique.
 */
@Singleton
class BlockRepository @Inject constructor(
    private val db: TindaDatabase,
) {
    suspend fun isBlocked(number: String): Boolean =
        PhoneNumbers.isBlocked(number, db.blockDao().getAllNumbers())

    suspend fun block(number: String) {
        val key = PhoneNumbers.storageKey(number)
        if (key.isBlank()) return
        // Pas de doublon sous un autre format du même numéro.
        if (PhoneNumbers.isBlocked(key, db.blockDao().getAllNumbers())) return
        db.blockDao().block(BlockedNumber(key, System.currentTimeMillis()))
    }

    suspend fun unblock(number: String) {
        val stored = db.blockDao().getAllNumbers()
            .firstOrNull { PhoneNumbers.equivalent(it, number) } ?: return
        db.blockDao().unblock(stored)
    }

    fun observeBlocked(): Flow<List<BlockedNumber>> =
        db.blockDao().observeBlocked()

    fun observeEvents(limit: Int = 50): Flow<List<BlockEvent>> =
        db.blockDao().observeEvents(limit)

    /** Journalise un SMS bloqué : numéro + date uniquement, pas le contenu. */
    suspend fun logBlockedEvent(number: String) {
        db.blockDao().logEvent(
            BlockEvent(number = PhoneNumbers.storageKey(number), date = System.currentTimeMillis()),
        )
    }
}
