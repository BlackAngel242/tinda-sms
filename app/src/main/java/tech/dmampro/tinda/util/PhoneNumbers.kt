package tech.dmampro.tinda.util

/**
 * Normalisation et comparaison des adresses SMS (numéros et expéditeurs
 * alphanumériques). Source unique pour la liste noire et les futurs
 * appelants, en remplacement des normalisations locales divergentes.
 */
object PhoneNumbers {

    /**
     * Indicatif du contexte régional du projet (SIM de SmokeSama, Congo).
     * Sert uniquement à reconnaître la forme nationale d'un numéro
     * international du même pays ; jamais d'identité universelle.
     */
    const val PROJECT_COUNTRY_CODE = "242"

    /** En dessous de ce nombre de chiffres, pas d'équivalence par forme nationale. */
    private const val MIN_NATIONAL_DIGITS = 6

    /** Que des chiffres. */
    fun digits(raw: String): String = raw.filter { it.isDigit() }

    /** Vrai pour un expéditeur alphanumérique (nom d'opérateur, service). */
    fun isAlphanumeric(raw: String): Boolean = raw.any { it.isLetter() }

    /** Clé canonique de stockage : texte en majuscules, ou + optionnel et chiffres. */
    fun storageKey(raw: String): String {
        val trimmed = raw.trim()
        if (isAlphanumeric(trimmed)) return trimmed.uppercase()
        val d = digits(trimmed)
        return if (trimmed.startsWith("+")) "+$d" else d
    }

    /**
     * Équivalence entre deux adresses.
     *
     * - Une adresse vide n'est équivalente à rien, pas même à une autre vide.
     * - Un expéditeur alphanumérique ne se compare qu'à un autre
     *   alphanumérique, sur sa clé canonique (casse ignorée).
     * - Deux numéros sont équivalents si leurs chiffres sont identiques,
     *   ou si leurs formes nationales coïncident avec au moins 6 chiffres
     *   significatifs. La forme nationale retire l'indicatif du contexte
     *   régional du projet ([PROJECT_COUNTRY_CODE]) ou le 0 initial.
     *   Ce n'est pas une identité universelle : un numéro international
     *   d'un autre pays ne se réduit pas à la forme nationale du projet
     *   et ne peut donc pas correspondre à un numéro local par suffixe.
     */
    fun equivalent(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        val alphaA = isAlphanumeric(a)
        val alphaB = isAlphanumeric(b)
        if (alphaA || alphaB) return alphaA && alphaB && storageKey(a) == storageKey(b)
        val da = internationalDigits(a)
        val db = internationalDigits(b)
        if (da.isEmpty() || db.isEmpty()) return false
        if (da == db) return true
        val na = nationalForm(da)
        val nb = nationalForm(db)
        return na.length >= MIN_NATIONAL_DIGITS && na == nb
    }

    /** Chiffres, avec le préfixe international « 00 » ramené à la forme « + ». */
    private fun internationalDigits(raw: String): String {
        val d = digits(raw)
        return if (!raw.trim().startsWith("+") && d.startsWith("00")) d.drop(2) else d
    }

    /** Retire l'indicatif du projet ou le 0 de tête d'un numéro national. */
    private fun nationalForm(d: String): String = when {
        d.startsWith(PROJECT_COUNTRY_CODE) && d.length > PROJECT_COUNTRY_CODE.length ->
            d.drop(PROJECT_COUNTRY_CODE.length)
        d.startsWith("0") && d.length > 1 -> d.drop(1)
        else -> d
    }

    /** Vrai si l'adresse correspond à l'une des clés bloquées stockées. */
    fun isBlocked(number: String, blockedKeys: List<String>): Boolean =
        blockedKeys.any { equivalent(it, number) }
}
