package tech.dmampro.tinda.util

import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Journal d'envoi (diagnostic terrain) : les 60 derniers événements du
 * circuit d'envoi, horodatés, visibles dans l'écran Diagnostic.
 * Règle absolue (AGENTS.md) : jamais de contenu SMS, de numéro, de PIN
 * ni de donnée personnelle. Seuls des codes et des étapes techniques.
 */
object SendJournal {

    data class Entry(val time: Long, val event: String)

    private const val MAX_ENTRIES = 60
    private val entries = ArrayDeque<Entry>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Synchronized
    fun log(event: String) {
        entries.addLast(Entry(System.currentTimeMillis(), event))
        while (entries.size > MAX_ENTRIES) entries.removeFirst()
        TindaLogger.i("SendJournal", event)
    }

    @Synchronized
    fun snapshot(): List<Entry> = entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
    }

    fun formatTime(time: Long): String = timeFormat.format(Date(time))
}
