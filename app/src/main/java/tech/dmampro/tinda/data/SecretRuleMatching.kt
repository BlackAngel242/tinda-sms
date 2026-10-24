package tech.dmampro.tinda.data

import tech.dmampro.tinda.data.local.SecretRule

/**
 * Correspondance d'un message entrant avec les règles du coffre (liste jaune).
 * Fonction pure, extraite de VaultRepository pour être testée unitairement.
 */
object SecretRuleMatching {

    fun matches(address: String, body: String, rules: List<SecretRule>): Boolean {
        if (rules.isEmpty()) return false
        val lowerBody = body.lowercase()
        return rules.any { rule ->
            when (rule.type) {
                VaultRepository.RULE_NUMBER -> matchesNumber(address, rule.value)
                VaultRepository.RULE_KEYWORD ->
                    rule.value.isNotEmpty() && lowerBody.contains(rule.value)
                else -> false
            }
        }
    }

    /**
     * Règle NUMBER : réservée aux adresses numériques.
     *
     * Une adresse sans aucun chiffre (expéditeur alphanumérique type
     * opérateur, ou adresse vide) ne correspond jamais : sa forme normalisée
     * est vide et une comparaison par suffixe sur du vide correspondait à
     * toutes les règles (bogue P0-2, corrigé ici). Même garde côté règle :
     * une valeur sans chiffre ne correspond à rien.
     * La comparaison des adresses numériques reste celle éprouvée sur le
     * terrain (suffixe dans un sens ou dans l'autre, formes avec ou sans +).
     */
    private fun matchesNumber(address: String, ruleValue: String): Boolean {
        val normAddr = address.filter { it.isDigit() || it == '+' }
        if (normAddr.none { it.isDigit() }) return false
        val normRule = ruleValue.filter { it.isDigit() || it == '+' }
        if (normRule.none { it.isDigit() }) return false
        return normAddr.endsWith(normRule) || normRule.endsWith(normAddr)
    }
}
