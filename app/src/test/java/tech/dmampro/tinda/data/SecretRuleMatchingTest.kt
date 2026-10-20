package tech.dmampro.tinda.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.dmampro.tinda.data.local.SecretRule

/**
 * Routage vers le coffre (liste jaune) : correspondance adresse/contenu.
 * Cas critique P0-2 : un expéditeur alphanumérique ne doit jamais
 * correspondre à une règle NUMBER, mais peut correspondre à une règle KEYWORD.
 */
class SecretRuleMatchingTest {

    private fun numberRule(value: String) =
        SecretRule(type = VaultRepository.RULE_NUMBER, value = value, addedAt = 0L)

    private fun keywordRule(value: String) =
        SecretRule(type = VaultRepository.RULE_KEYWORD, value = value, addedAt = 0L)

    @Test
    fun `expediteur alphabetique ne correspond jamais a une regle numero`() {
        val rules = listOf(numberRule("0612345678"))
        assertFalse(SecretRuleMatching.matches("MOMO", "Votre solde est insuffisant", rules))
        assertFalse(SecretRuleMatching.matches("Airtel", "Promotion du jour", rules))
    }

    @Test
    fun `adresse vide ne correspond a aucune regle numero`() {
        val rules = listOf(numberRule("0612345678"))
        assertFalse(SecretRuleMatching.matches("", "Bonjour", rules))
    }

    @Test
    fun `expediteur alphabetique peut correspondre a une regle mot-cle`() {
        val rules = listOf(numberRule("0612345678"), keywordRule("solde"))
        assertTrue(SecretRuleMatching.matches("MOMO", "Votre SOLDE restant est faible", rules))
    }

    @Test
    fun `mot-cle absent du contenu ne correspond pas`() {
        val rules = listOf(keywordRule("urgent"))
        assertFalse(SecretRuleMatching.matches("MOMO", "Bonjour, bonne journee", rules))
    }

    @Test
    fun `numero correspondant par suffixe correspond a la regle numero`() {
        val rules = listOf(numberRule("612345678"))
        assertTrue(SecretRuleMatching.matches("+242612345678", "Bonjour", rules))
        assertTrue(SecretRuleMatching.matches("612345678", "Bonjour", rules))
    }

    @Test
    fun `numero different ne correspond pas a la regle numero`() {
        val rules = listOf(numberRule("612345678"))
        assertFalse(SecretRuleMatching.matches("+242699887766", "Bonjour", rules))
    }

    @Test
    fun `aucune regle ne correspond jamais`() {
        assertFalse(SecretRuleMatching.matches("MOMO", "solde urgent", emptyList()))
        assertFalse(SecretRuleMatching.matches("+242612345678", "solde", emptyList()))
    }

    @Test
    fun `regle numero vide ne correspond a rien`() {
        val rules = listOf(numberRule(""))
        assertFalse(SecretRuleMatching.matches("MOMO", "Bonjour", rules))
        assertFalse(SecretRuleMatching.matches("+242612345678", "Bonjour", rules))
    }
}
