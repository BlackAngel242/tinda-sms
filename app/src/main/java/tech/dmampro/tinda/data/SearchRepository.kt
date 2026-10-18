package tech.dmampro.tinda.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.dmampro.tinda.data.local.MessageSearchResult
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.data.local.searchMatchQuery
import tech.dmampro.tinda.data.local.toFtsMatch
import javax.inject.Inject
import javax.inject.Singleton

/** Résultat de recherche prêt pour l'UI. */
data class SearchResultItem(
    val threadId: Long,
    val displayName: String,
    /** Extrait avec marqueurs de surlignage (zone privée Unicode). */
    val snippet: String,
    val date: Long,
)

/**
 * Recherche plein texte (F3) sur l'index FTS4 des messages.
 * Les messages simulés et (lot 4) ceux du coffre sont exclus par l'index.
 */
@Singleton
class SearchRepository @Inject constructor(
    private val db: TindaDatabase,
    private val smsRepository: SmsRepository,
) {
    /** Recherche dans tout l'historique ; liste vide si la requête est vide. */
    suspend fun search(rawQuery: String): List<SearchResultItem> =
        withContext(Dispatchers.IO) {
            if (toFtsMatch(rawQuery).isEmpty()) return@withContext emptyList()
            val rows: List<MessageSearchResult> =
                db.searchDao().searchRaw(searchMatchQuery(rawQuery))
            rows.map { row ->
                SearchResultItem(
                    threadId = row.threadId,
                    displayName = smsRepository.resolveDisplayName(row.address)
                        .ifBlank { row.address },
                    snippet = row.snippet,
                    date = row.date,
                )
            }
        }
}
