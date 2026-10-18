package tech.dmampro.tinda.util

/**
 * Encodage SMS : GSM-7 bits contre UCS-2, décompte des segments.
 * Pur Kotlin, sans dépendance Android : testable en JUnit.
 *
 * Règles : GSM-7 = 160 caractères en unitaire, 153 par segment en concaténé ;
 * UCS-2 = 70 en unitaire, 67 par segment. Les caractères de la table
 * d'extension GSM-7 comptent double.
 */
object SmsEncoding {

    enum class Encoding { GSM_7, UCS_2 }

    data class SegmentInfo(
        val encoding: Encoding,
        /** Nombre de segments (0 si le texte est vide). */
        val parts: Int,
        /** Caractères restants dans le segment courant. */
        val remaining: Int,
    )

    // Alphabet GSM 7 bits, table de base (3GPP TS 23.038).
    private const val GSM_7BIT_BASIC =
        "@£\$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞ\u001BÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà"

    // Table d'extension : chaque caractère compte double.
    private const val GSM_7BIT_EXT = "^{}\\[~]|€\u000C"

    /** Nombre de septets nécessaires, ou null si le texte n'est pas codable en GSM-7. */
    fun septetCount(text: String): Int? {
        var count = 0
        for (c in text) {
            count += when {
                c in GSM_7BIT_BASIC -> 1
                c in GSM_7BIT_EXT -> 2
                else -> return null
            }
        }
        return count
    }

    fun segmentInfo(text: String): SegmentInfo {
        val septets = septetCount(text)
        return if (septets != null) {
            info(septets, Encoding.GSM_7, singleLimit = 160, multiLimit = 153)
        } else {
            info(text.length, Encoding.UCS_2, singleLimit = 70, multiLimit = 67)
        }
    }

    private fun info(units: Int, encoding: Encoding, singleLimit: Int, multiLimit: Int): SegmentInfo {
        if (units == 0) return SegmentInfo(encoding, 0, singleLimit)
        if (units <= singleLimit) return SegmentInfo(encoding, 1, singleLimit - units)
        val parts = (units + multiLimit - 1) / multiLimit
        return SegmentInfo(encoding, parts, parts * multiLimit - units)
    }
}
