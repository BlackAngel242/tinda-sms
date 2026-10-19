package tech.dmampro.tinda.data

import kotlinx.coroutines.flow.Flow
import tech.dmampro.tinda.data.local.FavoriteContact
import tech.dmampro.tinda.data.local.FavoriteContactDao
import javax.inject.Inject
import javax.inject.Singleton

/** Contacts favoris (maquette Stitch : étoile dans l'en-tête du chat). */
@Singleton
class FavoriteRepository @Inject constructor(
    private val dao: FavoriteContactDao,
) {
    fun observeAll(): Flow<List<FavoriteContact>> = dao.observeAll()

    suspend fun isFavorite(address: String): Boolean =
        dao.isFavorite(SimAssignmentRepository.normalize(address)) > 0

    suspend fun toggleFavorite(address: String): Boolean {
        val normalized = SimAssignmentRepository.normalize(address)
        if (normalized.isBlank()) return false
        return if (isFavorite(normalized)) {
            dao.remove(normalized)
            false
        } else {
            dao.add(FavoriteContact(normalized, System.currentTimeMillis()))
            true
        }
    }
}
