package tech.dmampro.tinda.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lot 1 : résolution Material You. Sans préférence enregistrée, la
 * palette fixe choisie doit s'appliquer (défaut false) ; une valeur
 * explicitement enregistrée est toujours respectée.
 */
class ResolveDynamicColorTest {

    @Test
    fun storedNull_defaultsToFalse() {
        assertFalse(resolveDynamicColor(null))
    }

    @Test
    fun storedTrue_staysTrue() {
        assertTrue(resolveDynamicColor(true))
    }

    @Test
    fun storedFalse_staysFalse() {
        assertFalse(resolveDynamicColor(false))
    }
}
