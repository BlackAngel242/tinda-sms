package tech.dmampro.tinda.util

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Journal d'événements d'utilisation : quel écran, quel bouton, quel
 * réglage, quelle erreur. Sert au diagnostic à distance : l'utilisateur
 * exporte le fichier depuis l'écran Diagnostic et l'envoie au développeur.
 *
 * RÈGLE D'OR (AGENTS.md) : jamais de contenu de SMS, de numéro de
 * téléphone, de PIN ou de donnée personnelle ici. Uniquement des noms
 * d'actions et des détails techniques (identifiant de thème, classe
 * d'erreur, résultat).
 */
object EventLog {

    data class Event(val time: Long, val name: String, val detail: String)

    private const val MAX_MEMORY = 200
    private const val MAX_FILE_BYTES = 256 * 1024L

    private val events = ConcurrentLinkedDeque<Event>()
    @Volatile private var logFile: File? = null

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /** À appeler une fois au démarrage (TindaApp). */
    fun init(filesDir: File) {
        logFile = File(filesDir, "tinda_events.log")
    }

    /**
     * Enregistre un événement. [name] en minuscules pointées
     * ("tap.fab_new_message"), [detail] technique uniquement, sans PII.
     */
    @Synchronized
    fun log(name: String, detail: String = "") {
        val event = Event(System.currentTimeMillis(), name, detail.take(160))
        events.addLast(event)
        while (events.size > MAX_MEMORY) events.removeFirst()
        appendToFile(event)
    }

    fun snapshot(): List<Event> = events.toList()

    /** Chemin du fichier de log, null si non initialisé. */
    fun file(): File? = logFile?.takeIf { it.exists() }

    @Synchronized
    private fun appendToFile(event: Event) {
        val file = logFile ?: return
        try {
            if (file.exists() && file.length() > MAX_FILE_BYTES) {
                // Rotation simple : on garde la seconde moitié du fichier.
                val lines = file.readLines()
                file.writeText(lines.drop(lines.size / 2).joinToString("\n") + "\n")
            }
            val stamp = timeFormat.format(Date(event.time))
            val detail = if (event.detail.isBlank()) "" else " | ${event.detail}"
            file.appendText("$stamp | ${event.name}$detail\n")
        } catch (e: Exception) {
            // Le journal ne doit jamais faire planter l'appli.
        }
    }
}
