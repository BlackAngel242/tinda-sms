package tech.dmampro.tinda.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chaîne de migrations de la base principale (P0-5) : le retrait du
 * repli destructif n'est sûr que si chaque version supportée mène à la
 * suivante sans trou, jusqu'à la version déclarée par @Database.
 * Les migrations elles-mêmes (SQL) seront couvertes par des tests
 * instrumentés MigrationTestHelper quand l'artefact room-testing sera
 * disponible dans l'environnement ; ils ne sont pas exécutables ici.
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
