package tech.dmampro.tinda.data.local

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrats SQL des DAO impliqués dans les accusés (revue PR nº 2).
 *
 * Les annotations Room ne sont pas conservées à l'exécution, ces tests
 * lisent donc le code source des DAO et vérifient les conditions
 * réellement déclarées autour de chaque méthode. Ils prouvent le
 * contrat écrit, pas son exécution par SQLite : l'exécution contre
 * une vraie base Room demande un test instrumenté (room-testing et
 * androidx.test absents du cache hors ligne, aucun appareil ni
 * émulateur ici), voir docs/validation/revue-codex-pr2-correctifs.md.
 */
class DaoQueryContractTest {

    /** Fenêtre de source entourant la déclaration d'une méthode DAO. */
    private fun sourceAround(method: String): String {
        val source = loadDaoSource()
        val index = source.indexOf("suspend fun $method")
        if (index < 0) throw AssertionError("Méthode DAO introuvable dans la source : $method")
        val start = maxOf(0, index - 900)
        val end = minOf(source.length, index + 200)
        return source.substring(start, end)
    }

    private fun loadDaoSource(): String {
        val relative = "java/tech/dmampro/tinda/data/local/TindaDatabase.kt"
        val candidates = listOf(
            File("src/main/$relative"),
            File("app/src/main/$relative"),
            File("../app/src/main/$relative"),
            File("../../app/src/main/$relative"),
        )
        var dir = File(System.getProperty("user.dir")).absoluteFile
        val walked = generateSequence(dir) { it.parentFile }
            .take(5)
            .flatMap { base ->
                listOf(
                    File(base, "src/main/$relative"),
                    File(base, "app/src/main/$relative"),
                )
            }
            .toList()
        return (candidates + walked).firstOrNull { it.isFile }
            ?.readText()
            ?: throw AssertionError(
                "Code source des DAO introuvable depuis ${System.getProperty("user.dir")}",
            )
    }

    @Test
    fun `resolveByToken resout aussi une tentative AMBIGUOUS du meme jeton`() {
        val query = sourceAround("resolveByToken")
        assertTrue(query.contains("attemptToken = :token"))
        assertTrue(query.contains("status IN ('SENDING', 'AMBIGUOUS')"))
    }

    @Test
    fun `un jeton remplace ne peut plus resoudre la ligne reenfilee`() {
        // Après réenfilage, le jeton repasse à NULL avant l'attribution
        // d'un nouveau jeton : la condition attemptToken = :token de
        // resolveByToken ne peut donc plus matcher l'ancienne tentative.
        val requeue = sourceAround("requeueAmbiguous")
        assertTrue(requeue.contains("attemptToken = NULL"))
        assertTrue(requeue.contains("status = 'AMBIGUOUS'"))
        val claim = sourceAround("claim")
        assertTrue(claim.contains("attemptToken = NULL"))
    }

    @Test
    fun `la transition AMBIGUOUS est conditionnee par la tentative relue`() {
        val query = sourceAround("markAmbiguousIfStillSending")
        assertTrue(query.contains("status = 'AMBIGUOUS'"))
        assertTrue(query.contains("id = :id"))
        assertTrue(query.contains("status = 'SENDING'"))
        assertTrue(query.contains("attemptStartedAt = :startedAt"))
        assertTrue(query.contains("attemptToken = :token"))
        // Valeurs NULL acceptées des deux côtés (jeton pas encore attaché).
        assertTrue(query.contains("attemptToken IS NULL"))
        assertTrue(query.contains("attemptStartedAt IS NULL"))
    }

    @Test
    fun `les ecritures de statut accusees ne peuvent pas ecraser un terminal`() {
        val plain = sourceAround("updateStatusByTokenIfNotTerminal")
        assertTrue(plain.contains("status NOT IN ('DELIVERED', 'FAILED')"))
        assertTrue(plain.contains("OR status = :status"))
        val withFailure = sourceAround("updateStatusAndFailureByTokenIfNotTerminal")
        assertTrue(withFailure.contains("status NOT IN ('DELIVERED', 'FAILED')"))
        assertTrue(withFailure.contains("failureCode = :failureCode"))
    }
}
