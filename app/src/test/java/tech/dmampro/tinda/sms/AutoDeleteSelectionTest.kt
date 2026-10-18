package tech.dmampro.tinda.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.dmampro.tinda.data.local.Message

/**
 * Logique pure de la suppression automatique (lot 4) : calcul de l'échéance
 * et sélection des messages à supprimer. Aucune dépendance Android.
 */
class AutoDeleteSelectionTest {

    private fun message(id: Long, date: Long) = Message(
        id = id,
        threadId = 1L,
        body = "test",
        date = date,
        read = true,
        incoming = true,
    )

    @Test
    fun cutoff_isDaysInMillis() {
        val now = 1_000_000_000_000L
        assertEquals(now - 90L * 24 * 3_600_000, deletionCutoff(90, now))
        assertEquals(now - 1L * 24 * 3_600_000, deletionCutoff(1, now))
    }

    @Test
    fun cutoff_clampsToAtLeastOneDay() {
        val now = 1_000_000_000_000L
        assertEquals(now - 1L * 24 * 3_600_000, deletionCutoff(0, now))
        assertEquals(now - 1L * 24 * 3_600_000, deletionCutoff(-5, now))
    }

    @Test
    fun selection_keepsMostRecent() {
        // Triés du plus récent au plus ancien, comme la requête DAO.
        val messages = listOf(
            message(3, 3000),
            message(2, 2000),
            message(1, 1000),
        )
        val toDelete = selectForDeletion(messages, keep = 1)
        assertEquals(listOf(2L, 1L), toDelete.map { it.id })
    }

    @Test
    fun selection_keepZero_deletesAll() {
        val messages = listOf(message(2, 2000), message(1, 1000))
        assertEquals(2, selectForDeletion(messages, keep = 0).size)
    }

    @Test
    fun selection_keepMoreThanSize_deletesNothing() {
        val messages = listOf(message(1, 1000))
        assertTrue(selectForDeletion(messages, keep = 20).isEmpty())
    }
}
