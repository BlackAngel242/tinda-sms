package tech.dmampro.tinda.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chaîne de migrations de la base principale (P0-5) : le retrait du
 * repli destructif n'est sûr que si chaque version supportée mène à la
 * suivante sans trou, jusqu'à la version déclarée par @Database.
 * Le SQL émis par chaque migration est exécuté et contrôlé en JVM par
 * MigrationExecutionTest. En revanche, aucune migration n'a été
 * exécutée contre une vraie base SQLite ici : MigrationTestHelper
 * demande androidx.room:room-testing et androidx.test, absents du
 * cache Gradle hors ligne (recherche faite le 23 octobre 2026), les
 * schémas exportés ne couvrent que les versions 10 et 11, et aucun
 * appareil ni émulateur n'est disponible. La conservation réelle des
 * données par les migrations reste donc non exécutée dans cet
 * environnement, voir docs/validation/revue-codex-pr2-correctifs.md.
 */
class MigrationChainTest {

    @Test
    fun `la chaine est continue de la version 3 a la version courante`() {
        val steps = ALL_MIGRATIONS
            .map { it.startVersion to it.endVersion }
            .sortedBy { it.first }
        assertEquals(3, steps.first().first)
        steps.forEach { (start, end) -> assertEquals(start + 1, end) }
        for (i in 1 until steps.size) {
            assertEquals(steps[i - 1].second, steps[i].first)
        }
        assertEquals(DATABASE_VERSION, steps.last().second)
    }

    @Test
    fun `aucune etape de migration en double`() {
        val keys = ALL_MIGRATIONS.map { it.startVersion to it.endVersion }
        assertEquals(keys.size, keys.distinct().size)
        assertTrue(ALL_MIGRATIONS.isNotEmpty())
    }
}
