package tech.dmampro.tinda.util

import android.util.Log
import tech.dmampro.tinda.BuildConfig

/**
 * Logger de l'app. Règle absolue (AGENTS.md) : jamais de contenu SMS, de
 * numéro, de PIN ou de donnée personnelle dans les logs.
 *
 * En debug, un garde-fou remplace les séquences de chiffres suspects par
 * [PII?] au lieu de les journaliser telles quelles.
 */
object TindaLogger {

    // Suite de 6 chiffres ou plus : probablement un numéro ou un code.
    private val sensitiveDigits = Regex("\\d{6,}")

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, scrub(message))
    }

    fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.w(tag, scrub(message))
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(tag, scrub(message), throwable)
    }

    private fun scrub(message: String): String =
        sensitiveDigits.replace(message, "[PII?]")
}
