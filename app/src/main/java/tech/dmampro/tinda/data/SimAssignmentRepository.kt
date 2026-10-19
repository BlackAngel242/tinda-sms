package tech.dmampro.tinda.data

import kotlinx.coroutines.flow.Flow
import tech.dmampro.tinda.data.local.SimAssignment
import tech.dmampro.tinda.data.local.SimAssignmentDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attribution SIM par contact (Stitch 2.1, DEC-S2) : mémorise la SIM
 * d'envoi pour un numéro. Normalisation : chiffres, 9 derniers.
 */
@Singleton
class SimAssignmentRepository @Inject constructor(
    private val dao: SimAssignmentDao,
) {
    fun observeAll(): Flow<List<SimAssignment>> = dao.observeAll()

    suspend fun getForAddress(address: String): Int? =
        dao.getForAddress(normalize(address))

    suspend fun setForAddress(address: String, subscriptionId: Int) {
        val normalized = normalize(address)
        if (normalized.isBlank()) return
        dao.upsert(
            SimAssignment(
                address = normalized,
                subscriptionId = subscriptionId,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun clearForAddress(address: String) {
        dao.delete(normalize(address))
    }

    companion object {
        /** Normalise un numéro : chiffres uniquement, 9 derniers. */
        fun normalize(address: String): String =
            address.filter { it.isDigit() }.takeLast(9)
    }
}
