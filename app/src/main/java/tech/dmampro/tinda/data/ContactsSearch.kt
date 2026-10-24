package tech.dmampro.tinda.data

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.dmampro.tinda.util.PhoneNumbers
import javax.inject.Inject
import javax.inject.Singleton

/** Un numéro joignable d'un contact, proposé pendant la saisie du destinataire. */
data class ContactSuggestion(
    val contactId: Long,
    val displayName: String,
    val number: String,
    /** Libellé du type de numéro fourni par Android (Mobile, Domicile...), si connu. */
    val typeLabel: String? = null,
)

/** Nombre maximal de suggestions affichées sous le champ destinataire. */
const val CONTACT_SUGGESTION_LIMIT = 20

/**
 * Termes de recherche déduits de la saisie, purs et testables :
 * le terme nom (texte libre) et le terme chiffres (pour les numéros).
 * Un terme null signifie « ne pas chercher sur cet axe ».
 */
fun contactSearchTerms(raw: String): Pair<String?, String?> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null to null
    val digits = PhoneNumbers.digits(trimmed)
    val nameTerm = trimmed.takeIf { it.any { c -> c.isLetter() } }
    val digitTerm = digits.takeIf { it.length >= 2 }
    return nameTerm to digitTerm
}

/** Échappe les caractères spéciaux d'un motif SQLite LIKE. */
fun escapeLike(term: String): String = buildString(term.length + 4) {
    for (c in term) {
        if (c == '%' || c == '_' || c == '\\') append('\\')
        append(c)
    }
}

/**
 * Déduplique les lignes du fournisseur : un même contact peut exposer
 * le même numéro sous plusieurs étiquettes. On garde la première
 * ligne par (contact, numéro normalisé), ordre d'arrivée conservé,
 * et on borne le résultat à [limit].
 */
fun dedupeSuggestions(
    rows: List<ContactSuggestion>,
    limit: Int = CONTACT_SUGGESTION_LIMIT,
): List<ContactSuggestion> {
    val seen = HashSet<Pair<Long, String>>()
    val out = ArrayList<ContactSuggestion>(limit)
    for (row in rows) {
        val key = row.contactId to PhoneNumbers.digits(row.number)
        if (!seen.add(key)) continue
        out += row
        if (out.size >= limit) break
    }
    return out
}

/**
 * Recherche de contacts pour le champ destinataire : interrogation du
 * fournisseur système bornée, toujours hors du fil d'interface.
 * Un contact sans numéro n'a aucune ligne dans la table des numéros :
 * il ne produit donc aucune suggestion, ce qui est le comportement
 * voulu (on ne propose jamais un nom sans numéro à envoyer).
 */
@Singleton
class ContactsSearcher @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Cherche par nom ou par chiffres ; liste vide si rien ne correspond. */
    suspend fun search(raw: String): List<ContactSuggestion> {
        val (nameTerm, digitTerm) = contactSearchTerms(raw)
        if (nameTerm == null && digitTerm == null) return emptyList()
        return withContext(Dispatchers.IO) {
            val clauses = mutableListOf<String>()
            val args = mutableListOf<String>()
            if (nameTerm != null) {
                clauses += "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? ESCAPE '\\'"
                args += "%${escapeLike(nameTerm)}%"
            }
            if (digitTerm != null) {
                clauses += "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ? ESCAPE '\\'"
                args += "%${escapeLike(digitTerm)}%"
                clauses += "${ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER} LIKE ? ESCAPE '\\'"
                args += "%${escapeLike(digitTerm)}%"
            }
            val rows = queryPhones(
                selection = clauses.joinToString(" OR "),
                selectionArgs = args.toTypedArray(),
                contactId = null,
            )
            dedupeSuggestions(rows)
        }
    }

    /**
     * Identifiant du contact derrière l'URI rendue par le sélecteur
     * natif : lu par une requête sur l'URI elle-même, pas déduit de
     * son dernier segment (format non garanti).
     */
    suspend fun contactIdFor(uri: android.net.Uri): Long? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.Contacts._ID),
                    null, null, null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else null
                }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Tous les numéros d'un contact précis, pour le sélecteur natif :
     * le numéro est relu depuis la table des numéros du contact, jamais
     * depuis l'URI retourné seul (son extraction directe est fragile).
     */
    suspend fun numbersForContact(contactId: Long): List<ContactSuggestion> =
        withContext(Dispatchers.IO) {
            dedupeSuggestions(
                queryPhones(
                    selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    selectionArgs = arrayOf(contactId.toString()),
                    contactId = contactId,
                ),
            )
        }

    private fun queryPhones(
        selection: String,
        selectionArgs: Array<String>,
        contactId: Long?,
    ): List<ContactSuggestion> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
        )
        val rows = mutableListOf<ContactSuggestion>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
            )?.use { cursor -> rows += cursor.toSuggestions() }
        } catch (e: SecurityException) {
            // Permission refusée : aucune suggestion, la saisie manuelle reste.
            return emptyList()
        }
        return rows
    }

    private fun Cursor.toSuggestions(): List<ContactSuggestion> {
        val idIdx = getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameIdx = getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIdx = getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val typeIdx = getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
        val labelIdx = getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
        val out = mutableListOf<ContactSuggestion>()
        var scanned = 0
        while (moveToNext()) {
            // Borne de lecture : on ne parcourt jamais tout le carnet.
            if (++scanned > 200) break
            val number = getString(numberIdx)?.trim().orEmpty()
            if (number.isBlank()) continue
            val name = getString(nameIdx)?.trim().orEmpty().ifBlank { number }
            val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                context.resources,
                getInt(typeIdx),
                if (labelIdx >= 0) getString(labelIdx) else null,
            )?.toString()?.takeIf { it.isNotBlank() }
            out += ContactSuggestion(
                contactId = getLong(idIdx),
                displayName = name,
                number = number,
                typeLabel = typeLabel,
            )
        }
        return out
    }
}
