package tech.dmampro.tinda.util

/**
 * Détection de codes de vérification (OTP / 2FA) dans un SMS entrant.
 * Un message est considéré comme porteur de code s'il contient à la fois
 * un mot-clé (code, vérification, OTP, 2FA) et une suite de 4 à 8 chiffres.
 * Pur Kotlin, sans dépendance Android : testable en JUnit.
 */
object OtpDetection {

    private val codePattern = Regex("\\b\\d{4,8}\\b")
    private val keywords = listOf(
        "code", "vérification", "verification", "otp", "2fa",
        "valider", "confirmer", "confirm", "authentification", "one-time",
    )

    /** Premier code détecté, ou null si le texte ne ressemble pas à un OTP. */
    fun detectCode(text: String): String? {
        val lowered = text.lowercase()
        if (keywords.none { it in lowered }) return null
        return codePattern.find(text)?.value
    }

    /** Mise en forme lisible : "492018" devient "492-018". */
    fun formatCode(code: String): String =
        if (code.length == 6) code.substring(0, 3) + "-" + code.substring(3) else code

    private val expiryPattern = Regex("(\\d+)\\s*(minute|minutes|min|heure|heures|hour|hours)")

    /**
     * Durée de validité du code en minutes, extraite du texte
     * ("expire dans 10 minutes", "valable 5 min"). Null si introuvable.
     */
    fun detectExpiryMinutes(text: String): Int? {
        val lowered = text.lowercase()
        // Cherche "expire" ou "valable" près d'une durée.
        val expiryKeywords = listOf("expire", "valable", "valid", "expire dans")
        if (expiryKeywords.none { it in lowered }) return null
        val match = expiryPattern.find(lowered) ?: return null
        val value = match.groupValues[1].toIntOrNull() ?: return null
        val unit = match.groupValues[2]
        return if (unit.startsWith("heure") || unit.startsWith("hour")) value * 60 else value
    }
}
